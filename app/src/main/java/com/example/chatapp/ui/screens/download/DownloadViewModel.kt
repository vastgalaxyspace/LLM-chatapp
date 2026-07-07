package com.example.chatapp.ui.screens.download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.data.repository.ModelFileRepository
import com.example.chatapp.domain.usecase.DownloadState
import com.example.chatapp.worker.ModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val chatRepository: ChatRepository,
    private val modelFileRepository: ModelFileRepository,
    private val workManager: WorkManager
) : ViewModel() {
    private val _downloadState =
        MutableStateFlow<DownloadState>(DownloadState.Downloading(0f, 0f, 0f))
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId.asStateFlow()

    private val _activeModelIds = MutableStateFlow<Set<String>>(emptySet())
    val activeModelIds: StateFlow<Set<String>> = _activeModelIds.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _lastTouchedModelId = MutableStateFlow<String?>(null)
    val lastTouchedModelId: StateFlow<String?> = _lastTouchedModelId.asStateFlow()

    private val _downloadedModelIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModelIds: StateFlow<Set<String>> = _downloadedModelIds.asStateFlow()

    val selectedModelId: StateFlow<String> = appPreferences.selectedModel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModelCatalog.QWEN_SMALL
    )
    val huggingFaceToken: StateFlow<String> = appPreferences.huggingFaceToken.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    private val downloadJobs = mutableMapOf<String, Job>()
    private val _openSelectedModel = MutableStateFlow(false)
    val openSelectedModel: StateFlow<Boolean> = _openSelectedModel.asStateFlow()

    init {
        viewModelScope.launch {
            modelFileRepository.observeDownloadedModelIds().collect {
                _downloadedModelIds.value = it
            }
        }
    }

    fun models(): List<ModelOption> = ModelCatalog.all

    fun isDownloaded(modelId: String): Boolean = downloadedModelIds.value.contains(modelId)

    fun useModel(modelId: String) {
        viewModelScope.launch {
            _lastTouchedModelId.value = modelId
            if (isDownloaded(modelId)) {
                appPreferences.updateSelectedModel(modelId)
                chatRepository.closeEngine()
                _openSelectedModel.value = true
            } else {
                startDownload(modelId)
            }
        }
    }

    private fun startDownload(modelId: String) {
        if (downloadJobs[modelId]?.isActive == true) return
        val model = ModelCatalog.fromId(modelId)
        _activeModelId.value = modelId
        _lastTouchedModelId.value = modelId
        _downloadState.value = DownloadState.Downloading(0f, 0f, model.sizeMb)
        _activeModelIds.value = _activeModelIds.value + modelId
        _downloadStates.value = _downloadStates.value + (modelId to DownloadState.Downloading(0f, 0f, model.sizeMb))

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(ModelDownloadWorker.KEY_MODEL_ID to modelId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("download_$modelId")
            .build()

        workManager.enqueueUniqueWork("download_$modelId", ExistingWorkPolicy.KEEP, request)

        downloadJobs[modelId] = viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("download_$modelId").collect { infos ->
                val info = infos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f)
                        val dlMb = info.progress.getFloat(ModelDownloadWorker.KEY_DOWNLOADED_MB, 0f)
                        val totalMb = info.progress.getFloat(ModelDownloadWorker.KEY_TOTAL_MB, model.sizeMb)
                        val speed = info.progress.getFloat(ModelDownloadWorker.KEY_SPEED_MBPS, 0f)
                        val eta = info.progress.getLong(ModelDownloadWorker.KEY_ETA_SECONDS, -1L).takeIf { it >= 0 }
                        val state = DownloadState.Downloading(progress, dlMb, totalMb, speed, eta)
                        _downloadState.value = state
                        _downloadStates.value = _downloadStates.value + (modelId to state)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        modelFileRepository.notifyChange()
                        _downloadedModelIds.value = modelFileRepository.downloadedModelIds()
                        appPreferences.updateSelectedModel(modelId)
                        chatRepository.closeEngine()
                        _activeModelIds.value = _activeModelIds.value - modelId
                        _downloadStates.value = _downloadStates.value - modelId
                        if (_activeModelId.value == modelId) _activeModelId.value = null
                        _openSelectedModel.value = true
                        downloadJobs.remove(modelId)
                    }
                    WorkInfo.State.FAILED -> {
                        val error = info.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                            ?: "Download failed. Please retry."
                        _downloadStates.value = _downloadStates.value + (modelId to DownloadState.Error(error))
                        _downloadState.value = DownloadState.Error(error)
                        _activeModelIds.value = _activeModelIds.value - modelId
                        if (_activeModelId.value == modelId) _activeModelId.value = null
                        downloadJobs.remove(modelId)
                    }
                    WorkInfo.State.CANCELLED -> {
                        _activeModelIds.value = _activeModelIds.value - modelId
                        _downloadStates.value = _downloadStates.value - modelId
                        if (_activeModelId.value == modelId) _activeModelId.value = null
                        downloadJobs.remove(modelId)
                    }
                    else -> Unit
                }
                if (info.state.isFinished) {
                    return@collect
                }
            }
        }
    }

    fun onModelOpened() {
        _openSelectedModel.value = false
    }

    fun deleteModel(modelId: String) {
        downloadJobs.remove(modelId)?.cancel()
        workManager.cancelUniqueWork("download_$modelId")

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

            modelFileRepository.notifyChange()
            _downloadedModelIds.value = modelFileRepository.downloadedModelIds()
            if (activeModelId.value == modelId) {
                _activeModelId.value = null
            }
            _activeModelIds.value = _activeModelIds.value - modelId
            _downloadStates.value = _downloadStates.value - modelId
            _lastTouchedModelId.value = modelId
            _downloadState.value = DownloadState.Downloading(0f, 0f, model.sizeMb)

            if (selectedModelId.value == modelId) {
                val fallbackModelId = ModelCatalog.all.firstOrNull { it.id in _downloadedModelIds.value }?.id
                    ?: ModelCatalog.QWEN_SMALL
                appPreferences.updateSelectedModel(fallbackModelId)
            }
        }
    }

    fun retryActiveModel() {
        val modelId = activeModelId.value ?: lastTouchedModelId.value ?: selectedModelId.value
        startDownload(modelId)
    }

    fun retryModel(modelId: String) {
        startDownload(modelId)
    }

    fun updateHuggingFaceToken(value: String) {
        viewModelScope.launch {
            appPreferences.updateHuggingFaceToken(value)
        }
    }
}
