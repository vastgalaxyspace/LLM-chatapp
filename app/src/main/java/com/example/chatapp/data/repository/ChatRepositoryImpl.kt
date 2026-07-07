package com.example.chatapp.data.repository

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message
import com.example.chatapp.data.model.MessageAttachment
import com.example.chatapp.data.model.MessageType
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
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
        temperature: Float,
        maxTokens: Int,
        attachments: List<MessageAttachment>
    ): Flow<String> = flow {
        val selectedModel = ModelCatalog.fromId(appPreferences.selectedModel.first())
        val formattedPrompt = buildUserPrompt(message, selectedModel)
        emitAll(engineManager.send(buildUserMessage(formattedPrompt, attachments), formattedPrompt))
    }

    private fun buildUserMessage(formattedPrompt: String, attachments: List<MessageAttachment>): Message {
        if (attachments.isEmpty()) {
            return Message.user(formattedPrompt)
        }

        val parts = buildList {
            add(Content.Text(formattedPrompt))
            attachments.forEach { attachment ->
                when (attachment.type) {
                    MessageType.IMAGE -> add(Content.ImageFile(attachment.path))
                    MessageType.AUDIO -> add(Content.AudioFile(attachment.path))
                    MessageType.TEXT -> Unit
                }
            }
        }

        return Message.user(Contents.of(parts))
    }

    private fun buildUserPrompt(message: String, model: ModelOption): String =
        model.chatTemplate.format(
            userMessage = if (model.id == ModelCatalog.DEEPSEEK_R1_QWEN_1_5B) "$message\n/no_think" else message,
            systemPrompt = SYSTEM_PROMPT
        )

    override suspend fun clearConversation() = engineManager.clearConversation()

    override suspend fun closeEngine() = engineManager.close()

    override fun isEngineReady(): Boolean = engineManager.isReady()

    private companion object {
        const val SYSTEM_PROMPT =
            "You are a helpful private AI assistant running fully on this device. Be concise and accurate. Respond in plain text only unless the user explicitly asks for code or markdown."
    }
}
