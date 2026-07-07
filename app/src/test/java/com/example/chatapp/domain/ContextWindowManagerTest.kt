package com.example.chatapp.domain

import com.example.chatapp.data.repository.ContextWindowManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextWindowManagerTest {
    @Test
    fun addingTurnsWithinBudgetKeepsAllTurns() {
        val manager = ContextWindowManager()

        manager.addTurn("hello", "there", maxNumTokens = 100)
        manager.addTurn("how are you", "fine", maxNumTokens = 100)

        assertEquals(2, manager.retainedTurns().size)
    }

    @Test
    fun addingTurnThatExceedsBudgetDropsOldestTurn() {
        val manager = ContextWindowManager()

        manager.addTurn("12345678", "12345678", maxNumTokens = 20)
        manager.addTurn("a".repeat(60), "bbbb", maxNumTokens = 20)

        assertEquals(listOf("a".repeat(60) to "bbbb"), manager.retainedTurns())
    }

    @Test
    fun estimatedTokensUsedReflectsCurrentDequeSize() {
        val manager = ContextWindowManager()

        manager.addTurn("12345678", "1234", maxNumTokens = 100)

        assertEquals(3, manager.estimatedTokensUsed.value)
    }
}
