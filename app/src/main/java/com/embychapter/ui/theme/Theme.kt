package com.embychapter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    background = BackgroundDark,
    surface = Surface,
    surfaceVariant = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = Color.White,
    outline = TextMuted.copy(alpha = 0.3f)
)

// Light scheme is provided for completeness / future system-theme support.
// The app's brand palette is dark-navy by design, so darkTheme defaults to true.
private val LightColorScheme = lightColorScheme(
    primary = PrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    background = Color(0xFFF6F3EE),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7EDEC),
    onBackground = Color(0xFF13222B),
    onSurface = Color(0xFF13222B),
    onSurfaceVariant = Color(0xFF41565E),
    error = Danger,
    onError = Color.White,
    outline = TextMuted.copy(alpha = 0.4f)
)

@Composable
fun EmbyChapterTheme(
    darkTheme: Boolean = true,
    // Brand-driven app: dynamic color is off by default. To enable expressive
    // dynamic color on Android 12+, flip this to true (requires the
    // dynamicColorScheme(context, darkTheme, isExpressive = true) signature
    // available in your installed material3 version).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // On material3 1.5.0-alpha (M3 Expressive) the default MaterialTheme
    // motionScheme already uses expressive spring physics, so no extra setup
    // is required for components to animate expressively.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
