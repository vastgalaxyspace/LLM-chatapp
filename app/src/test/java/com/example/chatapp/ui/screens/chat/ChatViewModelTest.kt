package com.example.chatapp.ui.screens.chat

import android.util.Log
import app.cash.turbine.test
import com.example.chatapp.data.local.ChatLocalStore
import com.example.chatapp.data.local.LocalMediaStore
import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.data.repository.ContextWindowManager
import com.example.chatapp.domain.usecase.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var chatRepository: ChatRepository
    private lateinit var chatLocalStore: ChatLocalStore
    private lateinit var localMediaStore: LocalMediaStore
    private lateinit var appPreferences: AppPreferences
    private lateinit var contextWindowManager: ContextWindowManager
    private lateinit var messagesFlow: MutableStateFlow<List<ChatMessage>>

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        sendMessageUseCase = mockk(relaxed = true)
        chatRepository = mockk(relaxed = true)
        chatLocalStore = mockk(relaxed = true)
        localMediaStore = mockk(relaxed = true)
        appPreferences = mockk(relaxed = true)
        contextWindowManager = ContextWindowManager()
        messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

        every { appPreferences.selectedBackend } returns flowOf("GPU")
        every { appPreferences.temperature } returns flowOf(0.8f)
        every { appPreferences.maxTokens } returns flowOf(1536)
        every { appPreferences.selectedModel } returns flowOf(ModelCatalog.QWEN_SMALL)
        coEvery { chatLocalStore.finishAbandonedStreamingMessages() } returns Unit
        coEvery { chatLocalStore.latestConversationIdOrCreate() } returns 1L
        coEvery { chatLocalStore.ensureConversation(any()) } returns 1L
        every { chatLocalStore.observeConversationSummaries() } returns flowOf(emptyList())
        every { chatLocalStore.observeConversationMessages(any()) } returns messagesFlow
        every { chatRepository.isEngineReady() } returns true
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun sendMessageWithEmptyStringDoesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.isGenerating.test {
            assertEquals(false, awaitItem())
            viewModel.sendMessage("   ")
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun sendMessageWithWrongModelUseCaseSetsErrorMessage() = runTest(dispatcher) {
        every { appPreferences.selectedModel } returns flowOf(ModelCatalog.FAST_VLM_0_5B)
        val viewModel = viewModel()
        val modelCollector = launch { viewModel.selectedModelName.collect {} }
        viewModel.initEngine()
        advanceUntilIdle()

        viewModel.sendMessage("hello")
        advanceUntilIdle()
        modelCollector.cancel()

        assertEquals(
            "Multimodal model selected. Please attach media or switch models.",
            viewModel.errorMessage.value
        )
    }

    @Test
    fun stopGenerationCancelsJobAndSetsIsGeneratingFalse() = runTest(dispatcher) {
        every { sendMessageUseCase(any(), any()) } returns flow { awaitCancellation() }
        val viewModel = viewModel()
        viewModel.initEngine()
        advanceUntilIdle()

        viewModel.sendMessage("hello")
        advanceUntilIdle()
        viewModel.stopGeneration()
        advanceUntilIdle()

        assertEquals(false, viewModel.isGenerating.value)
        assertEquals(null, viewModel.errorMessage.value)
        verify { chatRepository.cancelGeneration() }
        coVerify { chatLocalStore.deleteMessage(any()) }
    }

    @Test
    fun retryMessageWithNoPriorUserMessageDoesNothing() = runTest(dispatcher) {
        val ai = ChatMessage(id = "ai", content = "answer", role = MessageRole.AI)
        messagesFlow.value = listOf(ai)
        val viewModel = viewModel()
        viewModel.initEngine()
        advanceUntilIdle()

        viewModel.retryMessage(ai.id)
        advanceUntilIdle()

        verify(exactly = 0) { sendMessageUseCase(any(), any()) }
    }

    @Test
    fun retryMessageWithValidPriorUserMessageRetriggersGeneration() = runTest(dispatcher) {
        val user = ChatMessage(id = "user", content = "hello", role = MessageRole.USER)
        val ai = ChatMessage(id = "ai", content = "answer", role = MessageRole.AI)
        messagesFlow.value = listOf(user, ai)
        every { sendMessageUseCase(any(), any()) } returns flowOf("ok")
        val viewModel = viewModel()
        viewModel.initEngine()
        advanceUntilIdle()

        viewModel.retryMessage(ai.id)
        advanceUntilIdle()

        verify { sendMessageUseCase("hello", emptyList()) }
    }

    private fun TestScope.viewModel(): ChatViewModel =
        ChatViewModel(
            sendMessageUseCase = sendMessageUseCase,
            chatRepository = chatRepository,
            chatLocalStore = chatLocalStore,
            localMediaStore = localMediaStore,
            contextWindowManager = contextWindowManager,
            appPreferences = appPreferences,
            defaultDispatcher = dispatcher
        )
}
