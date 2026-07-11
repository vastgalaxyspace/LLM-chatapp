package com.example.chatapp.data.model

data class ModelOption(
    val id: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeLabel: String,
    val sizeMb: Float,
    val sizeBytes: Long,
    val contextLabel: String,
    val quantizationLabel: String,
    val family: String = "Other",
    val description: String = "",
    val useCases: Set<String> = setOf("Text"),
    val requiresHuggingFaceAccess: Boolean = false,
    val licenseUrl: String? = null,
    val sha256: String? = null,
    val revision: String? = null,
    val downloadable: Boolean = true,
    val unavailableReason: String? = null
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

    // All artifacts below are GGUF files served by public, ungated Hugging Face
    // repositories, pinned to an immutable revision with exact size and SHA-256
    // taken from the repository's LFS metadata.
    private const val QWEN3_REVISION = "23749fefcc72300e3a2ad315e1317431b06b590a"
    private const val QWEN3_SHA256 = "9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031"
    private const val QWEN3_BYTES = 639_446_688L

    private fun unavailable(id: String, name: String, family: String, useCases: Set<String>, reason: String) =
        ModelOption(id, name, "$id.gguf", "", "Unavailable", 0f, 0L, "—", "—", family, useCases = useCases, downloadable = false, unavailableReason = reason)

    val all: List<ModelOption> = listOf(
        ModelOption(
            id = QWEN_SMALL,
            displayName = "Qwen 3 0.6B",
            fileName = "Qwen3-0.6B-Q8_0.gguf",
            downloadUrl = "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/$QWEN3_REVISION/Qwen3-0.6B-Q8_0.gguf",
            sizeLabel = "610 MB",
            sizeMb = QWEN3_BYTES / 1024f / 1024f,
            sizeBytes = QWEN3_BYTES,
            contextLabel = "4096 tokens",
            quantizationLabel = "Q8_0",
            family = "Qwen",
            description = "Verified public text model for reliable on-device chat.",
            useCases = setOf("Text", "Code"),
            sha256 = QWEN3_SHA256,
            revision = QWEN3_REVISION
        ),
        ModelOption(
            GEMMA, "Gemma 3 1B IT", "gemma-3-1b-it-Q4_K_M.gguf",
            "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF/resolve/f9c28bcd85737ffc5aef028638d3341d49869c27/gemma-3-1b-it-Q4_K_M.gguf",
            "769 MB", 806_058_240L / 1024f / 1024f, 806_058_240L, "4096 tokens", "Q4_K_M", "Gemma 3",
            "Verified compact Gemma text and code model.", setOf("Text", "Code"), false,
            "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF", "8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135", "f9c28bcd85737ffc5aef028638d3341d49869c27"
        ),
        ModelOption(
            QWEN, "Qwen 2.5 1.5B Instruct", "qwen2.5-1.5b-instruct-q8_0.gguf",
            "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/91cad51170dc346986eccefdc2dd33a9da36ead9/qwen2.5-1.5b-instruct-q8_0.gguf",
            "1.76 GB", 1_894_532_128L / 1024f / 1024f, 1_894_532_128L, "4096 tokens", "Q8_0", "Qwen",
            "Verified multilingual instruction model for stronger devices.", setOf("Text", "Code"), false, null,
            "d7efb072e7724d25048a4fda0a3e10b04bdef5d06b1403a1c93bd9f1240a63c8", "91cad51170dc346986eccefdc2dd33a9da36ead9"
        ),
        unavailable(GEMMA_4_E2B, "Gemma 4 E2B IT", "Gemma 4", setOf("Text", "Image", "Code"), "Multimodal backend support is not enabled in this text-first build."),
        unavailable(GEMMA_4_E4B, "Gemma 4 E4B IT", "Gemma 4", setOf("Text", "Image", "Code"), "Multimodal backend support is not enabled in this text-first build."),
        ModelOption(
            GEMMA_3_270M, "Gemma 3 270M IT", "gemma-3-270m-it-Q8_0.gguf",
            "https://huggingface.co/ggml-org/gemma-3-270m-it-GGUF/resolve/e7647be17ae1108f2f605ed061ca0608b171afff/gemma-3-270m-it-Q8_0.gguf",
            "278 MB", 291_545_600L / 1024f / 1024f, 291_545_600L, "4096 tokens", "Q8_0", "Gemma 3",
            "Verified lightweight Gemma model for low-memory devices.", setOf("Text"), false,
            "https://huggingface.co/ggml-org/gemma-3-270m-it-GGUF", "0ef57d2c838458a1952664260dcba38e5bdda37494f3af732f06e4add24068e3", "e7647be17ae1108f2f605ed061ca0608b171afff"
        ),
        unavailable(GEMMA_3N_E2B, "Gemma 3n E2B IT", "Gemma 3n", setOf("Text", "Image", "Audio"), "Multimodal backend support is not enabled in this text-first build."),
        unavailable(GEMMA_3N_E4B, "Gemma 3n E4B IT", "Gemma 3n", setOf("Text", "Image", "Audio"), "Multimodal backend support is not enabled in this text-first build."),
        unavailable(FUNCTION_GEMMA, "FunctionGemma 270M", "Utility", setOf("Tools"), "Tool calling is not enabled in this text-first build."),
        ModelOption(
            DEEPSEEK_R1_QWEN_1_5B, "DeepSeek R1 Qwen 1.5B", "DeepSeek-R1-Distill-Qwen-1.5B-Q8_0.gguf",
            "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/3cb4d15544a2a5e07439592b9a0965b6445fbd34/DeepSeek-R1-Distill-Qwen-1.5B-Q8_0.gguf",
            "1.76 GB", 1_894_532_416L / 1024f / 1024f, 1_894_532_416L, "4096 tokens", "Q8_0", "DeepSeek",
            "Verified reasoning-focused text and code model.", setOf("Text", "Code"), false, null,
            "068a721e47419ccfc94b6420118f772478544e1a0d4fad7118212774b3f9ba9e", "3cb4d15544a2a5e07439592b9a0965b6445fbd34"
        ),
        ModelOption(
            PHI_4_MINI, "Phi 4 Mini Instruct", "Phi-4-mini-instruct-Q4_K_M.gguf",
            "https://huggingface.co/unsloth/Phi-4-mini-instruct-GGUF/resolve/78eb92a46fc37e6b524df991ed9aca9bc6aa7b80/Phi-4-mini-instruct-Q4_K_M.gguf",
            "2.32 GB", 2_491_874_272L / 1024f / 1024f, 2_491_874_272L, "4096 tokens", "Q4_K_M", "Phi",
            "Verified high-quality instruction model for high-memory devices.", setOf("Text", "Code"), false, null,
            "88c00229914083cd112853aab84ed51b87bdf6b9ce42f532d8c85c7c63b1730a", "78eb92a46fc37e6b524df991ed9aca9bc6aa7b80"
        ),
        unavailable(FAST_VLM_0_5B, "FastVLM 0.5B", "FastVLM", setOf("Image"), "Vision backend support is not enabled in this text-first build.")
    )

    val available: List<ModelOption> = all.filter { it.downloadable }

    val families: List<String> = all.map { it.family }.distinct()
    val useCaseFilters: List<String> = listOf("Text", "Image", "Audio", "Code", "Tools")

    fun findById(id: String): ModelOption? = all.firstOrNull { it.id == id }

    fun requireById(id: String): ModelOption = findById(id) ?:
        throw IllegalArgumentException("Unknown model id: $id")

    /** UI compatibility helper; download and engine code must use requireById. */
    fun fromId(id: String): ModelOption = findById(id) ?: available.first()
}
