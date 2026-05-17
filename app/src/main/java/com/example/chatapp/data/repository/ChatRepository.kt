package com.example.chatapp.data.repository

import com.example.chatapp.data.model.MessageAttachment
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun initializeEngine(backend: String): Result<Unit>
    fun sendMessage(
        message: String,
        temperature: Float,
        maxTokens: Int,
        attachments: List<MessageAttachment> = emptyList()
    ): Flow<String>
    suspend fun clearConversation()
    suspend fun closeEngine()
    fun isEngineReady(): Boolean
}
