package com.example.chatapp.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.local.ChatLocalStore
import com.example.chatapp.data.local.ConversationSearchResult
import com.example.chatapp.data.local.LocalMediaStore
import androidx.lifecycle.ViewModel
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.data.repository.ModelFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatLocalStore: ChatLocalStore,
    private val localMediaStore: LocalMediaStore,
    private val appPreferences: AppPreferences,
    private val chatRepository: ChatRepository,
    private val modelFileRepository: ModelFileRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            appPreferences.migratePlaintextHuggingFaceTokenIfNeeded()
        }
    }

    val selectedBackend: StateFlow<String> = appPreferences.selectedBackend.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "GPU"
    )
    val selectedModel: StateFlow<String> = appPreferences.selectedModel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModelCatalog.QWEN_SMALL
    )
    val temperature: StateFlow<Float> = appPreferences.temperature.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0.8f
    )
    val maxTokens: StateFlow<Int> = appPreferences.maxTokens.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 1536
    )
    val huggingFaceToken: StateFlow<String> = appPreferences.huggingFaceToken.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )
    private val _searchResults = MutableStateFlow<List<ConversationSearchResult>>(emptyList())
    val searchResults: StateFlow<List<ConversationSearchResult>> = _searchResults.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private val _backendLoading = MutableStateFlow(false)
    val backendLoading: StateFlow<Boolean> = _backendLoading.asStateFlow()

    private val _backendError = MutableStateFlow<String?>(null)
    val backendError: StateFlow<String?> = _backendError.asStateFlow()

    fun updateBackend(backend: String) {
        viewModelScope.launch {
            _backendLoading.value = true
            _backendError.value = null
            try {
                val result = runCatching {
                    appPreferences.updateSelectedBackend(backend)
                    chatRepository.closeEngine()
                    chatRepository.initializeEngine(backend).getOrThrow()
                }
                if (result.isFailure) {
                    _backendError.value = "Couldn't switch backend. Please try another option."
                }
            } finally {
                _backendLoading.value = false
            }
        }
    }

    fun updateModel(modelId: String) {
        viewModelScope.launch {
            appPreferences.updateSelectedModel(modelId)
            chatRepository.closeEngine()
        }
    }

    fun updateTemperature(value: Float) {
        viewModelScope.launch {
            appPreferences.updateTemperature(value)
            chatRepository.closeEngine()
        }
    }

    fun updateMaxTokens(value: Int) {
        viewModelScope.launch {
            appPreferences.updateMaxTokens(value)
            chatRepository.closeEngine()
        }
    }

    fun updateHuggingFaceToken(value: String) {
        viewModelScope.launch {
            appPreferences.updateHuggingFaceToken(value)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearConversation()
            chatLocalStore.deleteAllHistory()
            localMediaStore.deleteAllChatMedia()
            _searchResults.value = emptyList()
        }
    }

    fun searchHistory(query: String) {
        viewModelScope.launch {
            _searchResults.value = chatLocalStore.searchMessages(query)
        }
    }

    fun shareHistory(format: ExportFormat) {
        viewModelScope.launch {
            runCatching {
                val (mimeType, title, body) = when (format) {
                    ExportFormat.TEXT -> Triple(
                        "text/plain",
                        "InnoAI conversation history.txt",
                        chatLocalStore.exportAllConversationsAsText()
                    )
                    ExportFormat.JSON -> Triple(
                        "application/json",
                        "InnoAI conversation history.json",
                        chatLocalStore.exportAllConversationsAsJson()
                    )
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export conversation history").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure {
                _exportError.value = "Couldn't export conversation history. Please try again."
            }
        }
    }

    fun clearExportError() {
        _exportError.value = null
    }

    fun clearBackendError() {
        _backendError.value = null
    }

    fun currentModel(): ModelOption = ModelCatalog.fromId(selectedModel.value)

    fun deleteSelectedModel() {
        viewModelScope.launch {
            val model = ModelCatalog.fromId(appPreferences.selectedModel.first())
            val targetFile = File(context.filesDir, "models/${model.fileName}")
            val partialFile = File(context.filesDir, "models/${model.fileName}.part")
            chatRepository.closeEngine()
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (partialFile.exists()) {
                partialFile.delete()
            }
            modelFileRepository.notifyChange()
            val modelsDir = File(context.filesDir, "models")
            val fallbackModelId = ModelCatalog.available.firstOrNull {
                val file = File(modelsDir, it.fileName)
                file.exists() && file.length() > 0L
            }?.id ?: ModelCatalog.QWEN_SMALL
            appPreferences.updateSelectedModel(fallbackModelId)
        }
    }
}

enum class ExportFormat {
    TEXT,
    JSON
}
