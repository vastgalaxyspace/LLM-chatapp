package com.example.chatapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnDarkPrimary,
    secondary = PrimaryGreenLight,
    onSecondary = OnDarkPrimary,
    tertiary = AccentTeal,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceContainer = DarkSurfaceCard,
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainerLowest = Color(0xFF101010),
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = Color(0xFF303030),
    onSurface = OnDarkPrimary,
    onSurfaceVariant = OnDarkSecondary,
    error = ErrorRed,
    onError = OnDarkPrimary
)

private val LightColors = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnDarkPrimary,
    secondary = PrimaryGreenLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurface.copy(alpha = 0.7f),
    error = ErrorRed
)

@Composable
fun ChatAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ChatAppTypography,
        content = content
    )
}
