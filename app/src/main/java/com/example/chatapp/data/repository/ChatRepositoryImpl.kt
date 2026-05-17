package com.example.chatapp.data.repository

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message
import com.example.chatapp.data.model.MessageAttachment
import com.example.chatapp.data.model.MessageType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val engineManager: ChatEngineManager
) : ChatRepository {
    override suspend fun initializeEngine(backend: String): Result<Unit> = engineManager.initialize(backend)

    override fun sendMessage(
        message: String,
        temperature: Float,
        maxTokens: Int,
        attachments: List<MessageAttachment>
    ): Flow<String> = engineManager.send(buildUserMessage(message, attachments))

    private fun buildUserMessage(message: String, attachments: List<MessageAttachment>): Message {
        if (attachments.isEmpty()) {
            return Message.user(buildUserPrompt(message))
        }

        val parts = buildList {
            add(Content.Text(buildUserPrompt(message)))
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

    private fun buildUserPrompt(message: String): String {
        return """
            Reply with the final answer only.
            Start immediately with the answer.
            Give a complete, helpful answer with enough detail for the question.
            Use simple plain text only.
            Do not use Markdown, XML, hidden reasoning, chain-of-thought, or <think> tags.

            Question: $message
            /no_think
        """.trimIndent()
    }

    override suspend fun clearConversation() = engineManager.clearConversation()

    override suspend fun closeEngine() = engineManager.close()

    override fun isEngineReady(): Boolean = engineManager.isReady()
}
