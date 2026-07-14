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
    @ApplicationContext private val context: Context,
    private val secureTokenStore: SecureTokenStore = SecureTokenStore(context)
) {
    private object Keys {
        val SelectedBackend = stringPreferencesKey("selected_backend")
        val SelectedModel = stringPreferencesKey("selected_model")
        val Temperature = floatPreferencesKey("temperature")
        val MaxTokens = intPreferencesKey("max_tokens")
        val HuggingFaceToken = stringPreferencesKey("hugging_face_token")
        val HasCompletedOnboarding = androidx.datastore.preferences.core.booleanPreferencesKey("has_completed_onboarding")
        val IsDarkTheme = androidx.datastore.preferences.core.booleanPreferencesKey("is_dark_theme")
        val UserName = stringPreferencesKey("user_name")
    }

    val userName: Flow<String> = context.dataStore.data.map { it[Keys.UserName]?.takeIf { name -> name.isNotBlank() } ?: "" }

    suspend fun updateUserName(value: String) {
        val trimmed = value.trim()
        context.dataStore.edit { it[Keys.UserName] = trimmed }
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.IsDarkTheme] ?: true }

    suspend fun updateDarkTheme(value: Boolean) {
        context.dataStore.edit { it[Keys.IsDarkTheme] = value }
    }

    val selectedBackend: Flow<String> = context.dataStore.data.map { it[Keys.SelectedBackend] ?: "CPU" }
    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        val stored = preferences[Keys.SelectedModel]
        stored?.takeIf { ModelCatalog.findById(it)?.downloadable == true } ?: ModelCatalog.QWEN_SMALL
    }
    val temperature: Flow<Float> = context.dataStore.data.map { it[Keys.Temperature] ?: 0.8f }
    val maxTokens: Flow<Int> = context.dataStore.data.map { (it[Keys.MaxTokens] ?: 1536).coerceIn(512, 2048) }
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { it[Keys.HasCompletedOnboarding] ?: false }
    val isModelDownloaded: Flow<Boolean> = context.dataStore.data.map {
        val selectedModelId = it[Keys.SelectedModel]?.takeIf { id -> ModelCatalog.findById(id)?.downloadable == true } ?: ModelCatalog.QWEN_SMALL
        val selectedModelFile = File(context.filesDir, "models/${ModelCatalog.fromId(selectedModelId).fileName}")
        selectedModelFile.exists() && selectedModelFile.length() > 0L
    }

    suspend fun updateSelectedBackend(value: String) {
        context.dataStore.edit { it[Keys.SelectedBackend] = value }
    }

    suspend fun updateSelectedModel(value: String) {
        require(ModelCatalog.requireById(value).downloadable) { "Model is unavailable: $value" }
        context.dataStore.edit { it[Keys.SelectedModel] = value }
    }

    suspend fun updateTemperature(value: Float) {
        context.dataStore.edit { it[Keys.Temperature] = value }
    }

    suspend fun updateMaxTokens(value: Int) {
        context.dataStore.edit { it[Keys.MaxTokens] = value }
    }

    val huggingFaceToken: Flow<String> = context.dataStore.data.map { preferences ->
        val legacyToken = preferences[Keys.HuggingFaceToken]?.takeIf { token -> token.isNotBlank() }
        legacyToken ?: secureTokenStore.readHuggingFaceToken()?.takeIf { token -> token.isNotBlank() } ?: ""
    }

    suspend fun updateHuggingFaceToken(value: String) {
        secureTokenStore.writeHuggingFaceToken(value)
        context.dataStore.edit { it.remove(Keys.HuggingFaceToken) }
    }

    suspend fun migratePlaintextHuggingFaceTokenIfNeeded() {
        context.dataStore.edit { preferences ->
            val legacyToken = preferences[Keys.HuggingFaceToken]?.takeIf { it.isNotBlank() }
            if (legacyToken != null && secureTokenStore.readHuggingFaceToken().isNullOrBlank()) {
                secureTokenStore.writeHuggingFaceToken(legacyToken)
            }
            preferences.remove(Keys.HuggingFaceToken)
        }
    }

    suspend fun markOnboardingComplete() {
        context.dataStore.edit { it[Keys.HasCompletedOnboarding] = true }
    }

    suspend fun completeOnboardingWithName(name: String) {
        val trimmed = name.trim()
        context.dataStore.edit {
            if (trimmed.isNotEmpty()) it[Keys.UserName] = trimmed
            it[Keys.HasCompletedOnboarding] = true
        }
    }
}
