package com.example.chatapp.data.model

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelValidatorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun validatesExactSizeAndChecksum() {
        val file = temporaryFolder.newFile("model.gguf").apply { writeText("model") }
        val model = testModel(file, ModelValidator.sha256(file))
        assertTrue(ModelValidator.validate(file, model) is ModelValidationResult.Valid)
    }

    @Test
    fun rejectsTruncatedAndCorruptedFiles() {
        val file = temporaryFolder.newFile("model.gguf").apply { writeText("model") }
        assertTrue(ModelValidator.validate(file, testModel(file, "0".repeat(64))) is ModelValidationResult.Invalid)
        assertTrue(ModelValidator.validate(file, testModel(file, ModelValidator.sha256(file), file.length() + 1)) is ModelValidationResult.Invalid)
    }

    private fun testModel(file: File, sha: String, size: Long = file.length()) = ModelOption(
        id = "test", displayName = "Test", fileName = file.name, downloadUrl = "https://example.test/model",
        sizeLabel = "test", sizeMb = 0f, sizeBytes = size, contextLabel = "test",
        quantizationLabel = "test", sha256 = sha, revision = "revision"
    )
}
