package com.example.chatapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkColors = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnDarkPrimary,
    secondary = PrimaryGreenLight,
    onSecondary = OnDarkPrimary,
    tertiary = AccentTeal,
    background = DarkBackground,
    surface = DarkSurfaceCard,
    surfaceVariant = DarkSurfaceVariant,
    surfaceContainer = DarkSurfaceCard,
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainerLowest = Color(0xFF101010),
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = Color(0xFF303030),
    onSurface = OnDarkPrimary,
    onSurfaceVariant = OnDarkTertiary,
    outline = Color(0x1FFFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    error = ErrorRed,
    onError = OnDarkPrimary
)

private val LightColors = lightColorScheme(
    primary = PrimaryGreenDark,
    onPrimary = Color.White,
    secondary = PrimaryGreen,
    onSecondary = Color.White,
    tertiary = AccentTeal,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceContainer = LightSurface,
    surfaceContainerLow = Color(0xFFFCFCFD),
    surfaceContainerLowest = Color.White,
    surfaceContainerHigh = LightSurfaceElevated,
    surfaceContainerHighest = Color(0xFFD8DDE3),
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFEDF0F4),
    error = ErrorRed,
    onError = Color.White
)

/** True when the active color scheme is the dark one. */
val ColorScheme.isDark: Boolean
    get() = background.luminance() < 0.5f

/** Secondary text color that adapts to the active theme. */
val ColorScheme.textSecondary: Color
    get() = if (isDark) OnDarkSecondary else LightOnSurfaceSecondary

/** Hint / placeholder text color that adapts to the active theme. */
val ColorScheme.textHint: Color
    get() = if (isDark) OnDarkHint else LightOnSurfaceHint

/** Subtle hairline border color that adapts to the active theme. */
val ColorScheme.subtleBorder: Color
    get() = if (isDark) Color.White.copy(alpha = 0.08f) else LightOutline

/** Assistant chat bubble background that adapts to the active theme. */
val ColorScheme.bubbleAssistant: Color
    get() = if (isDark) Color(0xFF1E1E21) else Color.White

/** Pill / chip background that adapts to the active theme. */
val ColorScheme.chipBackground: Color
    get() = if (isDark) Color(0xFF1A1A1A) else LightSurfaceVariant

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
