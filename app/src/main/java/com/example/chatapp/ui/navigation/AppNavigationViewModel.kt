package com.example.chatapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {
    val isModelDownloaded: StateFlow<Boolean> = appPreferences.isModelDownloaded.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val hasCompletedOnboarding: StateFlow<Boolean> = appPreferences.hasCompletedOnboarding.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun completeOnboarding() {
        viewModelScope.launch {
            appPreferences.markOnboardingComplete()
        }
    }
}
