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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request

// The missing definition:
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
        val tempFile = File(modelsDirectory, "${selectedModel.fileName}.part")

        // 1. Integrity Check
        if (destination.exists() && destination.length() > 0L) {
            if (destination.hasValidChecksum(selectedModel)) {
                emit(DownloadState.Complete)
                return@flow
            } else {
                destination.delete()
            }
        }

        // 2. Storage Pre-check
        val stats = android.os.StatFs(context.filesDir.absolutePath)
        val availableBytes = stats.availableBlocksLong * stats.blockSizeLong
        val requiredBytes = (selectedModel.sizeMb * 1024 * 1024 * 1.1).toLong()
        if (availableBytes < requiredBytes) {
            emit(DownloadState.Error("Not enough storage space."))
            return@flow
        }

        val requestBuilder = Request.Builder().url(selectedModel.downloadUrl)
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
        if (existingBytes > 0L) requestBuilder.header("Range", "bytes=$existingBytes-")

        val hfToken = appPreferences.huggingFaceToken.first()
        if (hfToken.isNotBlank()) requestBuilder.header("Authorization", "Bearer $hfToken")

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val isResuming = response.code == 206
                if (!response.isSuccessful) {
                    emit(DownloadState.Error("Server error (${response.code})"))
                    return@flow
                }

                val body = response.body ?: throw IOException("Empty body")
                val totalBytes = (if (isResuming) existingBytes else 0L) + body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(tempFile, isResuming).use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = if (isResuming) existingBytes else 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            emit(DownloadState.Downloading(
                                progress = (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f),
                                downloadedMB = downloaded / 1024f / 1024f,
                                totalMB = totalBytes / 1024f / 1024f
                            ))
                        }
                    }
                }

                if (tempFile.hasValidChecksum(selectedModel) && tempFile.renameTo(destination)) {
                    emit(DownloadState.Complete)
                } else {
                    emit(DownloadState.Error("Integrity check failed."))
                }
            }
        } catch (e: Exception) {
            emit(DownloadState.Error(e.localizedMessage ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun File.hasValidChecksum(model: ModelOption): Boolean {
        val expected = model.sha256?.trim()?.lowercase() ?: return true
        if (expected.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == expected
    }
}