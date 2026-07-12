package com.example.chatapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InnoAiColors = darkColorScheme(
    primary = SignalAmber, onPrimary = Color(0xFF171006),
    secondary = SignalAmberBright, onSecondary = Color.Black,
    tertiary = SignalAmberDark, background = TrueBlack,
    surface = InstrumentSurface, surfaceVariant = InstrumentSurfaceHigh,
    surfaceContainer = InstrumentSurface, surfaceContainerLow = InkBlack,
    surfaceContainerLowest = TrueBlack, surfaceContainerHigh = InstrumentSurfaceHigh,
    surfaceContainerHighest = Color(0xFF202024), onBackground = InstrumentText,
    onSurface = InstrumentText, onSurfaceVariant = InstrumentMuted,
    outline = InstrumentBorder, outlineVariant = Color(0xFF1C1C1F),
    error = ErrorRed, onError = Color.White,
)

val ColorScheme.isDark: Boolean get() = true
val ColorScheme.textSecondary: Color get() = OnDarkSecondary
val ColorScheme.textHint: Color get() = InstrumentMuted
val ColorScheme.subtleBorder: Color get() = InstrumentBorder
val ColorScheme.bubbleAssistant: Color get() = TrueBlack
val ColorScheme.chipBackground: Color get() = InstrumentSurfaceHigh

@Composable
fun ChatAppTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = InnoAiColors, typography = ChatAppTypography, content = content)
}
