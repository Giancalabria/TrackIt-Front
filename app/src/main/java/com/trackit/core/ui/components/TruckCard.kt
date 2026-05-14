package com.trackit.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.trackit.data.model.Truck

@Composable
fun TruckCard(
    truck: Truck,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (truck.totalCount == 0) 0f else truck.deliveredCount.toFloat() / truck.totalCount
    
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = truck.driverName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Patente: ${truck.plate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (truck.totalCount > 0) "${truck.deliveredCount}/${truck.totalCount} entregados" else "Sin ruta asignada",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver detalles",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
