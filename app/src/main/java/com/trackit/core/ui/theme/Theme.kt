package com.trackit.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TerracottaOrange,
    onPrimary = Color.White,
    primaryContainer = TerracottaLight,
    onPrimaryContainer = Color(0xFF3E1508),
    secondary = DeepForestGreen,
    onSecondary = Color.White,
    secondaryContainer = ForestLight,
    onSecondaryContainer = Color(0xFF0F2A1A),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59F),
    onPrimary = Color(0xFF5C1D0A),
    primaryContainer = Color(0xFF7A2F18),
    onPrimaryContainer = TerracottaLight,
    secondary = Color(0xFF9BC9AD),
    onSecondary = Color(0xFF0F2A1A),
    secondaryContainer = Color(0xFF1F4A31),
    onSecondaryContainer = ForestLight,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun TrackItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TrackItTypography,
        content = content
    )
}
