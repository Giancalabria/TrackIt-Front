package com.trackit.feature.warehouse.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.EmptyState
import com.trackit.core.ui.components.HistoryPackageCard
import com.trackit.core.ui.filters.PackageFilterBar
import com.trackit.core.ui.filters.PackageFilterSheet
import com.trackit.core.ui.filters.PackageFilterSheetConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val packages by viewModel.filteredPackages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterUiState by viewModel.filterUiState.collectAsStateWithLifecycle()
    val editForm by viewModel.editForm.collectAsStateWithLifecycle()
    val packageToDelete by viewModel.packageToDelete.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeFeedbackMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PackageFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                appliedFilters = filterUiState.applied,
                onFilterClick = viewModel::openFilterSheet,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (packages.isEmpty()) {
                val message = when {
                    searchQuery.isNotEmpty() ->
                        "No se encontraron paquetes para \"$searchQuery\"."
                    filterUiState.applied.isActive ->
                        "No hay paquetes con esos filtros."
                    else ->
                        "Todavía no hay ingresos registrados."
                }
                EmptyState(message = message)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(packages, key = { it.id }) { packageItem ->
                        HistoryPackageCard(
                            packageItem = packageItem,
                            onEditClick = { viewModel.startEditing(packageItem) },
                            onDeleteClick = { viewModel.requestDelete(packageItem) }
                        )
                    }
                }
            }
        }
    }

    if (filterUiState.showSheet) {
        PackageFilterSheet(
            config = PackageFilterSheetConfig.history(),
            draft = filterUiState.draft,
            onToggleStatus = viewModel::toggleDraftStatus,
            onDateFromSelected = viewModel::setDraftDateFrom,
            onDateToSelected = viewModel::setDraftDateTo,
            onApply = viewModel::applyFilters,
            onClear = viewModel::clearFilters,
            onDismiss = viewModel::dismissFilterSheet
        )
    }

    editForm?.let { form ->
        PackageEditSheet(
            form = form,
            onClientNameChange = viewModel::onEditClientNameChange,
            onAddressChange = viewModel::onEditAddressChange,
            onSizeSelected = viewModel::onEditSizeSelected,
            onSizeMenuExpandedChange = viewModel::onEditSizeMenuExpandedChange,
            onFragileChange = viewModel::onEditFragileChange,
            onBarcodeChange = viewModel::onEditBarcodeChange,
            onSave = viewModel::saveEdit,
            onDismiss = viewModel::dismissEdit
        )
    }

    packageToDelete?.let { pkg ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Eliminar paquete") },
            text = {
                Text("¿Eliminar el paquete de ${pkg.clientName}? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text("Cancelar")
                }
            }
        )
    }
}
