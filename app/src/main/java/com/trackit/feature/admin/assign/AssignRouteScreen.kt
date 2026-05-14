package com.trackit.feature.admin.assign

import androidx.compose.foundation.clickable
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
import com.trackit.data.model.Package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignRouteScreen(
    driverId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AssignRouteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Inicializamos el ViewModel con el driverId recibido
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

                    if (uiState.availablePackages.isEmpty() && uiState.currentRoutePackages.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay paquetes disponibles para asignar.")
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

@Composable
fun PackageSelectionItem(
    pkg: Package,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pkg.clientName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = pkg.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (pkg.isFragile) {
                Text(
                    "Frágil",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
