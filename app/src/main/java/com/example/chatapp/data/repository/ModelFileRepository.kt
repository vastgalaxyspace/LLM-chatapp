package com.example.chatapp.data.repository

import android.content.Context
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelValidationResult
import com.example.chatapp.data.model.ModelValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

@Singleton
class ModelFileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val changeCounter = MutableStateFlow(0L)

    fun observeDownloadedModelIds(): Flow<Set<String>> =
        changeCounter
            .map { scanDownloadedModelIds() }
            .onStart { emit(scanDownloadedModelIds()) }
            .flowOn(Dispatchers.IO)

    suspend fun downloadedModelIds(): Set<String> = withContext(Dispatchers.IO) {
        scanDownloadedModelIds()
    }

    fun notifyChange() {
        changeCounter.value += 1
    }

    private fun scanDownloadedModelIds(): Set<String> {
        val modelsDir = File(context.filesDir, "models")
        return ModelCatalog.all
            .filter { ModelValidator.validate(File(modelsDir, it.fileName), it) is ModelValidationResult.Valid }
            .map { it.id }
            .toSet()
    }
}
