package com.trackit.feature.admin.assign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.EmptyState
import com.trackit.core.ui.components.PackageSelectionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignRouteScreen(
    driverId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AssignRouteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(driverId) {
        viewModel.initialize(driverId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveChanges() },
                    text = { Text("Guardar Cambios") },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Chofer: ${uiState.driverName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Seleccioná los paquetes que debe llevar hoy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                if (uiState.availablePackages.isEmpty() && uiState.currentRoutePackages.isEmpty()) {
                    EmptyState(message = "No hay paquetes disponibles para asignar.")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.currentRoutePackages.isNotEmpty()) {
                            item {
                                Text(
                                    text = "En ruta actual",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(uiState.currentRoutePackages) { pkg ->
                                PackageSelectionItem(
                                    pkg = pkg,
                                    isSelected = uiState.selectedPackageIds.contains(pkg.id),
                                    onToggle = { viewModel.togglePackageSelection(pkg.id) }
                                )
                            }
                        }

                        if (uiState.availablePackages.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Disponibles en depósito",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            items(uiState.availablePackages) { pkg ->
                                PackageSelectionItem(
                                    pkg = pkg,
                                    isSelected = uiState.selectedPackageIds.contains(pkg.id),
                                    onToggle = { viewModel.togglePackageSelection(pkg.id) }
                                )
                            }
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
