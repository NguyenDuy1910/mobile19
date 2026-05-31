package com.minlish.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Soft, rounded shapes for a friendly feel. */
val MinLishShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Named shape tokens for consistent rounding across the design system. */
object MinLishShapeTokens {
    val pill = RoundedCornerShape(50)
    val button = RoundedCornerShape(16.dp)
    val field = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(20.dp)
    val cardLarge = RoundedCornerShape(28.dp)
    val chip = RoundedCornerShape(12.dp)
}

