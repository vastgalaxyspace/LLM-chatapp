package com.example.chatapp.data.model

data class ModelOption(
    val id: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeLabel: String,
    val sizeMb: Float,
    val contextLabel: String,
    val quantizationLabel: String,
    val family: String = "Gemma",
    val description: String = "",
    val useCases: Set<String> = setOf("Text"),
    val requiresHuggingFaceAccess: Boolean = false,
    val licenseUrl: String? = null,
    val sha256: String? = null
)

object ModelCatalog {
    const val GEMMA = "gemma_3_1b"
    const val QWEN = "qwen_2_5_1_5b"
    const val QWEN_SMALL = "qwen_3_0_6b"
    const val FUNCTION_GEMMA = "function_gemma_270m"
    const val GEMMA_4_E2B = "gemma_4_e2b"
    const val GEMMA_4_E4B = "gemma_4_e4b"
    const val GEMMA_3_270M = "gemma_3_270m_it"
    const val GEMMA_3N_E2B = "gemma_3n_e2b_it"
    const val GEMMA_3N_E4B = "gemma_3n_e4b_it"
    const val DEEPSEEK_R1_QWEN_1_5B = "deepseek_r1_qwen_1_5b"
    const val PHI_4_MINI = "phi_4_mini_instruct"
    const val FAST_VLM_0_5B = "fast_vlm_0_5b"

