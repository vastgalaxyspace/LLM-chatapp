package com.example.chatapp.domain.usecase

import android.content.Context
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import javax.inject.Inject
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class DownloadState {
    data class Downloading(
        val progress: Float,
        val downloadedMB: Float,
        val totalMB: Float,
        val speedMBps: Float = 0f,
        val etaSeconds: Long? = null
    ) : DownloadState()

    data object Complete : DownloadState()

    data class Error(val message: String) : DownloadState()
}

class DownloadModelUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    private val client = OkHttpClient()

    operator fun invoke(modelId: String? = null): Flow<DownloadState> = flow {
        val selectedModel = ModelCatalog.fromId(modelId ?: appPreferences.selectedModel.first())
        val modelsDirectory = File(context.filesDir, "models")
        if (!modelsDirectory.exists() && !modelsDirectory.mkdirs()) {
            emit(DownloadState.Error("Unable to create models directory."))
            return@flow
        }

        val destination = File(modelsDirectory, selectedModel.fileName)
        if (destination.exists() && destination.length() > 0L && destination.hasValidChecksum(selectedModel)) {
            emit(DownloadState.Complete)
            return@flow
        }
        if (destination.exists()) {
            destination.delete()
        }

        val tempFile = File(modelsDirectory, "${selectedModel.fileName}.part")
        val requestBuilder = Request.Builder().url(selectedModel.downloadUrl)
        val existingBytes = tempFile.length().takeIf { it > 0L } ?: 0L
        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }
        val hfToken = appPreferences.huggingFaceToken.first()
        if (hfToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $hfToken")
        }
        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                val rangeAccepted = existingBytes > 0L && response.code == 206
                if (existingBytes > 0L && response.code == 200) {
                    tempFile.delete()
                }

                if (!response.isSuccessful || (existingBytes > 0L && !rangeAccepted && response.code != 200)) {
                    val message = when (response.code) {
                        401 -> selectedModel.accessErrorMessage()
                        403 -> selectedModel.accessErrorMessage()
                        404 -> "Model file is currently unavailable on the server. Please try again later."
                        429 -> "Too many download attempts right now. Wait a bit, then retry."
                        in 500..599 -> "Model server is having a problem right now. Please retry in a few minutes."
                        else -> "Download failed (error ${response.code}). Please try again."
                    }
                    emit(DownloadState.Error(message))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(DownloadState.Error("Download failed because the server returned empty data. Please retry."))
                    return@flow
                }

                val fallbackTotalBytes = (selectedModel.sizeMb * 1024 * 1024).toLong()
                val resumedBytes = if (rangeAccepted) existingBytes else 0L
                val responseBytes = body.contentLength().takeIf { it > 0L }
                val totalBytes = responseBytes?.plus(resumedBytes) ?: fallbackTotalBytes

                body.byteStream().use { input ->
                    FileOutputStream(tempFile, rangeAccepted).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloadedBytes = resumedBytes
                        val startedAt = System.currentTimeMillis()

                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val elapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1000f).coerceAtLeast(0.25f)
                            val currentSessionBytes = downloadedBytes - resumedBytes
                            val speedBytesPerSecond = currentSessionBytes / elapsedSeconds
                            val remainingBytes = (totalBytes - downloadedBytes).coerceAtLeast(0L)

                            emit(
                                DownloadState.Downloading(
                                    progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f),
                                    downloadedMB = downloadedBytes / 1024f / 1024f,
                                    totalMB = totalBytes / 1024f / 1024f,
                                    speedMBps = speedBytesPerSecond / 1024f / 1024f,
                                    etaSeconds = if (speedBytesPerSecond > 0f) {
                                        (remainingBytes / speedBytesPerSecond).toLong()
                                    } else {
                                        null
                                    }
                                )
                            )
                        }

                        output.flush()
                    }
                }

                if (destination.exists()) {
                    destination.delete()
                }

                if (!tempFile.renameTo(destination)) {
                    emit(DownloadState.Error("Download finished but could not save the model file. Check storage space and retry."))
                    return@flow
                }

                if (!destination.hasValidChecksum(selectedModel)) {
                    destination.delete()
                    emit(DownloadState.Error("Download integrity check failed. Please retry on a stable connection."))
                    return@flow
                }

                emit(DownloadState.Complete)
            }
        } catch (throwable: Throwable) {
            emit(DownloadState.Error(toUserDownloadError(throwable)))
        }
    }.flowOn(Dispatchers.IO)

    private fun File.hasValidChecksum(model: ModelOption): Boolean {
        val expected = model.sha256?.trim()?.lowercase().takeUnless { it.isNullOrBlank() } ?: return true
        return sha256() == expected
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun toUserDownloadError(throwable: Throwable): String {
        val normalized = throwable.message.orEmpty().lowercase()
        return when {
            throwable is UnknownHostException ||
                normalized.contains("unable to resolve host") ||
                normalized.contains("no address associated with hostname") ->
                "No internet connection detected. Check your network and try again."
            throwable is SocketTimeoutException ||
                normalized.contains("timeout") ||
                normalized.contains("timed out") ->
                "Download took too long. Please check your connection and retry."
            throwable is SSLException ||
                normalized.contains("ssl") ||
                normalized.contains("certificate") ->
                "Secure connection failed. Check your device date/time and internet, then retry."
            normalized.contains("no space left") ||
                normalized.contains("enospc") ->
                "Not enough storage space to download this model. Free space and try again."
            throwable is IOException ->
                "Download failed due to a file or network issue. Please retry."
            else ->
                "Something went wrong while downloading. Please try again."
        }
    }

    private fun ModelOption.accessErrorMessage(): String =
        if (requiresHuggingFaceAccess) {
            "$displayName requires Hugging Face access. Accept the model license on Hugging Face, add your token in Settings, then retry."
        } else {
            "Access denied for $displayName. Check your Hugging Face token or try again later."
        }
}
