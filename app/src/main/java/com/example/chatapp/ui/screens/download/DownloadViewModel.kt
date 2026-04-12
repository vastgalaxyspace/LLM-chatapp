package com.example.chatapp.ui.screens.download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.domain.usecase.DownloadModelUseCase
import com.example.chatapp.domain.usecase.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadModelUseCase: DownloadModelUseCase,
    private val appPreferences: AppPreferences,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _downloadState =
        MutableStateFlow<DownloadState>(DownloadState.Downloading(0f, 0f, 0f))
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId.asStateFlow()

    private val _lastTouchedModelId = MutableStateFlow<String?>(null)
    val lastTouchedModelId: StateFlow<String?> = _lastTouchedModelId.asStateFlow()

    private val _downloadedModelIds = MutableStateFlow(scanDownloadedModels())
    val downloadedModelIds: StateFlow<Set<String>> = _downloadedModelIds.asStateFlow()

    val selectedModelId: StateFlow<String> = appPreferences.selectedModel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModelCatalog.GEMMA
    )

    private var downloadJob: Job? = null
    private val _openSelectedModel = MutableStateFlow(false)
    val openSelectedModel: StateFlow<Boolean> = _openSelectedModel.asStateFlow()

    fun models(): List<ModelOption> = ModelCatalog.all

    fun isDownloaded(modelId: String): Boolean = downloadedModelIds.value.contains(modelId)

    fun useModel(modelId: String) {
        if (downloadJob?.isActive == true) return
        viewModelScope.launch {
            appPreferences.updateSelectedModel(modelId)
            _lastTouchedModelId.value = modelId
            if (isDownloaded(modelId)) {
                _openSelectedModel.value = true
            } else {
                startDownload(modelId)
            }
        }
    }

    private suspend fun startDownload(modelId: String) {
        if (downloadJob?.isActive == true) return
        val model = ModelCatalog.fromId(modelId)
        _activeModelId.value = modelId
        _lastTouchedModelId.value = modelId
        _downloadState.value = DownloadState.Downloading(0f, 0f, model.sizeMb)

        downloadJob = viewModelScope.launch {
            downloadModelUseCase().collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Complete) {
                    _downloadedModelIds.value = scanDownloadedModels()
                    appPreferences.updateSelectedModel(modelId)
                    _activeModelId.value = null
                    _openSelectedModel.value = true
                }
                if (state is DownloadState.Error) {
                    _activeModelId.value = null
                }
            }
        }
    }

    fun onModelOpened() {
        _openSelectedModel.value = false
    }

    fun deleteModel(modelId: String) {
        if (downloadJob?.isActive == true && activeModelId.value == modelId) {
            downloadJob?.cancel()
        }

        viewModelScope.launch {
            val model = ModelCatalog.fromId(modelId)
            val targetFile = File(context.filesDir, "models/${model.fileName}")
            val partialFile = File(context.filesDir, "models/${model.fileName}.part")

            if (selectedModelId.value == modelId) {
                chatRepository.closeEngine()
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (partialFile.exists()) {
                partialFile.delete()
            }

            _downloadedModelIds.value = scanDownloadedModels()
            if (activeModelId.value == modelId) {
                _activeModelId.value = null
            }
            _lastTouchedModelId.value = modelId
            _downloadState.value = DownloadState.Downloading(0f, 0f, model.sizeMb)

            if (selectedModelId.value == modelId) {
                val fallbackModelId = ModelCatalog.all.firstOrNull { it.id in _downloadedModelIds.value }?.id
                    ?: ModelCatalog.GEMMA
                appPreferences.updateSelectedModel(fallbackModelId)
            }
        }
    }

    fun retryActiveModel() {
        val modelId = activeModelId.value ?: lastTouchedModelId.value ?: selectedModelId.value
        viewModelScope.launch {
            startDownload(modelId)
        }
    }

    private fun scanDownloadedModels(): Set<String> {
        val modelsDir = File(context.filesDir, "models")
        return ModelCatalog.all
            .filter { File(modelsDir, it.fileName).exists() && File(modelsDir, it.fileName).length() > 0L }
            .map { it.id }
            .toSet()
    }
}
