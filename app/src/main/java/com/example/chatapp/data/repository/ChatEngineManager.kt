package com.example.chatapp.data.repository

import android.content.Context
import android.util.Log
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class ChatEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var conversation: Conversation? = null

    @Volatile
    private var currentBackend: String? = null

    suspend fun initialize(backendName: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (engine != null && conversation != null && currentBackend == backendName) {
                Log.i(TAG, "Engine already initialized with backend=$backendName")
                return@withLock Result.success(Unit)
            }

            runCatching {
                Log.i(TAG, "Initializing engine with backend=$backendName")
                initializeInternal(backendName)
                Log.i(TAG, "Engine initialized with backend=$backendName")
                Unit
            }.recoverCatching { throwable ->
                Log.e(TAG, "Engine initialization failed with backend=$backendName", throwable)
                if (backendName.equals("GPU", ignoreCase = true)) {
                    Log.i(TAG, "Retrying engine initialization with CPU fallback")
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

    fun send(message: Message): Flow<String> = flow {
        val activeConversation = mutex.withLock {
            val activeEngine = engine ?: throw IllegalStateException("Engine is not initialized.")
            conversation ?: createConfiguredConversation(activeEngine).also { conversation = it }
        }

        var previousText = ""
        var chunkCount = 0
        Log.i(TAG, "Starting generation")
        activeConversation.sendMessageAsync(message).collect { chunk ->
            chunkCount += 1
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
        Log.i(TAG, "Generation completed chunks=$chunkCount")
    }.flowOn(Dispatchers.IO)

    suspend fun clearConversation() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val activeEngine = engine ?: return@withLock
            conversation?.close()
            conversation = createConfiguredConversation(activeEngine)
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
        }
    }

    fun isReady(): Boolean = engine != null && conversation != null

    private suspend fun initializeInternal(backendName: String) {
        closeInternal()
        val configuredMaxTokens = appPreferences.maxTokens.first().coerceIn(512, 2048)
        val selectedBackend = if (backendName.equals("CPU", ignoreCase = true)) {
            Backend.CPU()
        } else {
            Backend.GPU()
        }

        val createdEngine = Engine(
            EngineConfig(
                modelPath = modelPath(),
                backend = selectedBackend,
                maxNumTokens = configuredMaxTokens
            )
        )
        createdEngine.initialize()

        engine = createdEngine
        conversation = createConfiguredConversation(createdEngine)
        currentBackend = backendName
    }

    private suspend fun modelPath(): String {
        val selectedModel = ModelCatalog.fromId(appPreferences.selectedModel.first())
        return "${context.filesDir.absolutePath}/models/${selectedModel.fileName}"
    }

    private suspend fun createConfiguredConversation(activeEngine: Engine): Conversation {
        val configuredTemperature = appPreferences.temperature.first().coerceIn(0.1f, 1.5f)
        return activeEngine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 40,
                    topP = 0.95,
                    temperature = configuredTemperature.toDouble(),
                    seed = 0
                )
            )
        )
    }

    private fun closeInternal() {
        try {
            conversation?.close()
        } finally {
            conversation = null
            try {
                engine?.close()
            } finally {
                engine = null
                currentBackend = null
            }
        }
    }

    private companion object {
        const val TAG = "ChatEngineManager"
    }
}
