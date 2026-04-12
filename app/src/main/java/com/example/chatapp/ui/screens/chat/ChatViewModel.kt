package com.example.chatapp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.local.ChatLocalStore
import com.example.chatapp.data.local.ConversationSummary
import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val chatLocalStore: ChatLocalStore,
    appPreferences: AppPreferences
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()
    val conversationHistory: StateFlow<List<ConversationSummary>> = chatLocalStore.observeConversationSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val backend = appPreferences.selectedBackend.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "GPU"
    )
    private val effectiveBackend = appPreferences.selectedBackend.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "GPU"
    )
    private val temperature = appPreferences.temperature.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0.8f
    )
    private val maxTokens = appPreferences.maxTokens.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 512
    )

    private var generationJob: Job? = null
    private var conversationObserverJob: Job? = null
    private var lastStreamingMessageId: String? = null

    init {
        viewModelScope.launch {
            val conversationId = chatLocalStore.latestConversationIdOrCreate()
            observeConversation(conversationId)
        }
    }

    fun openConversation(conversationId: Long?) {
        viewModelScope.launch {
            val resolvedId = chatLocalStore.ensureConversation(conversationId)
            if (_activeConversationId.value == resolvedId) return@launch
            chatRepository.clearConversation()
            observeConversation(resolvedId)
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val newConversationId = chatLocalStore.createNewConversation()
            chatRepository.clearConversation()
            observeConversation(newConversationId)
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            val deletingActiveConversation = _activeConversationId.value == conversationId
            val nextConversationId = chatLocalStore.deleteConversation(conversationId)
            if (deletingActiveConversation) {
                generationJob?.cancel()
                _isGenerating.value = false
                chatRepository.clearConversation()
                observeConversation(nextConversationId)
            }
        }
    }

    private fun observeConversation(conversationId: Long) {
        _activeConversationId.value = conversationId
        conversationObserverJob?.cancel()
        conversationObserverJob = viewModelScope.launch {
            chatLocalStore.observeConversationMessages(conversationId).collect { items ->
                _messages.value = items
            }
        }
    }

    fun initEngine() {
        if (chatRepository.isEngineReady()) {
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val requestedBackend = backend.first()
            val result = chatRepository.initializeEngine(requestedBackend)
            _isLoading.value = false
            if (
                result.isSuccess &&
                requestedBackend.equals("GPU", ignoreCase = true) &&
                effectiveBackend.value.equals("CPU", ignoreCase = true)
            ) {
                _errorMessage.value = "GPU not available on this device. Switched to CPU."
            } else {
                result.exceptionOrNull()?.let {
                    _errorMessage.value = toUserError(it.message)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value || _isLoading.value) return

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val conversationId = chatLocalStore.ensureConversation(_activeConversationId.value)
            if (_activeConversationId.value != conversationId) {
                observeConversation(conversationId)
            }

            val userMessage = ChatMessage(content = trimmed, role = MessageRole.USER)
            val aiMessage = ChatMessage(content = "", role = MessageRole.AI, isStreaming = true)
            lastStreamingMessageId = aiMessage.id

            chatLocalStore.insertMessage(conversationId, userMessage)
            chatLocalStore.insertMessage(conversationId, aiMessage)

            _isGenerating.value = true
            _errorMessage.value = null

            var aiContent = ""
            try {
                sendMessageUseCase(trimmed, temperature.value, maxTokens.value).collect { token ->
                    aiContent += token
                    chatLocalStore.updateMessageContent(
                        messageId = aiMessage.id,
                        content = aiContent,
                        isStreaming = true
                    )
                }
            } catch (throwable: Throwable) {
                _errorMessage.value = toUserError(throwable.message)
            } finally {
                _isGenerating.value = false
                chatLocalStore.updateMessageContent(
                    messageId = aiMessage.id,
                    content = aiContent,
                    isStreaming = false
                )
                lastStreamingMessageId = null
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        viewModelScope.launch {
            val messageId = lastStreamingMessageId ?: return@launch
            val activeAiMessage = _messages.value.lastOrNull { it.id == messageId } ?: return@launch
            chatLocalStore.updateMessageContent(
                messageId = messageId,
                content = activeAiMessage.content,
                isStreaming = false
            )
            lastStreamingMessageId = null
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            generationJob?.cancel()
            _isGenerating.value = false
            _errorMessage.value = null
            chatRepository.clearConversation()
            chatLocalStore.deleteAllHistory()
            val newConversation = chatLocalStore.latestConversationIdOrCreate()
            observeConversation(newConversation)
        }
    }

    fun isEngineReady(): Boolean = chatRepository.isEngineReady()

    private fun toUserError(raw: String?): String {
        val message = raw.orEmpty().trim()
        val normalized = message.lowercase()
        return when {
            normalized.isBlank() ->
                "Something went wrong. Please try again."
            normalized.contains("model file not found") ||
                normalized.contains("no such file") ->
                "The selected model is missing on this device. Open Models and download it again."
            normalized.contains("not initialized") ||
                normalized.contains("engine is not initialized") ->
                "The AI model is still loading. Please wait a moment and try again."
            normalized.contains("outofmemory") ||
                normalized.contains("out of memory") ||
                normalized.contains("cannot allocate memory") ->
                "Your device ran out of memory. Close a few apps or use a smaller model."
            normalized.contains("timeout") ||
                normalized.contains("timed out") ->
                "The request took too long. Please try again."
            normalized.contains("no space left") ||
                normalized.contains("enospc") ->
                "Your device storage is full. Free some space and try again."
            normalized.contains("permission denied") ->
                "The app cannot access required files right now. Restart the app and try again."
            normalized.contains("gpu") && normalized.contains("backend") ->
                "This device cannot run the selected model on GPU. Switch to CPU in Settings and retry."
            else ->
                "Couldn't complete your request. Please try again."
        }
    }
}
