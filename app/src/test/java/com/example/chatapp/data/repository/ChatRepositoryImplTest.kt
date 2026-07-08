package com.example.chatapp.data.repository

import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import com.google.ai.edge.litertlm.Content
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChatRepositoryImplTest {
    private val engineManager: ChatEngineManager = mockk()
    private val appPreferences: AppPreferences = mockk()

    @Test
    fun sendMessageSendsPlainTextWithoutChatTemplateTokens() = runTest {
        every { appPreferences.selectedModel } returns flowOf(ModelCatalog.QWEN_SMALL)
        every {
            engineManager.send(
                match { message ->
                    (message.contents.contents.singleOrNull() as? Content.Text)?.text == "hello"
                },
                "hello"
            )
        } returns flowOf("ok")

        ChatRepositoryImpl(engineManager, appPreferences)
            .sendMessage("hello")
            .toList()

        verify {
            engineManager.send(any(), "hello")
        }
    }

    @Test
    fun deepSeekKeepsNoThinkAsPlainText() = runTest {
        every { appPreferences.selectedModel } returns flowOf(ModelCatalog.DEEPSEEK_R1_QWEN_1_5B)
        every {
            engineManager.send(
                match { message ->
                    (message.contents.contents.singleOrNull() as? Content.Text)?.text == "solve it\n/no_think"
                },
                "solve it\n/no_think"
            )
        } returns flowOf("ok")

        ChatRepositoryImpl(engineManager, appPreferences)
            .sendMessage("solve it")
            .toList()

        verify {
            engineManager.send(any(), "solve it\n/no_think")
        }
    }
}
