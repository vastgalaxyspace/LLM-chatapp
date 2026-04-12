package com.example.chatapp.data.repository

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun initializeEngine(backend: String): Result<Unit>
    fun sendMessage(message: String, temperature: Float, maxTokens: Int): Flow<String>
    suspend fun clearConversation()
    suspend fun closeEngine()
    fun isEngineReady(): Boolean
}
