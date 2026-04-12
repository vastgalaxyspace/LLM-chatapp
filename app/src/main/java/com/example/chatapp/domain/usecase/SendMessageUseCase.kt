package com.example.chatapp.domain.usecase

import com.example.chatapp.data.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(
        message: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<String> = chatRepository.sendMessage(message, temperature, maxTokens)
}
