package com.example.chatapp.domain

object AssistantResponseCleaner {
    private val completeThinkBlock = Regex(
        pattern = """<think\b[^>]*>.*?</think>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val thinkStart = Regex("""<think\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val thinkEnd = Regex("""</think>""", RegexOption.IGNORE_CASE)
    private val trailingLineSpaces = Regex("""[ \t]+\n""")
    private val excessiveBlankLines = Regex("""\n{3,}""")
    private val meaningfulText = Regex("""[\p{L}\p{N}]""")

    // Common special tokens leaked by on-device LLMs
    private val specialTokens = Regex(
        """<\|?(pad|eos|bos|unk|mask|sep|cls|endoftext|im_end|im_start|end|eot_id|start_header_id|end_header_id|begin_of_text|end_of_text|assistant|user|system)\|?>""",
        RegexOption.IGNORE_CASE
    )
    private val xmlStyleSpecialTokens = Regex(
        """</?(?:s|pad|eos|bos)>""",
        RegexOption.IGNORE_CASE
    )

    fun clean(raw: String): String {
        if (raw.isBlank()) return raw

        var cleaned = completeThinkBlock.replace(raw, "")

        thinkEnd.find(cleaned)?.let { end ->
            cleaned = cleaned.substring(end.range.last + 1)
        }

        thinkStart.find(cleaned)?.let { start ->
            cleaned = cleaned.substring(0, start.range.first)
        }

        return cleaned
            .replace(specialTokens, "")
            .replace(xmlStyleSpecialTokens, "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(trailingLineSpaces, "\n")
            .replace(excessiveBlankLines, "\n\n")
            .trim()
    }

    fun hasVisibleAnswer(text: String): Boolean {
        return meaningfulText.containsMatchIn(clean(text))
    }
}
