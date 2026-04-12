package com.example.chatapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.chatapp.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    appPreferences: AppPreferences
) : ViewModel() {
    val isModelDownloaded: StateFlow<Boolean> = appPreferences.isModelDownloaded.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
}
