package com.example.chatapp.data.repository

import com.example.chatapp.data.model.EngineMessage
import com.example.chatapp.data.model.MessageAttachment
import com.example.chatapp.data.model.MessageType
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val engineManager: ChatEngineManager,
    private val appPreferences: AppPreferences
) : ChatRepository {
    override suspend fun initializeEngine(backend: String): Result<Unit> = engineManager.initialize(backend)

    override fun sendMessage(
        message: String,
        attachments: List<MessageAttachment>
    ): Flow<String> = flow {
        val selectedModel = ModelCatalog.fromId(appPreferences.selectedModel.first())
        val prompt = if (selectedModel.id == ModelCatalog.DEEPSEEK_R1_QWEN_1_5B) {
            "$message\n/no_think"
        } else {
            message
        }
        emitAll(engineManager.send(buildUserMessage(prompt, attachments), prompt))
    }

    private fun buildUserMessage(prompt: String, attachments: List<MessageAttachment>): EngineMessage {
        // Multimodal inference is intentionally disabled in this text-first build;
        // media attachments are stored locally but never sent to the engine.
        val hasMedia = attachments.any { it.type == MessageType.IMAGE || it.type == MessageType.AUDIO }
        if (hasMedia) {
            android.util.Log.w("ChatRepositoryImpl", "Attachments ignored: multimodal inference is disabled.")
        }
        return EngineMessage.user(prompt)
    }

    override suspend fun clearConversation() = engineManager.clearConversation()

    override suspend fun restoreConversation(turns: List<Pair<String, String>>) = engineManager.restoreConversation(turns)

    override fun cancelGeneration() = engineManager.cancelGeneration()

    override suspend fun closeEngine() = engineManager.close()

    override fun isEngineReady(): Boolean = engineManager.isReady()
}
