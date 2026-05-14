package com.trackit.feature.admin.globalmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.MapPlaceholder
import com.trackit.core.ui.theme.DeepForestGreen
import com.trackit.core.ui.theme.TerracottaOrange

@Composable
fun GlobalMapScreen(
    viewModel: GlobalMapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MapPlaceholder(label = "Mapa global (Simulado)")

        MapMarkers()

        if (!uiState.isLoading) {
            MetricsOverlay(
                delivered = uiState.deliveredCount,
                pending = uiState.pendingCount,
                trucks = uiState.activeTrucks,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun MetricsOverlay(
    delivered: Int,
    pending: Int,
    trucks: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem("Entregados", delivered.toString(), DeepForestGreen)
            VerticalDivider(modifier = Modifier.height(24.dp))
            MetricItem("Pendientes", pending.toString(), TerracottaOrange)
            VerticalDivider(modifier = Modifier.height(24.dp))
            MetricItem("Camiones", trucks.toString(), MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MapMarkers() {
    Box(modifier = Modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = TerracottaOrange,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 48.dp, y = 72.dp)
        )
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = DeepForestGreen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-56).dp, y = 120.dp)
        )
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = TerracottaOrange,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 80.dp, y = 40.dp)
        )
    }
}
