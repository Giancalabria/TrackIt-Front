package com.trackit.feature.warehouse.intake

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.BarcodeScannerSheet
import com.trackit.data.model.PackageSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ingreso de paquetes",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = uiState.clientName,
                onValueChange = viewModel::onClientNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cliente") },
                singleLine = true
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = viewModel::onAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dirección de destino") },
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.isSearchingAddress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )

                if (uiState.addressSearchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        uiState.addressSearchResults.forEach { result ->
                            ListItem(
                                headlineContent = { Text(result.properties.getDisplayName()) },
                                supportingContent = { Text(result.properties.city ?: "") },
                                modifier = Modifier.clickable {
                                    viewModel.onAddressSelected(result)
                                }
                            )
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = uiState.isSizeMenuExpanded,
                onExpandedChange = viewModel::onSizeMenuExpandedChange
            ) {
                OutlinedTextField(
                    value = uiState.size.toLabel(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Tamaño") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isSizeMenuExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = uiState.isSizeMenuExpanded,
                    onDismissRequest = { viewModel.onSizeMenuExpandedChange(false) }
                ) {
                    PackageSize.entries.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.toLabel()) },
                            onClick = { viewModel.onSizeSelected(size) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Frágil")
                Switch(
                    checked = uiState.isFragile,
                    onCheckedChange = viewModel::onFragileChange
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = viewModel::openScanner,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.barcode.isEmpty()) "Escanear Código de Barras" else "Código: ${uiState.barcode}")
            }
        }
    }

    if (uiState.isScannerOpen) {
        BarcodeScannerSheet(
            onCodeScanned = viewModel::onBarcodeScanned,
            onDismiss = viewModel::closeScanner,
            title = "Asociar Código al Ingreso"
        )
    }
}

private fun PackageSize.toLabel(): String = when (this) {
    PackageSize.SMALL -> "Pequeño"
    PackageSize.MEDIUM -> "Mediano"
    PackageSize.LARGE -> "Grande"
}
