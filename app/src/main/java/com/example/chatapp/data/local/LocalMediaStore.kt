package com.example.chatapp.data.local

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredMedia(
    val path: String,
    val mimeType: String?,
    val sizeBytes: Long
)

@Singleton
class LocalMediaStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun copyImage(uri: Uri): StoredMedia = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri) ?: "image/*"
        val extension = extensionFor(mimeType) ?: "jpg"
        val target = File(imageDirectory(), "image_${System.currentTimeMillis()}.$extension")

        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Could not open selected image.")

        StoredMedia(
            path = target.absolutePath,
            mimeType = mimeType,
            sizeBytes = target.length()
        )
    }

    fun createAudioTarget(): File {
        val directory = audioDirectory()
        return File(directory, "audio_${System.currentTimeMillis()}.m4a")
    }

    fun storedAudio(file: File): StoredMedia =
        StoredMedia(
            path = file.absolutePath,
            mimeType = "audio/mp4",
            sizeBytes = file.length()
        )

    suspend fun deleteAllChatMedia() = withContext(Dispatchers.IO) {
        File(context.filesDir, "chat_media").deleteRecursively()
    }

    private fun imageDirectory(): File =
        File(context.filesDir, "chat_media/images").apply { mkdirs() }

    private fun audioDirectory(): File =
        File(context.filesDir, "chat_media/audio").apply { mkdirs() }

    private fun extensionFor(mimeType: String): String? =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
}
