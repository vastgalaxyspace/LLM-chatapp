package com.example.chatapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatapp.ui.components.ErrorStateView
import com.example.chatapp.ui.components.ModelLoadingOverlay
import com.example.chatapp.ui.screens.chat.ChatScreen
import com.example.chatapp.ui.screens.chat.ChatViewModel
import com.example.chatapp.ui.screens.download.DownloadScreen
import com.example.chatapp.ui.screens.download.DownloadViewModel
import com.example.chatapp.ui.screens.onboarding.OnboardingScreen
import com.example.chatapp.ui.screens.profile.ProfileScreen
import com.example.chatapp.ui.screens.settings.SettingsScreen

private object Routes {
    const val Onboarding = "onboarding"
    const val Download = "download"
    const val Loading = "loading"
    const val Chat = "chat"
    const val Settings = "settings"
    const val Profile = "profile"
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Onboarding
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate(Routes.Download) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Download) {
            val downloadViewModel: DownloadViewModel = hiltViewModel()
            DownloadScreen(
                viewModel = downloadViewModel,
                onUseModel = {
                    navController.navigate(Routes.Loading) {
                        launchSingleTop = true
                    }
                },
                onOpenChat = {
                    navController.navigateTopLevel(Routes.Chat)
                },
                onProfileClick = {
                    navController.navigateTopLevel(Routes.Profile)
                }
            )
        }

        composable(Routes.Loading) {
            val chatViewModel: ChatViewModel = hiltViewModel()
            val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()
            val errorMessage by chatViewModel.errorMessage.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                chatViewModel.initEngine()
            }

            LaunchedEffect(isLoading, errorMessage) {
                if (!isLoading && errorMessage == null) {
                    navController.navigate(Routes.Chat) {
                        popUpTo(Routes.Loading) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            if (errorMessage != null && !isLoading) {
                ErrorStateView(
                    message = errorMessage ?: "The model failed to initialize.",
                    onRetry = { chatViewModel.initEngine() },
                    onRedownload = {
                        navController.navigate(Routes.Download) {
                            popUpTo(Routes.Download) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                ModelLoadingOverlay()
            }
        }

        composable(
            route = "${Routes.Chat}?prompt={prompt}&conversationId={conversationId}",
            arguments = listOf(
                navArgument("prompt") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("conversationId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val prompt = backStackEntry.arguments?.getString("prompt")
            val conversationId = backStackEntry.arguments?.getLong("conversationId")?.takeIf { it > 0L }
            val chatViewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                viewModel = chatViewModel,
                initialPrompt = prompt,
                initialConversationId = conversationId,
                onOpenSettings = {
                    navController.navigate(Routes.Settings) {
                        launchSingleTop = true
                    }
                },
                onOpenModels = {
                    navController.navigateTopLevel(Routes.Download)
                },
                onProfileClick = {
                    navController.navigateTopLevel(Routes.Profile)
                }
            )
        }

        composable(Routes.Settings) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenModels = {
                    navController.navigateTopLevel(Routes.Download)
                },
                onOpenChat = {
                    navController.navigateTopLevel(Routes.Chat)
                }
            )
        }

        composable(Routes.Profile) {
            ProfileScreen(
                onChatClick = {
                    navController.navigateTopLevel(Routes.Chat)
                },
                onModelsClick = {
                    navController.navigateTopLevel(Routes.Download)
                }
            )
        }
    }
}
