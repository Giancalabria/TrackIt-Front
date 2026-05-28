package com.trackit.feature.warehouse.loadtruck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.PackageStatusChip
import com.trackit.data.model.Package
import com.trackit.data.model.Truck

@Composable
fun LoadTruckScreen(
    viewModel: LoadTruckViewModel = viewModel()
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.step == LoadTruckStep.SELECT_PACKAGES && uiState.packages.isNotEmpty()) {
                Button(
                    onClick = viewModel::confirmPackages,
                    enabled = uiState.selectedPackageIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                ) {
                    Text("Confirmar (${uiState.selectedPackageIds.size})")
                }
            }
        }
    ) { padding ->
        when (uiState.step) {
            LoadTruckStep.SELECT_PACKAGES -> PackageSelectionContent(
                packages = uiState.packages,
                selectedPackageIds = uiState.selectedPackageIds,
                onPackageClick = viewModel::togglePackage,
                modifier = Modifier.padding(padding)
            )

            LoadTruckStep.SELECT_TRUCK -> TruckSelectionContent(
                trucks = uiState.trucks,
                selectedCount = uiState.selectedPackageIds.size,
                onBack = viewModel::backToPackageSelection,
                onTruckClick = viewModel::loadSelectedPackages,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun PackageSelectionContent(
    packages: List<Package>,
    selectedPackageIds: Set<String>,
    onPackageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (packages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay paquetes en depósito para cargar.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Cargar camión",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Seleccioná los paquetes que van al camión.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        items(packages, key = { it.id }) { packageItem ->
            SelectablePackageCard(
                packageItem = packageItem,
                selected = packageItem.id in selectedPackageIds,
                onClick = { onPackageClick(packageItem.id) }
            )
        }
    }
}

@Composable
private fun SelectablePackageCard(
    packageItem: Package,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() }
            )

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
                Text(
                    text = "ETA: ${packageItem.eta}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            PackageStatusChip(status = packageItem.status)
        }
    }
}

@Composable
private fun TruckSelectionContent(
    trucks: List<Truck>,
    selectedCount: Int,
    onBack: () -> Unit,
    onTruckClick: (Truck) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
                Column {
                    Text(
                        text = "Seleccionar camión",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "$selectedCount paquete(s) seleccionados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(trucks, key = { it.id }) { truck ->
            TruckCard(
                truck = truck,
                onClick = { onTruckClick(truck) }
            )
        }
    }
}

@Composable
private fun TruckCard(
    truck: Truck,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = truck.plate,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Chofer: ${truck.driverName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${truck.deliveredCount}/${truck.totalCount} entregas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
