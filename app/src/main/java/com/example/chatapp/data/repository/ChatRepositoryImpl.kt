package com.example.chatapp.data.repository

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) : ChatRepository {
    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var conversation: Conversation? = null

    @Volatile
    private var currentBackend: String? = null

    private suspend fun modelPath(): String {
        val selectedModel = ModelCatalog.fromId(appPreferences.selectedModel.first())
        return "${context.filesDir.absolutePath}/models/${selectedModel.fileName}"
    }

    override suspend fun initializeEngine(backendName: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (engine != null && conversation != null && currentBackend == backendName) {
                return@withLock Result.success(Unit)
            }

            runCatching {
                initializeEngineInternal(backendName)
            }.recoverCatching { throwable ->
                if (backendName.equals("GPU", ignoreCase = true)) {
                    initializeEngineInternal("CPU")
                    appPreferences.updateSelectedBackend("CPU")
                } else {
                    closeInternal()
                    throw throwable
                }
            }
        }
    }

    override fun sendMessage(message: String, temperature: Float, maxTokens: Int): Flow<String> = flow {
        val activeConversation = mutex.withLock {
            val activeEngine = engine ?: throw IllegalStateException("Engine is not initialized.")
            conversation ?: createConfiguredConversation(activeEngine).also { conversation = it }
        }

        var previousText = ""
        activeConversation.sendMessageAsync(Message.of(message)).collect { chunk ->
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
    }.flowOn(Dispatchers.IO)

    override suspend fun clearConversation() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val activeEngine = engine ?: return@withLock
                conversation?.close()
                conversation = createConfiguredConversation(activeEngine)
            }
        }
    }

    override suspend fun closeEngine() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                closeInternal()
            }
        }
    }

    override fun isEngineReady(): Boolean = engine != null && conversation != null

    private suspend fun initializeEngineInternal(backendName: String) {
        closeInternal()
        val configuredMaxTokens = appPreferences.maxTokens.first().coerceIn(128, 2048)
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

        val createdConversation = createConfiguredConversation(createdEngine)

        engine = createdEngine
        conversation = createdConversation
        currentBackend = backendName
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
}
