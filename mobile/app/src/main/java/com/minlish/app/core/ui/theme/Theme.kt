package com.minlish.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = MinLishColors.Primary,
    onPrimary = Color.White,
    primaryContainer = MinLishColors.PrimaryContainer,
    onPrimaryContainer = MinLishColors.OnPrimaryContainer,
    secondary = MinLishColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = MinLishColors.SecondaryContainer,
    onSecondaryContainer = MinLishColors.OnSecondaryContainer,
    tertiary = MinLishColors.Accent,
    onTertiary = Color.White,
    tertiaryContainer = MinLishColors.AccentContainer,
    onTertiaryContainer = Color(0xFF2C1F52),
    background = MinLishColors.Background,
    onBackground = MinLishColors.TextPrimary,
    surface = MinLishColors.Surface,
    onSurface = MinLishColors.TextPrimary,
    surfaceVariant = MinLishColors.SurfaceVariant,
    onSurfaceVariant = MinLishColors.TextSecondary,
    outline = MinLishColors.Outline,
    outlineVariant = MinLishColors.Outline,
    error = MinLishColors.Error,
    onError = Color.White,
    errorContainer = MinLishColors.ErrorContainer,
    onErrorContainer = Color(0xFF6B1C16),
)

private val DarkColors = darkColorScheme(
    primary = MinLishColors.PrimaryLight,
    onPrimary = Color(0xFF06281D),
    primaryContainer = MinLishColors.PrimaryDark,
    onPrimaryContainer = MinLishColors.PrimaryContainer,
    secondary = MinLishColors.Secondary,
    tertiary = MinLishColors.Accent,
    background = MinLishColors.DarkBackground,
    onBackground = Color(0xFFECEDF0),
    surface = MinLishColors.DarkSurface,
    onSurface = Color(0xFFECEDF0),
    surfaceVariant = MinLishColors.DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9AEB8),
    outline = MinLishColors.DarkOutline,
    error = MinLishColors.Error,
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun MinLishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = MinLishShapes,
        content = content,
    )
}
