package com.example.chatapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ModelFileRepository
import com.example.chatapp.domain.usecase.DownloadModelUseCase
import com.example.chatapp.domain.usecase.DownloadState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadModelUseCase: DownloadModelUseCase,
    private val appPreferences: AppPreferences,
    private val modelFileRepository: ModelFileRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()

        setForeground(createForegroundInfo(modelId))

        var finalResult: Result = Result.failure()
        var lastProgressAt = 0L
        try {
            downloadModelUseCase(modelId).collect { state ->
                when (state) {
                is DownloadState.Downloading -> {
                    val now = System.currentTimeMillis()
                    if (now - lastProgressAt >= PROGRESS_INTERVAL_MS || state.progress >= 1f) {
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS to state.progress,
                                KEY_DOWNLOADED_MB to state.downloadedMB,
                                KEY_TOTAL_MB to state.totalMB,
                                KEY_SPEED_MBPS to state.speedMBps,
                                KEY_ETA_SECONDS to (state.etaSeconds ?: -1L)
                            )
                        )
                        lastProgressAt = now
                    }
                }
                is DownloadState.Complete -> {
                    appPreferences.updateSelectedModel(modelId)
                    modelFileRepository.notifyChange()
                    finalResult = Result.success(workDataOf(KEY_MODEL_ID to modelId))
                }
                is DownloadState.Error -> {
                    finalResult = Result.failure(workDataOf(KEY_ERROR to state.message))
                }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
        return finalResult
    }

    private fun createForegroundInfo(modelId: String): ForegroundInfo {
        val channelId = "model_download"
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "Model Downloads", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Downloading model")
            .setContentText(ModelCatalog.fromId(modelId).displayName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED_MB = "downloaded_mb"
        const val KEY_TOTAL_MB = "total_mb"
        const val KEY_SPEED_MBPS = "speed_mbps"
        const val KEY_ETA_SECONDS = "eta_seconds"
        const val KEY_ERROR = "error"
        private const val NOTIFICATION_ID = 1001
        private const val PROGRESS_INTERVAL_MS = 500L
    }
}
