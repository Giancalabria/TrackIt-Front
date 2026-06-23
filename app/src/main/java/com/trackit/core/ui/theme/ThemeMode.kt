package com.trackit.core.ui.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun label(): String = when (this) {
        SYSTEM -> "Sistema"
        LIGHT -> "Claro"
        DARK -> "Oscuro"
    }
}

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true
)
