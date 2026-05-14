package com.trackit.feature.driver.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackit.core.ui.components.MapPlaceholder

@Composable
fun DriverMapScreen() {
    MapPlaceholder(
        modifier = Modifier.fillMaxSize(),
        label = "Mapa de ruta (Simulado)"
    )
}
