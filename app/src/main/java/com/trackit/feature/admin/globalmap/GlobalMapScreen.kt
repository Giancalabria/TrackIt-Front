package com.trackit.feature.admin.globalmap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackit.core.ui.components.MapPlaceholder
import com.trackit.core.ui.theme.DeepForestGreen
import com.trackit.core.ui.theme.TerracottaOrange
import com.trackit.data.repository.FleetRepository
import com.trackit.data.repository.PackageRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalMapScreen() {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val deliveredCount = PackageRepository.getDeliveredCount()
    val pendingCount = PackageRepository.getPendingCount()
    val activeTrucks = FleetRepository.getActiveTruckCount()

    Box(modifier = Modifier.fillMaxSize()) {
        MapPlaceholder(label = "Mapa global (Simulado)")

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
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = DeepForestGreen,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-72).dp, y = (-96).dp)
        )
    }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Métricas del día",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Entregas completadas: $deliveredCount",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Paquetes pendientes: $pendingCount",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Camiones activos: $activeTrucks",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
