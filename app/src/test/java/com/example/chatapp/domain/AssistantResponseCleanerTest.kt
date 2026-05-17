package com.example.chatapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantResponseCleanerTest {
    @Test
    fun removesCompleteThinkBlock() {
        val raw = """
            <think>
            I should reason internally.
            </think>

            RAG means retrieval-augmented generation.
        """.trimIndent()

        assertEquals(
            "RAG means retrieval-augmented generation.",
            AssistantResponseCleaner.clean(raw)
        )
    }

    @Test
    fun hidesIncompleteThinkBlockWhileStreaming() {
        val raw = "<think>Still reasoning about the answer"

        assertEquals("", AssistantResponseCleaner.clean(raw))
    }

    @Test
    fun handlesOrphanClosingThinkTag() {
        val raw = "internal notes</think>\nFinal answer"

        assertEquals("Final answer", AssistantResponseCleaner.clean(raw))
    }

    @Test
    fun leavesNormalResponseUnchangedExceptOuterWhitespace() {
        val raw = "  RAG connects an LLM to external knowledge.  "

        assertEquals(
            "RAG connects an LLM to external knowledge.",
            AssistantResponseCleaner.clean(raw)
        )
    }

    @Test
    fun preservesMarkdownMarkersForRendering() {
        val raw = """
            The word **rag** can mean:
            - `cloth`
            - *teasing*
        """.trimIndent()

        assertEquals(
            "The word **rag** can mean:\n- `cloth`\n- *teasing*",
            AssistantResponseCleaner.clean(raw)
        )
    }

    @Test
    fun preservesFencedCodeBlocksForRendering() {
        val raw = """
            ```kotlin
            val answer = 42
            ```
        """.trimIndent()

        assertEquals(raw, AssistantResponseCleaner.clean(raw))
    }

    @Test
    fun treatsPunctuationOnlyAsNoVisibleAnswer() {
        assertEquals(false, AssistantResponseCleaner.hasVisibleAnswer(">"))
        assertEquals(false, AssistantResponseCleaner.hasVisibleAnswer("!"))
        assertEquals(true, AssistantResponseCleaner.hasVisibleAnswer("AI means artificial intelligence."))
    }
}
