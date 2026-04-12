package com.example.chatapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chatapp.data.model.ModelCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chatapp_preferences")

class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SelectedBackend = stringPreferencesKey("selected_backend")
        val SelectedModel = stringPreferencesKey("selected_model")
        val Temperature = floatPreferencesKey("temperature")
        val MaxTokens = intPreferencesKey("max_tokens")
        val HuggingFaceToken = stringPreferencesKey("hugging_face_token")
    }

    val selectedBackend: Flow<String> = context.dataStore.data.map { it[Keys.SelectedBackend] ?: "GPU" }
    val selectedModel: Flow<String> = context.dataStore.data.map { it[Keys.SelectedModel] ?: ModelCatalog.GEMMA }
    val temperature: Flow<Float> = context.dataStore.data.map { it[Keys.Temperature] ?: 0.8f }
    val maxTokens: Flow<Int> = context.dataStore.data.map { it[Keys.MaxTokens] ?: 512 }
    val isModelDownloaded: Flow<Boolean> = context.dataStore.data.map {
        val selectedModelId = it[Keys.SelectedModel] ?: ModelCatalog.GEMMA
        val selectedModelFile = File(context.filesDir, "models/${ModelCatalog.fromId(selectedModelId).fileName}")
        selectedModelFile.exists() && selectedModelFile.length() > 0L
    }

    suspend fun updateSelectedBackend(value: String) {
        context.dataStore.edit { it[Keys.SelectedBackend] = value }
    }

    suspend fun updateSelectedModel(value: String) {
        context.dataStore.edit { it[Keys.SelectedModel] = value }
    }

    suspend fun updateTemperature(value: Float) {
        context.dataStore.edit { it[Keys.Temperature] = value }
    }

    suspend fun updateMaxTokens(value: Int) {
        context.dataStore.edit { it[Keys.MaxTokens] = value }
    }

    val huggingFaceToken: Flow<String> = context.dataStore.data.map { it[Keys.HuggingFaceToken] ?: "" }

    suspend fun updateHuggingFaceToken(value: String) {
        context.dataStore.edit { it[Keys.HuggingFaceToken] = value.trim() }
    }
}