    val all = listOf(
        // ── Gemma 4 family ──────────────────────────────────────────────────
        ModelOption(
            id = GEMMA_4_E2B,
            displayName = "Gemma 4 E2B IT",
            fileName = "gemma-4-E2B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            sizeLabel = "2.6 GB",
            sizeMb = 2580f,
            contextLabel = "32768 tokens",
            quantizationLabel = "4-bit quantization",
            family = "Gemma 4",
            description = "Latest Gemma 4 model, optimised for on-device.",
            useCases = setOf("Text", "Image", "Code")
        ),
        ModelOption(
            id = GEMMA_4_E4B,
            displayName = "Gemma 4 E4B IT",
            fileName = "gemma-4-E4B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            sizeLabel = "3.7 GB",
            sizeMb = 3650f,
            contextLabel = "32768 tokens",
            quantizationLabel = "4-bit quantization",
            family = "Gemma 4",
            description = "Larger Gemma 4 variant with superior quality.",
            useCases = setOf("Text", "Image", "Code")
        ),
        ModelOption(
            id = GEMMA_3N_E2B,
            displayName = "Gemma 3n E2B IT",
            fileName = "gemma-3n-E2B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm",
            sizeLabel = "3.66 GB",
            sizeMb = 3660f,
            contextLabel = "Multimodal",
            quantizationLabel = "int4 quantization",
            family = "Gemma 3n",
            description = "Multimodal Gemma 3n model for advanced text, image, and audio workflows. Requires Gemma license access on Hugging Face.",
            useCases = setOf("Text", "Image", "Audio"),
            requiresHuggingFaceAccess = true,
            licenseUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm"
        ),
        ModelOption(
            id = GEMMA_3N_E4B,
            displayName = "Gemma 3n E4B IT",
            fileName = "gemma-3n-E4B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/resolve/main/gemma-3n-E4B-it-int4.litertlm",
            sizeLabel = "4.92 GB",
            sizeMb = 4920f,
            contextLabel = "Multimodal",
            quantizationLabel = "int4 quantization",
            family = "Gemma 3n",
            description = "Larger multimodal Gemma 3n model with higher quality. Requires Gemma license access on Hugging Face.",
            useCases = setOf("Text", "Image", "Audio"),
            requiresHuggingFaceAccess = true,
            licenseUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm"
        ),

        // ── Gemma 3 family ──────────────────────────────────────────────────
        ModelOption(
            id = GEMMA,
            displayName = "Gemma 3 1B IT",
            fileName = "gemma3-1b.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv4096.litertlm",
            sizeLabel = "584 MB",
            sizeMb = 584f,
            contextLabel = "4096 tokens",
            quantizationLabel = "4-bit quantization",
            family = "Gemma 3",
            description = "Balanced Gemma 3 model with great performance.",
            useCases = setOf("Text", "Code"),
            requiresHuggingFaceAccess = true,
            licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT"
        ),
        ModelOption(
            id = GEMMA_3_270M,
            displayName = "Gemma 3 270M IT",
            fileName = "gemma3-270m-it-q8.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.litertlm",
            sizeLabel = "304 MB",
            sizeMb = 304f,
            contextLabel = "32768 tokens",
            quantizationLabel = "8-bit quantization",
            family = "Gemma 3",
            description = "Tiny Gemma 3 — fastest model with low memory use.",
            useCases = setOf("Text"),
            requiresHuggingFaceAccess = true,
            licenseUrl = "https://huggingface.co/litert-community/gemma-3-270m-it"
        ),

        // ── Qwen family ─────────────────────────────────────────────────────
        ModelOption(
            id = QWEN,
            displayName = "Qwen 2.5 1.5B Instruct",
            fileName = "qwen2.5-1.5b-instruct.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeLabel = "1.6 GB",
            sizeMb = 1600f,
            contextLabel = "4096 tokens",
            quantizationLabel = "8-bit quantization",
            family = "Qwen",
            description = "Strong multilingual Qwen 2.5 chat model.",
            useCases = setOf("Text", "Code")
        ),
        ModelOption(
            id = QWEN_SMALL,
            displayName = "Qwen 3 0.6B",
            fileName = "Qwen3-0.6B.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm",
            sizeLabel = "614 MB",
            sizeMb = 614f,
            contextLabel = "4096 tokens",
            quantizationLabel = "dynamic int8 quantization",
            family = "Qwen",
            description = "Compact Qwen 3 for resource-limited devices.",
            useCases = setOf("Text")
        ),
        ModelOption(
            id = DEEPSEEK_R1_QWEN_1_5B,
            displayName = "DeepSeek R1 Qwen 1.5B",
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeLabel = "1.83 GB",
            sizeMb = 1830f,
            contextLabel = "4096 tokens",
            quantizationLabel = "8-bit quantization",
            family = "DeepSeek",
            description = "Reasoning-focused model for math, planning, and step-by-step answers.",
            useCases = setOf("Text", "Code")
        ),
        ModelOption(
            id = PHI_4_MINI,
            displayName = "Phi 4 Mini Instruct",
            fileName = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeLabel = "3.91 GB",
            sizeMb = 3910f,
            contextLabel = "4096 tokens",
            quantizationLabel = "8-bit quantization",
            family = "Phi",
            description = "High-quality small instruct model for chat and code on stronger phones.",
            useCases = setOf("Text", "Code")
        ),
        ModelOption(
            id = FAST_VLM_0_5B,
            displayName = "FastVLM 0.5B",
            fileName = "FastVLM-0.5B.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/FastVLM-0.5B/resolve/main/FastVLM-0.5B.litertlm",
            sizeLabel = "1.16 GB",
            sizeMb = 1160f,
            contextLabel = "1280 tokens",
            quantizationLabel = "dynamic int8 quantization",
            family = "FastVLM",
            description = "Vision-language model for image understanding on device.",
            useCases = setOf("Image")
        ),

        // ── Utility models ──────────────────────────────────────────────────
        ModelOption(
            id = FUNCTION_GEMMA,
            displayName = "FunctionGemma 270M",
            fileName = "mobile-actions_q8_ekv1024.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/functiongemma-mobile-actions_q8_ekv1024.litertlm/resolve/main/mobile-actions_q8_ekv1024.litertlm",
            sizeLabel = "284 MB",
            sizeMb = 284f,
            contextLabel = "1024 tokens",
            quantizationLabel = "8-bit quantization",
            family = "Utility",
            description = "Function calling model for mobile actions.",
            useCases = setOf("Tools")
        )
    )

    /** All distinct model families for filtering chips. */
    val families: List<String> = all.map { it.family }.distinct()
    val useCaseFilters: List<String> = listOf("Text", "Image", "Audio", "Code", "Tools")

    fun fromId(id: String): ModelOption = all.firstOrNull { it.id == id } ?: all.first()
}
