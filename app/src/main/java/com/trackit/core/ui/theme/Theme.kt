package com.trackit.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamicColor && supportsDynamicColor -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TrackItTypography,
        content = content
    )
}
