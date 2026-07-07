package com.example.chatapp.data.model

sealed class ChatTemplate {
    abstract fun format(userMessage: String, systemPrompt: String?): String

    data object Gemma : ChatTemplate() {
        override fun format(userMessage: String, systemPrompt: String?): String = buildString {
            append("<start_of_turn>user\n")
            systemPrompt?.takeIf { it.isNotBlank() }?.let {
                append(it)
                append('\n')
            }
            append(userMessage)
            append("<end_of_turn>\n<start_of_turn>model\n")
        }
    }

    data object ChatML : ChatTemplate() {
        override fun format(userMessage: String, systemPrompt: String?): String = buildString {
            systemPrompt?.takeIf { it.isNotBlank() }?.let {
                append("<|im_start|>system\n")
                append(it)
                append("<|im_end|>\n")
            }
            append("<|im_start|>user\n")
            append(userMessage)
            append("<|im_end|>\n<|im_start|>assistant\n")
        }
    }

    data object Phi : ChatTemplate() {
        override fun format(userMessage: String, systemPrompt: String?): String = buildString {
            systemPrompt?.takeIf { it.isNotBlank() }?.let {
                append("<|system|>\n")
                append(it)
                append("<|end|>\n")
            }
            append("<|user|>\n")
            append(userMessage)
            append("<|end|>\n<|assistant|>\n")
        }
    }

    data object PlainInstruct : ChatTemplate() {
        override fun format(userMessage: String, systemPrompt: String?): String = userMessage
    }
}
