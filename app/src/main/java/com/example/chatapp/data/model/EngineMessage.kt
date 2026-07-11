package com.example.chatapp.data.model

/** A single chat-template message handed to the llama.cpp engine. */
data class EngineMessage(val role: String, val text: String) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"

        fun system(text: String) = EngineMessage(ROLE_SYSTEM, text)
        fun user(text: String) = EngineMessage(ROLE_USER, text)
        fun model(text: String) = EngineMessage(ROLE_ASSISTANT, text)
    }
}
