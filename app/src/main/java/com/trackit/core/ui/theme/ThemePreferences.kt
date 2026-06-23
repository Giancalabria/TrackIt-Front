package com.trackit.core.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trackit_theme"
)

class ThemePreferences(context: Context) {

    private val dataStore = context.applicationContext.themeDataStore

    val settingsFlow: Flow<ThemeSettings> = dataStore.data.map { prefs ->
        ThemeSettings(
            mode = ThemeMode.entries.find { it.name == prefs[KEY_THEME_MODE] }
                ?: ThemeMode.SYSTEM,
            useDynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
