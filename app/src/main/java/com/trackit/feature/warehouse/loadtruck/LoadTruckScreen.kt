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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.trackit.core.ui.components.BarcodeScannerSheet
import com.trackit.core.ui.components.PackageStatusChip
import com.trackit.core.ui.filters.PackageFilterBar
import com.trackit.core.ui.filters.PackageFilterSheet
import com.trackit.core.ui.filters.PackageFilterSheetConfig
import com.trackit.data.model.Package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadTruckScreen(
    onBack: () -> Unit = {},
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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            when (uiState.step) {
                LoadTruckStep.SELECT_TRUCK -> {
                    TopAppBar(
                        title = { Text("Cargar camión") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
                        }
                    )
                }
                LoadTruckStep.LOADING -> {
                    val truck = uiState.selectedTruck
                    val info = uiState.trucks.firstOrNull { it.truck.id == truck?.id }
                    TopAppBar(
                        title = {
                            Column {
                                Text("Camión ${truck?.plate.orEmpty()}")
                                Text(
                                    text = buildString {
                                        append("Chofer: ${truck?.driverName.orEmpty()}")
                                        info?.let {
                                            append(" · ${it.loadedCount} de ${it.totalCount} cargados")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = viewModel::backToTruckSelection) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Cambiar camión"
                                )
                            }
                        }
                    )
                }
                LoadTruckStep.PACKAGE_DETAIL -> {
                    TopAppBar(
                        title = { Text("Detalle de paquete") },
                        navigationIcon = {
                            IconButton(onClick = viewModel::backToLoading) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.step == LoadTruckStep.PACKAGE_DETAIL && !uiState.isSaving) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::openScanner,
                    icon = {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    },
                    text = { Text("Escanear para cargar") }
                )
            }
        }
    ) { padding ->
        when (uiState.step) {
            LoadTruckStep.SELECT_TRUCK -> TruckSelectionContent(
                trucks = uiState.trucks,
                onTruckClick = viewModel::selectTruck,
                modifier = Modifier.padding(padding)
            )
            LoadTruckStep.LOADING -> LoadingSessionContent(
                pendingPackages = uiState.pendingPackages,
                loadedPackages = uiState.loadedPackages,
                searchQuery = uiState.searchQuery,
                appliedFilters = uiState.filterUiState.applied,
                isSaving = uiState.isSaving,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFilterClick = viewModel::openFilterSheet,
                onPackageClick = viewModel::selectPackage,
                modifier = Modifier.padding(padding)
            )
            LoadTruckStep.PACKAGE_DETAIL -> {
                uiState.selectedPackage?.let { pkg ->
                    PackageDetailContent(
                        pkg = pkg,
                        isSaving = uiState.isSaving,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    if (uiState.filterUiState.showSheet) {
        PackageFilterSheet(
            config = PackageFilterSheetConfig.loadTruck(),
            draft = uiState.filterUiState.draft,
            onToggleStatus = viewModel::toggleDraftStatus,
            onDateFromSelected = { },
            onDateToSelected = { },
            onApply = viewModel::applyFilters,
            onClear = viewModel::clearFilters,
            onDismiss = viewModel::dismissFilterSheet
        )
    }

    if (uiState.isScannerOpen) {
        BarcodeScannerSheet(
            onCodeScanned = viewModel::onBarcodeScanned,
            onDismiss = viewModel::closeScanner,
            title = "Escanear para cargar"
        )
    }
}

@Composable
private fun PackageDetailContent(
    pkg: Package,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isSaving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Text(
            text = "Información de entrega",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoItem(label = "Cliente", value = pkg.clientName)
                InfoItem(label = "Dirección", value = pkg.address)
                if (pkg.barcode.isNotBlank()) {
                    InfoItem(label = "Código esperado", value = pkg.barcode)
                }
            }
        }

    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun TruckSelectionContent(
    trucks: List<TruckLoadInfo>,
    onTruckClick: (com.trackit.data.model.Truck) -> Unit,
    modifier: Modifier = Modifier
) {
    if (trucks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay camiones disponibles.",
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
                text = "Elegí el camión que vas a cargar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(trucks, key = { it.truck.id }) { info ->
            TruckLoadCard(
                info = info,
                onClick = { onTruckClick(info.truck) }
            )
        }
    }
}

@Composable
private fun TruckLoadCard(
    info: TruckLoadInfo,
    onClick: () -> Unit
) {
    val truck = info.truck
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
                    text = when {
                        info.totalCount == 0 ->
                            "Sin paquetes asignados hoy"
                        info.pendingCount == 0 ->
                            "${info.loadedCount} cargados · listo"
                        else ->
                            "${info.pendingCount} pendientes · ${info.loadedCount} cargados"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingSessionContent(
    pendingPackages: List<Package>,
    loadedPackages: List<Package>,
    searchQuery: String,
    appliedFilters: com.trackit.core.ui.filters.PackageFilters,
    isSaving: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onPackageClick: (Package) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        PackageFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            appliedFilters = appliedFilters,
            onFilterClick = onFilterClick
        )

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (pendingPackages.isEmpty() && loadedPackages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        searchQuery.isNotEmpty() ->
                            "No se encontraron paquetes para \"$searchQuery\"."
                        appliedFilters.isActive ->
                            "No hay paquetes con esos filtros."
                        else ->
                            "No hay paquetes asignados a este camión hoy."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pendingPackages.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pendientes de cargar (${pendingPackages.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(pendingPackages, key = { "pending-${it.id}" }) { packageItem ->
                        LoadTruckPackageCard(
                            packageItem = packageItem,
                            onClick = { onPackageClick(packageItem) }
                        )
                    }
                }

                if (loadedPackages.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ya cargados (${loadedPackages.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(loadedPackages, key = { "loaded-${it.id}" }) { packageItem ->
                        LoadTruckPackageCard(
                            packageItem = packageItem,
                            onClick = { /* Already loaded */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadTruckPackageCard(
    packageItem: Package,
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                packageItem.routeOrder?.let { order ->
                    Text(
                        text = "Orden de ruta: $order",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            PackageStatusChip(
                status = packageItem.status,
                driverName = packageItem.assignedDriverName
            )
        }
    }
}
