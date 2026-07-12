package com.example.chatapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.chatapp.R

val PlexSans = FontFamily(
    Font(R.font.ibm_plex_sans, FontWeight.Normal),
    Font(R.font.ibm_plex_sans, FontWeight.Medium),
    Font(R.font.ibm_plex_sans, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans, FontWeight.Bold),
)
val PlexMono = FontFamily(Font(R.font.ibm_plex_mono_medium, FontWeight.Medium))

val ChatAppTypography = Typography(
    displayLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp),
    displayMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 31.sp),
    headlineMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 9.sp, lineHeight = 13.sp, letterSpacing = 0.6.sp),
)
