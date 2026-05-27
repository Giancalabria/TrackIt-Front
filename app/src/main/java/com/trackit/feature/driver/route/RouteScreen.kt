package com.trackit.feature.driver.route

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.BarcodeScannerSheet
import com.trackit.core.ui.components.UnifiedPackageCard
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus

@Composable
fun RouteScreen(
    onPackageClick: (String) -> Unit,
    viewModel: RouteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var scanningPackage by remember { mutableStateOf<Package?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.packages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No tienes paquetes asignados para hoy.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Mi Ruta de Hoy",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(uiState.packages, key = { it.id }) { packageItem ->
                    UnifiedPackageCard(
                        packageItem = packageItem,
                        onActionClick = { 
                            scanningPackage = packageItem
                        },
                        onCardClick = { onPackageClick(packageItem.id) }
                    )
                }
            }
        }
    }

    // Scanner Dialog Logic
    scanningPackage?.let { pkg ->
        val scannerTitle = when (pkg.status) {
            PackageStatus.ASIGNADO -> "Escanear para Cargar"
            PackageStatus.EN_CAMINO -> "Escanear para Entregar"
            else -> "Escanear Paquete"
        }

        BarcodeScannerSheet(
            title = scannerTitle,
            onCodeScanned = { code ->
                if (pkg.status == PackageStatus.ASIGNADO) {
                    viewModel.loadPackage(pkg.id, code)
                } else if (pkg.status == PackageStatus.EN_CAMINO) {
                    viewModel.deliverPackage(pkg.id, code)
                }
                scanningPackage = null
            },
            onDismiss = { scanningPackage = null }
        )
    }
}
