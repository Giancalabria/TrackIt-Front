package com.trackit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.core.navigation.TrackItNavHost
import com.trackit.core.ui.theme.ThemeLocator
import com.trackit.core.ui.theme.ThemeSettings
import com.trackit.core.ui.theme.TrackItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeSettings by ThemeLocator.preferences.settingsFlow
                .collectAsStateWithLifecycle(initialValue = ThemeSettings())

            TrackItTheme(
                themeMode = themeSettings.mode,
                useDynamicColor = themeSettings.useDynamicColor
            ) {
                TrackItNavHost()
            }
        }
    }
}
