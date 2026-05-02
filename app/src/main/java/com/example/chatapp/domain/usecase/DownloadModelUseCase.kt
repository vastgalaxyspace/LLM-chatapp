package com.example.chatapp.domain.usecase

import android.content.Context
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
        val totalMB: Float
    ) : DownloadState()

    data object Complete : DownloadState()

    data class Error(val message: String) : DownloadState()
}

class DownloadModelUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    private val client = OkHttpClient()

    operator fun invoke(): Flow<DownloadState> = flow {
        val selectedModel = ModelCatalog.fromId(appPreferences.selectedModel.first())
        val modelsDirectory = File(context.filesDir, "models")
        if (!modelsDirectory.exists() && !modelsDirectory.mkdirs()) {
            emit(DownloadState.Error("Unable to create models directory."))
            return@flow
        }

        val destination = File(modelsDirectory, selectedModel.fileName)
        if (destination.exists() && destination.length() > 0L) {
            emit(DownloadState.Complete)
            return@flow
        }

        val tempFile = File(modelsDirectory, "${selectedModel.fileName}.part")
        val requestBuilder = Request.Builder().url(selectedModel.downloadUrl)
        val hfToken = appPreferences.huggingFaceToken.first()
        if (hfToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $hfToken")
        }
        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
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
                val totalBytes = body.contentLength().takeIf { it > 0L } ?: fallbackTotalBytes

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloadedBytes = 0L

                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            emit(
                                DownloadState.Downloading(
                                    progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f),
                                    downloadedMB = downloadedBytes / 1024f / 1024f,
                                    totalMB = totalBytes / 1024f / 1024f
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

                emit(DownloadState.Complete)
            }
        } catch (throwable: Throwable) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            emit(DownloadState.Error(toUserDownloadError(throwable)))
        }
    }.flowOn(Dispatchers.IO)

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
