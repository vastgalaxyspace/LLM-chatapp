package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatapp.ui.navigation.AppNavigation
import com.example.chatapp.ui.navigation.AppNavigationViewModel
import com.example.chatapp.ui.theme.ChatAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val appNavigationViewModel: AppNavigationViewModel = hiltViewModel()
            val isDarkTheme by appNavigationViewModel.isDarkTheme.collectAsStateWithLifecycle()

            ChatAppTheme(darkTheme = isDarkTheme) {
                AppNavigation()
            }
        }
    }
}
