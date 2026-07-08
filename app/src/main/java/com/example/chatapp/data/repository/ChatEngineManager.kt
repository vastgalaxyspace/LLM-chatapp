package com.example.chatapp.data.repository

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@Singleton
class ChatEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val contextWindowManager: ContextWindowManager
) {
    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var conversation: Conversation? = null

    @Volatile
    private var currentBackend: String? = null

    @Volatile
    private var currentMaxNumTokens: Int = 0

    suspend fun initialize(backendName: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Check if already initialized with the same settings
            if (engine != null && conversation != null && currentBackend == backendName) {
                Log.i(TAG, "Engine already active with backend=$backendName")
                return@withLock Result.success(Unit)
            }

            // High-priority: Clean up existing native resources before re-initializing
            closeInternal()

            runCatching {
                Log.i(TAG, "Starting engine initialization: backend=$backendName")
                checkMemoryAvailability()
                initializeInternal(backendName)
                Log.i(TAG, "Engine initialization successful")
                Unit
            }.recoverCatching { throwable ->
                Log.e(TAG, "Initialization failed. Attempting CPU fallback...", throwable)
                if (backendName.equals("GPU", ignoreCase = true)) {
                    // Force complete cleanup before fallback
                    closeInternal()
                    initializeInternal("CPU")
                    appPreferences.updateSelectedBackend("CPU")
                    Unit
                } else {
                    closeInternal()
                    throw throwable
                }
            }
        }
    }

    fun send(message: Message, userTurn: String): Flow<String> = flow {
        val maxNumTokens = currentMaxNumTokens.takeIf { it > 0 }
            ?: appPreferences.maxTokens.first().coerceIn(512, 2048)

        val activeConversation = mutex.withLock {
            val activeEngine = engine ?: throw IllegalStateException("Engine is not initialized.")
            var activeConv = conversation ?: createConfiguredConversation(activeEngine).also { conversation = it }

            contextWindowManager.refresh(maxNumTokens)

            if (contextWindowManager.shouldRebuildConversation(maxNumTokens)) {
                Log.i(TAG, "Threshold reached. Rebuilding conversation context.")
                activeConv.close()
                activeConv = createConfiguredConversation(
                    activeEngine = activeEngine,
                    initialMessages = contextWindowManager.retainedMessages()
                )
                conversation = activeConv
            }
            activeConv
        }

        var previousText = ""
        var chunkCount = 0

        activeConversation.sendMessageAsync(message)
            .collect { chunk ->
                chunkCount++
                val fullText = chunk.toString()
                val delta = if (fullText.startsWith(previousText)) {
                    fullText.removePrefix(previousText)
                } else {
                    fullText
                }
                previousText = fullText

                if (delta.isNotEmpty()) {
                    emit(delta)
                }
            }

        contextWindowManager.addTurn(userTurn, previousText, maxNumTokens)
    }
        .buffer() // Prevent emission backpressure from slowing down inference
        .onCompletion { Log.i(TAG, "Generation flow completed") }
        .flowOn(Dispatchers.Default) // Run computational heavy work on Default dispatcher

    suspend fun clearConversation() = withContext(Dispatchers.IO) {
        mutex.withLock {
            contextWindowManager.reset(currentMaxNumTokens)
            val activeEngine = engine ?: return@withLock
            conversation?.close()
            conversation = createConfiguredConversation(activeEngine)
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
            contextWindowManager.reset()
        }
    }

    fun isReady(): Boolean = engine != null && conversation != null

    private suspend fun initializeInternal(backendName: String) {
        val modelPath = getValidatedModelPath()
        val configuredMaxTokens = appPreferences.maxTokens.first().coerceIn(512, 2048)
        currentMaxNumTokens = configuredMaxTokens

        val selectedBackend = if (backendName.equals("GPU", ignoreCase = true)) {
            Backend.GPU()
        } else {
            Backend.CPU()
        }

        val createdEngine = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = selectedBackend,
                maxNumTokens = configuredMaxTokens
            )
        )
        createdEngine.initialize()

        engine = createdEngine
        conversation = createConfiguredConversation(createdEngine)
        currentBackend = backendName
    }

    private suspend fun getValidatedModelPath(): String {
        val selectedModelId = appPreferences.selectedModel.first()
        val model = ModelCatalog.fromId(selectedModelId)
        val file = File(context.filesDir, "models/${model.fileName}")

        if (!file.exists() || file.length() <= 0L) {
            throw IllegalStateException("Model file not found at ${file.absolutePath}")
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

    private suspend fun createConfiguredConversation(
        activeEngine: Engine,
        initialMessages: List<Message> = emptyList()
    ): Conversation {
        val temperature = appPreferences.temperature.first().coerceIn(0.1f, 1.5f)
        return activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_PROMPT),
                initialMessages = initialMessages,
                samplerConfig = SamplerConfig(
                    topK = 40,
                    topP = 0.95,
                    temperature = temperature.toDouble(),
                    seed = 0
                )
            )
        )
    }

    private fun closeInternal() {
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing conversation", e)
        } finally {
            conversation = null
            try {
                engine?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing engine", e)
            } finally {
                engine = null
                currentBackend = null
                currentMaxNumTokens = 0
            }
        }
    }

    private companion object {
        const val TAG = "ChatEngineManager"
        const val SYSTEM_PROMPT =
            "You are a helpful private AI assistant running fully on this device. Be concise and accurate. Respond in plain text only unless the user explicitly asks for code or markdown."
    }
}
