package com.example.chatapp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.chatapp.data.local.ChatLocalStore
import com.example.chatapp.data.local.ConversationSummary
import com.example.chatapp.data.local.LocalMediaStore
import com.example.chatapp.data.local.StoredMedia
import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageAttachment
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.model.MessageType
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.data.repository.ContextWindowManager
import com.example.chatapp.domain.AssistantResponseCleaner
import com.example.chatapp.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val chatLocalStore: ChatLocalStore,
    private val localMediaStore: LocalMediaStore,
    private val contextWindowManager: ContextWindowManager,
    private val appPreferences: AppPreferences
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationTokensPerSecond = MutableStateFlow<Float?>(null)
    val generationTokensPerSecond: StateFlow<Float?> = _generationTokensPerSecond.asStateFlow()

    private val _timeToFirstTokenMillis = MutableStateFlow<Long?>(null)
    val timeToFirstTokenMillis: StateFlow<Long?> = _timeToFirstTokenMillis.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val estimatedTokensUsed: StateFlow<Int> = contextWindowManager.estimatedTokensUsed
    val contextWindowPercent: StateFlow<Float> = contextWindowManager.contextWindowPercent

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

    private val selectedModelId = appPreferences.selectedModel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModelCatalog.QWEN_SMALL
    )

    val selectedModelName: StateFlow<String> = selectedModelId
        .map { modelId -> ModelCatalog.fromId(modelId).displayName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "Loading..."
        )

    private var generationJob: Job? = null
    private var conversationObserverJob: Job? = null
    private var lastStreamingMessageId: String? = null
    private var initJob: Job? = null

    init {
        viewModelScope.launch {
            chatLocalStore.finishAbandonedStreamingMessages()
            val conversationId = chatLocalStore.latestConversationIdOrCreate()
            observeConversation(conversationId)
        }
    }

    /** FIX: bridge for UI audio recording */
    fun createAudioTarget(): java.io.File = localMediaStore.createAudioTarget()

    /** FIX: bridge for UI conversation renaming */
    fun renameConversation(id: Long, title: String) {
        viewModelScope.launch {
            val trimmed = title.trim()
            if (trimmed.isNotEmpty()) {
                chatLocalStore.renameConversation(id, trimmed)
            }
        }
    }

    fun initEngine() {
        if (chatRepository.isEngineReady()) {
            _isLoading.value = false
            return
        }

        initJob?.cancel()
        initJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = withTimeoutOrNull(60_000L) {
                val requestedBackend = backend.first()
                chatRepository.initializeEngine(requestedBackend)
            }

            _isLoading.value = false
            if (result == null) {
                _errorMessage.value = "Model initialization timed out. Please try again."
            } else {
                result.onFailure { _errorMessage.value = toUserError(it.message) }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value || _isLoading.value) return

        val selectedModel = ModelCatalog.fromId(selectedModelId.value)
        val attachments = pendingAttachmentsForPrompt(selectedModel).getOrElse {
            _errorMessage.value = it.message
            return
        }

        if ("Text" !in selectedModel.useCases && attachments.isEmpty()) {
            _errorMessage.value = "Multimodal model selected. Please attach media or switch models."
            return
        }

        sendPreparedMessage(trimmed, attachments)
    }

    /** FIX: bridge for regenerations */
    fun retryMessage(messageId: String) {
        if (_isGenerating.value || _isLoading.value) return

        val currentMsgs = _messages.value
        val assistantIdx = currentMsgs.indexOfFirst { it.id == messageId && it.role == MessageRole.AI }
        if (assistantIdx <= 0) return

        val turnBoundaryIdx = currentMsgs.subList(0, assistantIdx).indexOfLast { it.role == MessageRole.AI }
        val targetTurn = currentMsgs.subList(turnBoundaryIdx + 1, assistantIdx)

        val prompt = targetTurn.lastOrNull { it.role == MessageRole.USER && it.type == MessageType.TEXT }
            ?.content?.trim().orEmpty()

        if (prompt.isEmpty()) return

        val model = ModelCatalog.fromId(selectedModelId.value)
        val attachments = attachmentsFromMessages(targetTurn, model).getOrElse {
            _errorMessage.value = it.message
            return
        }

        sendPreparedMessage(prompt, attachments)
    }

    private fun sendPreparedMessage(trimmed: String, attachments: List<MessageAttachment>) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val conversationId = chatLocalStore.ensureConversation(_activeConversationId.value)
            if (_activeConversationId.value != conversationId) observeConversation(conversationId)

            val aiMessageId = ChatMessage(content = "", role = MessageRole.AI).id
            lastStreamingMessageId = aiMessageId

            chatLocalStore.insertMessage(conversationId, ChatMessage(content = trimmed, role = MessageRole.USER))
            chatLocalStore.insertMessage(conversationId, ChatMessage(id = aiMessageId, content = "", role = MessageRole.AI, isStreaming = true))

            _isGenerating.value = true
            _errorMessage.value = null

            var rawContent = ""
            val startedAt = System.currentTimeMillis()
            var chunks = 0

            try {
                sendMessageUseCase(trimmed, attachments)
                    .flowOn(Dispatchers.Default)
                    .collect { token ->
                        if (chunks == 0) _timeToFirstTokenMillis.value = System.currentTimeMillis() - startedAt
                        chunks++
                        rawContent += token
                        _generationTokensPerSecond.value = chunks / ((System.currentTimeMillis() - startedAt) / 1000f).coerceAtLeast(0.1f)
                        val cleaned = AssistantResponseCleaner.clean(rawContent)
                        chatLocalStore.updateMessageContent(aiMessageId, cleaned, true)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                _errorMessage.value = toUserError(e.message)
            } finally {
                _isGenerating.value = false
                _generationTokensPerSecond.value = null
                val finalCleaned = AssistantResponseCleaner.clean(rawContent)
                chatLocalStore.updateMessageContent(aiMessageId, finalCleaned.ifBlank { "Error generating response." }, false)
                lastStreamingMessageId = null
            }
        }
    }

    private fun observeConversation(conversationId: Long) {
        _activeConversationId.value = conversationId
        conversationObserverJob?.cancel()
        conversationObserverJob = viewModelScope.launch {
            chatLocalStore.observeConversationMessages(conversationId).collect { _messages.value = it }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        lastStreamingMessageId = null
    }

    fun openConversation(id: Long?) = viewModelScope.launch {
        val resolved = chatLocalStore.ensureConversation(id)
        if (_activeConversationId.value == resolved) return@launch
        chatRepository.clearConversation()
        observeConversation(resolved)
    }

    fun startNewConversation() = viewModelScope.launch {
        chatRepository.clearConversation()
        observeConversation(chatLocalStore.createNewConversation())
    }

    fun deleteConversation(id: Long) = viewModelScope.launch {
        if (_activeConversationId.value == id) {
            stopGeneration()
            chatRepository.clearConversation()
        }
        observeConversation(chatLocalStore.deleteConversation(id))
    }

    private fun pendingAttachmentsForPrompt(model: ModelOption): Result<List<MessageAttachment>> = runCatching {
        attachmentsFromMessages(_messages.value.afterLastAssistantMessage(), model).getOrThrow()
    }

    private fun attachmentsFromMessages(msgs: List<ChatMessage>, model: ModelOption): Result<List<MessageAttachment>> = runCatching {
        msgs.filter { it.role == MessageRole.USER && it.type != MessageType.TEXT }
            .map {
                val typeTag = if (it.type == MessageType.IMAGE) "Image" else "Audio"
                if (typeTag !in model.useCases) throw IllegalStateException("${model.displayName} doesn't support $typeTag.")
                MessageAttachment(it.mediaUri!!, it.type, it.mimeType)
            }
    }

    private fun List<ChatMessage>.afterLastAssistantMessage(): List<ChatMessage> {
        val lastIdx = indexOfLast { it.role == MessageRole.AI }
        return drop(lastIdx + 1)
    }

    fun attachImage(uri: android.net.Uri) = viewModelScope.launch {
        runCatching { localMediaStore.copyImage(uri) }
            .onSuccess { insertLocalMediaMessage(MessageType.IMAGE, it, "Image attached") }
            .onFailure { _errorMessage.value = "Failed to attach image." }
    }

    fun attachRecordedAudio(file: java.io.File, duration: Long) = viewModelScope.launch {
        if (file.exists() && file.length() > 0) {
            insertLocalMediaMessage(MessageType.AUDIO, localMediaStore.storedAudio(file), "Audio note", duration)
        }
    }

    private suspend fun insertLocalMediaMessage(type: MessageType, media: StoredMedia, content: String, duration: Long? = null) {
        val cid = chatLocalStore.ensureConversation(_activeConversationId.value)
        chatLocalStore.insertMessage(cid, ChatMessage(content = content, role = MessageRole.USER, type = type, mediaUri = media.path, mimeType = media.mimeType, durationMillis = duration))
    }

    private fun toUserError(raw: String?): String {
        val msg = raw.orEmpty().lowercase()
        return when {
            msg.contains("memory") -> "Device out of memory. Try a smaller model."
            msg.contains("file not found") -> "Model file missing. Please re-download."
            msg.contains("gpu") -> "GPU not supported. Switching to CPU."
            else -> "Generation failed. Please try again."
        }
    }

    fun isEngineReady(): Boolean = chatRepository.isEngineReady()

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
        initJob?.cancel()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
