package com.example.chatapp

import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.model.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
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
    fun defaultModelHasPinnedVerifiedArtifactMetadata() {
        val option = ModelCatalog.requireById(ModelCatalog.QWEN_SMALL)
        assertTrue(option.downloadable)
        assertEquals(639_446_688L, option.sizeBytes)
        assertEquals("9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031", option.sha256)
        assertTrue(option.downloadUrl.contains("23749fefcc72300e3a2ad315e1317431b06b590a"))
        assertTrue(option.fileName.endsWith(".gguf"))
        assertNull(ModelCatalog.findById("missing-model-id"))
    }

    @Test
    fun chatMessage_generatesStableDefaults() {
        val message = ChatMessage(content = "Hello", role = MessageRole.USER)
        assertTrue(message.id.isNotBlank())
        assertTrue(message.timestamp > 0L)
        assertEquals(false, message.isStreaming)
    }
}
