package com.example.chatapp.data.repository

import com.google.ai.edge.litertlm.Message
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ContextWindowManager @Inject constructor() {
    private val retainedTurns = ArrayDeque<Pair<String, String>>()

    private val _estimatedTokensUsed = MutableStateFlow(0)
    val estimatedTokensUsed: StateFlow<Int> = _estimatedTokensUsed.asStateFlow()

    private val _contextWindowPercent = MutableStateFlow(0f)
    val contextWindowPercent: StateFlow<Float> = _contextWindowPercent.asStateFlow()

    @Synchronized
    fun retainedTurns(): List<Pair<String, String>> = retainedTurns.toList()

    @Synchronized
    fun retainedMessages(): List<Message> = retainedTurns.flatMap { (userTurn, assistantTurn) ->
        listOf(Message.user(userTurn), Message.model(assistantTurn))
    }

    @Synchronized
    fun shouldRebuildConversation(maxNumTokens: Int): Boolean {
        if (maxNumTokens <= 0) return false
        // Trigger rebuild if we are nearing the engine's hard token limit
        return _estimatedTokensUsed.value > (maxNumTokens * REBUILD_THRESHOLD).toInt()
    }

    @Synchronized
    fun addTurn(userTurn: String, assistantTurn: String, maxNumTokens: Int) {
        retainedTurns.addLast(userTurn to assistantTurn)
        trimToBudget(maxNumTokens)
        publish(maxNumTokens)
    }

    @Synchronized
    fun reset(maxNumTokens: Int = 0) {
        retainedTurns.clear()
        publish(maxNumTokens)
    }

    @Synchronized
    fun refresh(maxNumTokens: Int) {
        trimToBudget(maxNumTokens)
        publish(maxNumTokens)
    }

    private fun trimToBudget(maxNumTokens: Int) {
        if (maxNumTokens <= 0) return

        // Ensure we always leave room for the system prompt and a new user message
        val usableBudget = (maxNumTokens * RETAINED_CONTEXT_BUDGET).toInt() - SYSTEM_PROMPT_RESERVE

        // Remove oldest turns until we are within budget
        // We always keep the most recent turn if possible
        while (retainedTurns.size > 1 && estimateTokens(retainedTurns) > usableBudget) {
            retainedTurns.removeFirst()
        }
    }

    private fun publish(maxNumTokens: Int) {
        val tokens = estimateTokens(retainedTurns)
        _estimatedTokensUsed.value = tokens

        _contextWindowPercent.value = if (maxNumTokens > 0) {
            (tokens.toFloat() / maxNumTokens.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    private fun estimateTokens(turns: Iterable<Pair<String, String>>): Int =
        turns.sumOf { (user, assistant) -> estimateTokens(user) + estimateTokens(assistant) }

    /**
     * More conservative estimation for LiteRT-LM models.
     * 2.5 chars per token is safer for models that use rich tokenizers (Gemma/Qwen).
     */
    private fun estimateTokens(text: String): Int {
        if (text.isBlank()) return 0
        return (text.length / CHARS_PER_TOKEN_SAFE).toInt().coerceAtLeast(1)
    }

    private companion object {
        // Updated for safety: Gemma/Qwen typically average 2.5 - 3 chars per token
        const val CHARS_PER_TOKEN_SAFE = 2.5f

        // Reserve tokens for the internal system prompt and metadata tags
        const val SYSTEM_PROMPT_RESERVE = 150

        // Percentage of context to keep after trimming
        const val RETAINED_CONTEXT_BUDGET = 0.80f

        // Threshold at which engine Manager should flush and rebuild native context
        const val REBUILD_THRESHOLD = 0.85f
    }
}
