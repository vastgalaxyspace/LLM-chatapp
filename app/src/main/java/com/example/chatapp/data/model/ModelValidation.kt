package com.example.chatapp.data.model

import java.io.File
import java.security.MessageDigest

sealed interface ModelValidationResult {
    data object Valid : ModelValidationResult
    data class Invalid(val reason: String) : ModelValidationResult
}

object ModelValidator {
    fun validate(file: File, model: ModelOption): ModelValidationResult {
        if (!model.downloadable || model.sizeBytes <= 0L || model.sha256.isNullOrBlank()) {
            return ModelValidationResult.Invalid(model.unavailableReason ?: "Model is not available for this build.")
        }
        if (!file.isFile) return ModelValidationResult.Invalid("Model file is missing.")
        if (file.length() != model.sizeBytes) {
            return ModelValidationResult.Invalid("Model size is ${file.length()} bytes; expected ${model.sizeBytes} bytes.")
        }
        val actual = sha256(file)
        return if (actual.equals(model.sha256, ignoreCase = true)) {
            ModelValidationResult.Valid
        } else {
            ModelValidationResult.Invalid("Model checksum does not match the trusted artifact.")
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
