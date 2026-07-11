package com.example.chatapp.data.repository

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.os.Build
import com.example.chatapp.data.engine.LlamaBridge
import com.example.chatapp.data.model.EngineMessage
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelValidationResult
import com.example.chatapp.data.model.ModelValidator
import com.example.chatapp.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.io.File

@Singleton
class ChatEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val contextWindowManager: ContextWindowManager,
    private val llamaBridge: LlamaBridge
) {
    private val mutex = Mutex()
    private val generationMutex = Mutex()

    @Volatile
    private var sessionHandle: Long = 0L

    @Volatile
    private var currentBackend: String? = null

    @Volatile
    private var currentMaxNumTokens: Int = 0

    suspend fun initialize(backendName: String): Result<Unit> = withContext(Dispatchers.IO) {
        generationMutex.withLock {
            mutex.withLock {
            // Check if already initialized with the same settings
            if (sessionHandle != 0L && currentBackend == backendName) {
                Log.i(TAG, "Engine already active with backend=$backendName")
                return@withLock Result.success(Unit)
            }

            // Clean up existing native resources before re-initializing
            closeInternal()

            try {
                Log.i(TAG, "Starting engine initialization: backend=$backendName")
                checkMemoryAvailability()
                initializeInternal(backendName)
                Log.i(TAG, "Engine initialization successful")
                Result.success(Unit)
            } catch (cancelled: CancellationException) {
                closeInternal()
                throw cancelled
            } catch (throwable: Throwable) {
                Log.e(TAG, "Initialization failed. Attempting CPU fallback...", throwable)
                if (backendName.equals("GPU", ignoreCase = true)) {
                    closeInternal()
                    try {
                        initializeInternal("CPU")
                        appPreferences.updateSelectedBackend("CPU")
                        Result.success(Unit)
                    } catch (fallbackError: Throwable) {
                        closeInternal()
                        Result.failure(fallbackError)
                    }
                } else {
                    closeInternal()
                    Result.failure(throwable)
                }
            }
        }
    }
    }

    fun send(message: EngineMessage, userTurn: String): Flow<String> = flow {
        generationMutex.withLock {
            val maxNumTokens = currentMaxNumTokens.takeIf { it > 0 }
                ?: appPreferences.maxTokens.first().coerceIn(512, 2048)
            val handle = mutex.withLock {
                val activeHandle = sessionHandle
                check(activeHandle != 0L) { "Engine is not initialized." }
                contextWindowManager.refresh(maxNumTokens)
                activeHandle
            }

            val temperature = appPreferences.temperature.first().coerceIn(0.1f, 1.5f)
            val prompt = buildPrompt(handle, message)

            val initResult = llamaBridge.nativeCompletionInit(
                handle = handle,
                prompt = prompt,
                nPredict = maxNumTokens,
                temperature = temperature,
                topK = 40,
                topP = 0.95f,
                seed = 0
            )
            check(initResult == 0) {
                when (initResult) {
                    -2 -> "The conversation no longer fits the model context. Start a new chat."
                    else -> "The model could not process this message (code $initResult)."
                }
            }

            val responseText = StringBuilder()
            var chunkCount = 0
            try {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val piece = llamaBridge.nativeCompletionLoop(handle) ?: break
                    if (piece.isEmpty()) continue
                    chunkCount++
                    Log.d(TAG, "Generation chunk=$chunkCount pieceLength=${piece.length}")
                    responseText.append(piece)
                    emit(piece)
                }
                check(responseText.isNotBlank()) { "Model completed without text output." }
                contextWindowManager.addTurn(userTurn, responseText.toString(), maxNumTokens)
            } catch (cancelled: CancellationException) {
                runCatching { llamaBridge.nativeRequestAbort(handle) }
                if (responseText.isNotBlank()) {
                    contextWindowManager.addTurn(userTurn, responseText.toString(), maxNumTokens)
                }
                throw cancelled
            } finally {
                runCatching { llamaBridge.nativeCompletionEnd(handle) }
            }
            }
        }
        .buffer() // Prevent emission backpressure from slowing down inference
        .onCompletion { Log.i(TAG, "Generation flow completed") }
        .flowOn(Dispatchers.Default) // Run computational heavy work on Default dispatcher

    suspend fun clearConversation() = withContext(Dispatchers.IO) {
        cancelGeneration()
        generationMutex.withLock {
            mutex.withLock {
                contextWindowManager.reset(currentMaxNumTokens)
            }
        }
    }

    suspend fun restoreConversation(turns: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        cancelGeneration()
        generationMutex.withLock {
            mutex.withLock {
                val contextBudget = currentMaxNumTokens.takeIf { it > 0 } ?: appPreferences.maxTokens.first().coerceIn(512, 2048)
                contextWindowManager.replaceTurns(turns, contextBudget)
            }
        }
    }

    fun cancelGeneration() {
        val handle = sessionHandle
        if (handle != 0L) {
            runCatching { llamaBridge.nativeRequestAbort(handle) }
                .onFailure { Log.w(TAG, "Native generation cancellation failed", it) }
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        cancelGeneration()
        generationMutex.withLock {
            mutex.withLock {
                closeInternal()
                contextWindowManager.reset()
            }
        }
    }

    fun isReady(): Boolean = sessionHandle != 0L

    private suspend fun initializeInternal(backendName: String) {
        check(Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "x86_64" }) {
            "This device ABI is not supported by the llama.cpp native runtime."
        }
        if (backendName.equals("GPU", ignoreCase = true)) {
            // This build compiles llama.cpp for CPU only; the caller falls back to CPU.
            throw UnsupportedOperationException("GPU inference is not available in the llama.cpp build; CPU is used instead.")
        }

        val modelPath = getValidatedModelPath()
        val configuredMaxTokens = appPreferences.maxTokens.first().coerceIn(512, 2048)
        currentMaxNumTokens = configuredMaxTokens

        llamaBridge.ensureLoaded()
        val threads = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 6)
        val handle = llamaBridge.nativeLoadModel(modelPath, CONTEXT_SIZE, threads)
        check(handle != 0L) { "The model failed to load. It may be corrupted or too large for this device." }
        sessionHandle = handle
        currentBackend = "CPU"
    }

    private fun buildPrompt(handle: Long, userMessage: EngineMessage): String {
        val messages = buildList {
            add(EngineMessage.system(SYSTEM_PROMPT))
            addAll(contextWindowManager.retainedMessages())
            add(userMessage)
        }
        val roles = messages.map { it.role }.toTypedArray()
        val contents = messages.map { it.text }.toTypedArray()
        val prompt = llamaBridge.nativeApplyChatTemplate(handle, roles, contents)
        check(!prompt.isNullOrBlank()) { "Failed to format the conversation for the model." }
        return prompt
    }

    private suspend fun getValidatedModelPath(): String {
        val selectedModelId = appPreferences.selectedModel.first()
        val model = ModelCatalog.requireById(selectedModelId)
        val file = File(context.filesDir, "models/${model.fileName}")

        when (val validation = ModelValidator.validate(file, model)) {
            is ModelValidationResult.Valid -> Unit
            is ModelValidationResult.Invalid -> throw IllegalStateException("Model validation failed: ${validation.reason}")
        }
        return file.absolutePath
    }

    private fun checkMemoryAvailability() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // Basic safety check: threshold of 256MB free to attempt load
        if (memoryInfo.lowMemory || memoryInfo.availMem < 256 * 1024 * 1024) {
            Log.w(TAG, "System reporting low memory state.")
        }
    }

    private fun closeInternal() {
        val handle = sessionHandle
        sessionHandle = 0L
        currentBackend = null
        currentMaxNumTokens = 0
        if (handle != 0L) {
            try {
                llamaBridge.nativeFree(handle)
            } catch (e: Exception) {
                Log.e(TAG, "Error closing engine", e)
            }
        }
    }

    private companion object {
        const val TAG = "ChatEngineManager"

        // All cataloged GGUF models support at least a 4096-token context.
        const val CONTEXT_SIZE = 4096

        const val SYSTEM_PROMPT =
            "You are a helpful private AI assistant running fully on this device. Be concise and accurate. Respond in plain text only unless the user explicitly asks for code or markdown."
    }
}
