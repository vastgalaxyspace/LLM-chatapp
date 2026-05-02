package com.example.chatapp.ui.screens.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.local.ChatLocalStore
import com.example.chatapp.data.local.LocalMediaStore
import androidx.lifecycle.ViewModel
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatLocalStore: ChatLocalStore,
    private val localMediaStore: LocalMediaStore,
    private val appPreferences: AppPreferences,
    private val chatRepository: ChatRepository
) : ViewModel() {
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
        initialValue = 512
    )
    val huggingFaceToken: StateFlow<String> = appPreferences.huggingFaceToken.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    fun updateBackend(backend: String) {
        viewModelScope.launch {
            appPreferences.updateSelectedBackend(backend)
            chatRepository.closeEngine()
            chatRepository.initializeEngine(backend)
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
            val currentBackend = selectedBackend.first()
            chatRepository.closeEngine()
            chatRepository.initializeEngine(currentBackend)
        }
    }

    fun updateMaxTokens(value: Int) {
        viewModelScope.launch {
            appPreferences.updateMaxTokens(value)
            val currentBackend = selectedBackend.first()
            chatRepository.closeEngine()
            chatRepository.initializeEngine(currentBackend)
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
        }
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
            val modelsDir = File(context.filesDir, "models")
            val fallbackModelId = ModelCatalog.all.firstOrNull {
                val file = File(modelsDir, it.fileName)
                file.exists() && file.length() > 0L
            }?.id ?: ModelCatalog.QWEN_SMALL
            appPreferences.updateSelectedModel(fallbackModelId)
        }
    }
}
