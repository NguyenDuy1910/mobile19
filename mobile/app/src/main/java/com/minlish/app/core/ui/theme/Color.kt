package com.minlish.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * MinLish cute & friendly palette.
 *
 * Direction: soft, cute, modern (Duolingo / Quizlet / Drops feeling) but cleaner.
 * Primary = soft green, Secondary = warm peach, Accent = lavender/mint.
 * Backgrounds = cream / off-white, cards = white with soft shadows.
 * Error = soft red (not aggressive), Success = friendly green.
 */
object MinLishColors {
    // Brand greens
    val Primary = Color(0xFF49C293)
    val PrimaryDark = Color(0xFF2FA277)
    val PrimaryLight = Color(0xFF6FD3AC)
    val PrimaryContainer = Color(0xFFD8F3E8)
    val OnPrimaryContainer = Color(0xFF0E3B2C)

    // Warm peach / yellow secondary
    val Secondary = Color(0xFFFFB570)
    val SecondaryContainer = Color(0xFFFFE7D2)
    val OnSecondaryContainer = Color(0xFF5A3416)

    // Lavender accent + mint
    val Accent = Color(0xFFB39DF0)
    val AccentContainer = Color(0xFFEAE3FF)
    val Mint = Color(0xFF7FD9C4)
    val MintContainer = Color(0xFFDBF6EF)

    // Neutrals
    val Background = Color(0xFFFBFAF6)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFF6F5F0)
    val SurfaceVariant = Color(0xFFF1F0EA)
    val Outline = Color(0xFFE7E5DD)
    val TextPrimary = Color(0xFF2A2A33)
    val TextSecondary = Color(0xFF8C8C99)

    // Semantic
    val Error = Color(0xFFE8736A)
    val ErrorContainer = Color(0xFFFFE2DE)
    val Success = Color(0xFF49C293)
    val SuccessContainer = Color(0xFFD8F3E8)
    val Warning = Color(0xFFF5A742)
    val WarningContainer = Color(0xFFFFEACE)
    val Info = Color(0xFF5FA8E8)
    val InfoContainer = Color(0xFFDCEDFB)

    // Playful highlights
    val Streak = Color(0xFFFF8A5B) // fire
    val Trophy = Color(0xFFF6B93B) // gold/star
    val Target = Color(0xFF7E6CF0) // accuracy

    // Flashcard rating buttons
    val RatingAgain = Color(0xFFEC7E74)
    val RatingHard = Color(0xFFF5A742)
    val RatingGood = Color(0xFF5FA8E8)
    val RatingEasy = Color(0xFF49C293)

    // Gradients
    val HeroGradient = listOf(Color(0xFF5BD0A4), Color(0xFF3FB587))
    val PeachGradient = listOf(Color(0xFFFFC78C), Color(0xFFFFB06A))
    val LavenderGradient = listOf(Color(0xFFC4B2F4), Color(0xFFA98FEC))

    // Dark
    val DarkBackground = Color(0xFF14161B)
    val DarkSurface = Color(0xFF1E2128)
    val DarkSurfaceVariant = Color(0xFF272B33)
    val DarkOutline = Color(0xFF3A3F49)
}

