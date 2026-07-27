package com.embychapter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Standard Material 3 shapes with Expressive corner radii
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Expressive shape library: use these for hero cards, posters, avatars
object ExpressiveShapes {
    val SquircleLarge = RoundedCornerShape(28.dp)
    val SquircleExtraLarge = RoundedCornerShape(36.dp)
    val FullyRounded = RoundedCornerShape(50)
    val Pill = RoundedCornerShape(50)
    val Sharp = RectangleShape

    // Asymmetric shapes for decorative moments
    val PosterCard = RoundedCornerShape(
        topStart = 24.dp, topEnd = 8.dp,
        bottomStart = 8.dp, bottomEnd = 24.dp
    )
    val HeroCard = RoundedCornerShape(
        topStart = 32.dp, topEnd = 32.dp,
        bottomStart = 16.dp, bottomEnd = 32.dp
    )
    val StatCard = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = 20.dp, bottomEnd = 8.dp
    )
}
