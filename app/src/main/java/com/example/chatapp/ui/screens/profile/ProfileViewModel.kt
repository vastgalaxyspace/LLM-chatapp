package com.example.chatapp.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.local.ChatLocalStore
import com.example.chatapp.data.local.ProfileStats
import com.example.chatapp.data.repository.ModelFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiStats(
    val conversations: Int,
    val messages: Int,
    val userMessages: Int,
    val aiMessages: Int,
    val downloadedModels: Int
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    chatLocalStore: ChatLocalStore,
    modelFileRepository: ModelFileRepository
) : ViewModel() {
    private val downloadedModelCount = modelFileRepository.observeDownloadedModelIds().map { it.size }

    val stats: StateFlow<ProfileUiStats> = combine(
        chatLocalStore.observeProfileStats(),
        downloadedModelCount
    ) { dbStats: ProfileStats, modelCount: Int ->
        ProfileUiStats(
            conversations = dbStats.conversationCount,
            messages = dbStats.messageCount,
            userMessages = dbStats.userMessages,
            aiMessages = dbStats.aiMessages,
            downloadedModels = modelCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiStats(0, 0, 0, 0, 0)
    )
}
