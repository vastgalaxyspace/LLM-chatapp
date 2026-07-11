package com.example.chatapp.domain.usecase

import android.content.Context
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.model.ModelValidationResult
import com.example.chatapp.data.model.ModelValidator
import com.example.chatapp.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class DownloadState {
    data class Downloading(val progress: Float, val downloadedMB: Float, val totalMB: Float, val speedMBps: Float = 0f, val etaSeconds: Long? = null) : DownloadState()
    data object Complete : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class DownloadModelUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    operator fun invoke(modelId: String? = null): Flow<DownloadState> = flow {
        val model = ModelCatalog.requireById(modelId ?: appPreferences.selectedModel.first())
        if (!model.downloadable) {
            emit(DownloadState.Error(model.unavailableReason ?: "This model is unavailable."))
            return@flow
        }
        val directory = File(context.filesDir, "models")
        if (!directory.exists() && !directory.mkdirs()) {
            emit(DownloadState.Error("Unable to create models directory."))
            return@flow
        }
        val destination = File(directory, model.fileName)
        val partial = File(directory, "${model.fileName}.part")
        if (ModelValidator.validate(destination, model) is ModelValidationResult.Valid) {
            emit(DownloadState.Complete)
            return@flow
        }
        if (destination.exists()) destination.delete()
        if (partial.length() > model.sizeBytes) partial.delete()
        val existing = partial.takeIf { it.isFile }?.length() ?: 0L
        val stats = android.os.StatFs(context.filesDir.absolutePath)
        val available = stats.availableBlocksLong * stats.blockSizeLong
        val required = ((model.sizeBytes - existing).coerceAtLeast(0L) * 1.1).toLong()
        if (available < required) {
            emit(DownloadState.Error("Not enough storage space."))
            return@flow
        }

        val requestBuilder = Request.Builder().url(model.downloadUrl)
        if (existing > 0L) requestBuilder.header("Range", "bytes=$existing-")
        appPreferences.huggingFaceToken.first().trim().takeIf { it.isNotBlank() }?.let { requestBuilder.header("Authorization", "Bearer $it") }
        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 416) {
                    if (partial.length() == model.sizeBytes && ModelValidator.validate(partial, model) is ModelValidationResult.Valid) {
                        promote(partial, destination)
                        emit(DownloadState.Complete)
                    } else {
                        partial.delete()
                        emit(DownloadState.Error("The server rejected the partial download. Please retry."))
                    }
                    return@use
                }
                if (!response.isSuccessful) {
                    val message = if (response.code == 401 || response.code == 403) "Hugging Face access denied. Accept the model license and add a read token." else "Server error (${response.code})"
                    emit(DownloadState.Error(message))
                    return@use
                }
                val resume = response.code == 206 && existing > 0L
                if (resume && !response.header("Content-Range").orEmpty().startsWith("bytes $existing-")) {
                    emit(DownloadState.Error("Server returned an invalid resume range."))
                    return@use
                }
                if (response.header("Content-Type").orEmpty().lowercase().contains("text/html")) {
                    emit(DownloadState.Error("The model server returned an HTML error page."))
                    return@use
                }
                val body = response.body ?: throw IOException("Empty response body")
                var downloaded = if (resume) existing else 0L
                var lastProgress = 0L
                var previousBytes = downloaded
                var previousNanos = System.nanoTime()
                body.byteStream().use { input ->
                    FileOutputStream(partial, resume).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            downloaded += read
                            if (downloaded > model.sizeBytes) throw IOException("Downloaded more bytes than expected")
                            output.write(buffer, 0, read)
                            val now = System.currentTimeMillis()
                            if (now - lastProgress >= PROGRESS_INTERVAL_MS) {
                                val elapsed = (System.nanoTime() - previousNanos) / 1_000_000_000f
                                val speed = if (elapsed > 0f) (downloaded - previousBytes) / 1024f / 1024f / elapsed else 0f
                                emit(DownloadState.Downloading((downloaded.toFloat() / model.sizeBytes).coerceIn(0f, 1f), downloaded / 1024f / 1024f, model.sizeBytes / 1024f / 1024f, speed, if (speed > 0f) ((model.sizeBytes - downloaded) / 1024f / 1024f / speed).toLong() else null))
                                lastProgress = now
                                previousBytes = downloaded
                                previousNanos = System.nanoTime()
                            }
                        }
                        output.flush()
                        output.fd.sync()
                    }
                }
                when (val validation = ModelValidator.validate(partial, model)) {
                    is ModelValidationResult.Valid -> {
                        promote(partial, destination)
                        emit(DownloadState.Complete)
                    }
                    is ModelValidationResult.Invalid -> {
                        partial.delete()
                        emit(DownloadState.Error("Integrity check failed: ${validation.reason}"))
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            emit(DownloadState.Error(error.localizedMessage ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun promote(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object { const val PROGRESS_INTERVAL_MS = 250L }
}
