package com.example.chatapp.data.model

data class MessageAttachment(
    val path: String,
    val type: MessageType,
    val mimeType: String? = null
)
