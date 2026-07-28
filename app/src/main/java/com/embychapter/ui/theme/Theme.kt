package com.embychapter.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Primary,
    error = Danger,
    onError = Color.White,
    outline = TextMuted.copy(alpha = 0.4f),
    outlineVariant = TextMuted.copy(alpha = 0.2f),
    scrim = Color.Black.copy(alpha = 0.6f)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmbyChapterTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = ExpressiveDarkColorScheme,
        typography = ExpressiveTypography,
        shapes = AppShapes,
        content = content
    )
}
