package com.trackit.core.ui.theme

import android.content.Context

object ThemeLocator {
    lateinit var preferences: ThemePreferences
        private set

    fun init(context: Context) {
        preferences = ThemePreferences(context.applicationContext)
    }
}
