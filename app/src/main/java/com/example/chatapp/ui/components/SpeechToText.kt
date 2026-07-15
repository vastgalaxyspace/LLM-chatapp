package com.example.chatapp.ui.components

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Lightweight controller around the platform [SpeechRecognizer]. Exposes listening state and
 * drives partial/final transcripts through callbacks so the caller can update its text field live.
 */
class SpeechToTextController internal constructor(
    private val context: Context
) {
    private var recognizer: SpeechRecognizer? = null

    var isListening by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    /** Called with the growing transcript while speaking. */
    var onPartial: (String) -> Unit = {}
    /** Called once recognition finishes with the final transcript. */
    var onFinal: (String) -> Unit = {}

    fun start() {
        if (isListening) return
        if (!isAvailable) {
            errorMessage = "Speech recognition is not available on this device."
            return
        }
        errorMessage = null

        val sr = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { if (it.isNotBlank()) onPartial(it) }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onFinal(text)
                finish()
            }

            override fun onError(error: Int) {
                errorMessage = errorText(error)
                finish()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
        }

        isListening = true
        sr.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    private fun finish() {
        isListening = false
        recognizer?.destroy()
        recognizer = null
    }

    internal fun dispose() {
        finish()
    }

    private fun errorText(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Network error during recognition."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy. Try again."
        else -> "Speech recognition failed."
    }
}

/** Remembers a [SpeechToTextController] and tears it down when leaving composition. */
@Composable
fun rememberSpeechToTextController(
    onPartial: (String) -> Unit,
    onFinal: (String) -> Unit
): SpeechToTextController {
    val context = LocalContext.current
    val partial by rememberUpdatedState(onPartial)
    val final by rememberUpdatedState(onFinal)

    val controller = remember(context) {
        SpeechToTextController(context.applicationContext)
    }
    controller.onPartial = { partial(it) }
    controller.onFinal = { final(it) }

    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}
