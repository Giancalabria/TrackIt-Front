package com.trackit.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trackit.core.ui.theme.MapPlaceholder

@Composable
fun MapPlaceholder(
    modifier: Modifier = Modifier,
    label: String = "Mapa (Simulado)"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MapPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
