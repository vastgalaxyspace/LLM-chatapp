package com.example.chatapp.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
        val mimeType = "image/jpeg"
        val target = File(imageDirectory(), "image_${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            val original = BitmapFactory.decodeStream(input)
                ?: error("Could not decode selected image.")

            val maxEdge = 1024
            val scale = minOf(maxEdge.toFloat() / original.width, maxEdge.toFloat() / original.height, 1f)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                )
            } else {
                original
            }

            target.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            if (scaled !== original) scaled.recycle()
            original.recycle()
        } ?: error("Could not open selected image.")

        StoredMedia(path = target.absolutePath, mimeType = mimeType, sizeBytes = target.length())
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
}
