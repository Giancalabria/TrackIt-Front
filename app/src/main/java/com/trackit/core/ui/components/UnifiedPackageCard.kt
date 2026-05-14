package com.trackit.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trackit.core.ui.theme.DeepForestGreen
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus

@Composable
fun UnifiedPackageCard(
    packageItem: Package,
    onActionClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información del paquete (Izquierda)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageItem.clientName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = packageItem.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ETA: ${packageItem.eta}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (packageItem.status == PackageStatus.EN_CAMINO) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Estado/Acción (Derecha)
            Box(modifier = Modifier.widthIn(min = 100.dp), contentAlignment = Alignment.CenterEnd) {
                when (packageItem.status) {
                    PackageStatus.ASIGNADO -> {
                        Button(
                            onClick = onActionClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Cargar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    PackageStatus.CARGADO -> {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Cargado",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    PackageStatus.EN_CAMINO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "En camino",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    PackageStatus.ENTREGADO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = DeepForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Entregado",
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepForestGreen
                            )
                        }
                    }
                    else -> {
                        PackageStatusChip(packageItem.status)
                    }
                }
            }
        }
    }
}
