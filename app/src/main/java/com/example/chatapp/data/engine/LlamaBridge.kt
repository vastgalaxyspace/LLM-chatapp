package com.example.chatapp.data.engine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin JNI surface over the bundled llama.cpp build (libinnoai_llama.so).
 *
 * All session calls must be externally serialized; only [nativeRequestAbort]
 * is safe to call concurrently with a running completion.
 */
@Singleton
class LlamaBridge @Inject constructor() {

    fun ensureLoaded() {
        synchronized(LlamaBridge) {
            if (!libraryLoaded) {
                System.loadLibrary("innoai_llama")
                nativeBackendInit()
                libraryLoaded = true
            }
        }
    }

    external fun nativeBackendInit()

    /** Returns a session handle, or 0 when the model could not be loaded. */
    external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): Long

    /** Formats a conversation with the model's chat template (assistant turn appended). */
    external fun nativeApplyChatTemplate(
        handle: Long,
        roles: Array<String>,
        contents: Array<String>
    ): String?

    /** Prefills the prompt and prepares sampling. Returns 0 on success (see llama_jni.cpp for error codes). */
    external fun nativeCompletionInit(
        handle: Long,
        prompt: String,
        nPredict: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        seed: Int
    ): Int

    /** Returns the next text piece, "" while a multi-byte character is incomplete, or null when done. */
    external fun nativeCompletionLoop(handle: Long): String?

    external fun nativeCompletionEnd(handle: Long)

    external fun nativeRequestAbort(handle: Long)

    external fun nativeFree(handle: Long)

    private companion object {
        @Volatile
        private var libraryLoaded = false
    }
}
