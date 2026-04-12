package com.example.chatapp

import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.model.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun fromId_returnsSelectedModelWhenPresent() {
        val option = ModelCatalog.fromId(ModelCatalog.GEMMA)
        assertEquals(ModelCatalog.GEMMA, option.id)
    }

    @Test
    fun fromId_fallsBackToFirstModelForUnknownId() {
        val option = ModelCatalog.fromId("missing-model-id")
        assertEquals(ModelCatalog.all.first().id, option.id)
    }

    @Test
    fun chatMessage_generatesStableDefaults() {
        val message = ChatMessage(content = "Hello", role = MessageRole.USER)
        assertTrue(message.id.isNotBlank())
        assertTrue(message.timestamp > 0L)
        assertEquals(false, message.isStreaming)
    }
}
