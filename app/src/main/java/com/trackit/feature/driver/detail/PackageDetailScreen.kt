package com.trackit.feature.driver.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.BarcodeScannerSheet
import com.trackit.core.ui.components.MapPlaceholder
import com.trackit.core.ui.components.PackageStatusChip
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDetailScreen(
    onBack: () -> Unit,
    viewModel: PackageDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.scanCompleted) {
        if (uiState.scanCompleted) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Paquete") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val pkg = uiState.packageItem
            if (pkg != null && !uiState.scanCompleted) {
                if (pkg.status == PackageStatus.CARGADO || pkg.status == PackageStatus.EN_CAMINO) {
                    ExtendedFloatingActionButton(
                        onClick = viewModel::openScanner,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Entregar")
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.packageItem == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Paquete no encontrado.")
                }
            }

            else -> {
                val packageItem = uiState.packageItem!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        MapPlaceholder()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = packageItem.clientName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = packageItem.address,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Tamaño: ${packageItem.size.toLabel()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Peso estimado: 4,5 kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Dimensiones: 40 x 30 x 20 cm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (packageItem.isFragile) {
                            SuggestionChip(
                                onClick = {},
                                enabled = false,
                                label = { Text("Frágil") }
                            )
                        }
                        PackageStatusChip(status = packageItem.status)
                    }
                }
            }
        }
    }

    if (uiState.isScannerOpen) {
        val scannerTitle = when (uiState.packageItem?.status) {
            PackageStatus.CARGADO -> "Escanear para Entregar"
            PackageStatus.EN_CAMINO -> "Escanear para Entregar"
            else -> "Escanear Paquete"
        }

        BarcodeScannerSheet(
            onCodeScanned = viewModel::onCodeScanned,
            onDismiss = viewModel::closeScanner,
            title = scannerTitle
        )
    }
}

private fun PackageSize.toLabel(): String = when (this) {
    PackageSize.SMALL -> "Pequeño"
    PackageSize.MEDIUM -> "Mediano"
    PackageSize.LARGE -> "Grande"
}
