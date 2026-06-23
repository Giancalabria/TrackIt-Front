package com.trackit.feature.warehouse.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.isEditableByWarehouse
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class HistoryFilterUiState(
    val showSheet: Boolean = false,
    val draft: HistoryFilters = HistoryFilters(),
    val applied: HistoryFilters = HistoryFilters()
)

class HistoryViewModel(
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterUiState = MutableStateFlow(HistoryFilterUiState())
    val filterUiState: StateFlow<HistoryFilterUiState> = _filterUiState.asStateFlow()

    private val _editForm = MutableStateFlow<PackageEditForm?>(null)
    val editForm: StateFlow<PackageEditForm?> = _editForm.asStateFlow()

    private val _packageToDelete = MutableStateFlow<Package?>(null)
    val packageToDelete: StateFlow<Package?> = _packageToDelete.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val filteredPackages: StateFlow<List<Package>> = combine(
        packageRepository.packages,
        _searchQuery,
        _filterUiState
    ) { allPackages, query, filters ->
        allPackages
            .filter { it.registeredByWarehouse }
            .filter { it.clientName.contains(query, ignoreCase = true) }
            .filter { filters.applied.matches(it) }
            .sortedWith(
                compareBy<Package> { pkg ->
                    when (pkg.status) {
                        PackageStatus.EN_CAMINO -> 0
                        PackageStatus.CARGADO -> 1
                        PackageStatus.ASIGNADO -> 2
                        PackageStatus.EN_DEPOSITO -> 3
                        else -> 4
                    }
                }.thenByDescending { it.updatedAt ?: Instant.MIN }
            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun openFilterSheet() {
        _filterUiState.update {
            it.copy(showSheet = true, draft = it.applied)
        }
    }

    fun dismissFilterSheet() {
        _filterUiState.update { it.copy(showSheet = false) }
    }

    fun toggleDraftStatus(status: PackageStatus) {
        _filterUiState.update { state ->
            val nextStatuses = state.draft.statuses.toMutableSet()
            if (status in nextStatuses) {
                nextStatuses.remove(status)
            } else {
                nextStatuses.add(status)
            }
            state.copy(draft = state.draft.copy(statuses = nextStatuses))
        }
    }

    fun setDraftDateFrom(date: LocalDate?) {
        _filterUiState.update { it.copy(draft = it.draft.copy(dateFrom = date)) }
    }

    fun setDraftDateTo(date: LocalDate?) {
        _filterUiState.update { it.copy(draft = it.draft.copy(dateTo = date)) }
    }

    fun applyFilters() {
        _filterUiState.update {
            it.copy(
                applied = it.draft,
                showSheet = false
            )
        }
    }

    fun clearFilters() {
        _filterUiState.update {
            HistoryFilterUiState(showSheet = false)
        }
    }

    fun startEditing(pkg: Package) {
        if (!pkg.isEditableByWarehouse()) {
            _feedbackMessage.value = "Solo podés editar paquetes en depósito o asignados."
            return
        }
        _editForm.value = PackageEditForm(
            packageId = pkg.id,
            clientName = pkg.clientName,
            address = pkg.address,
            destinationLat = pkg.destinationLat,
            destinationLon = pkg.destinationLon,
            size = pkg.size,
            isFragile = pkg.isFragile,
            barcode = pkg.barcode,
            status = pkg.status
        )
    }

    fun dismissEdit() {
        _editForm.value = null
    }

    fun onEditClientNameChange(value: String) {
        _editForm.update { it?.copy(clientName = value, errorMessage = null) }
    }

    fun onEditAddressChange(value: String) {
        _editForm.update { it?.copy(address = value, errorMessage = null) }
    }

    fun onEditSizeSelected(size: PackageSize) {
        _editForm.update { it?.copy(size = size, isSizeMenuExpanded = false) }
    }

    fun onEditSizeMenuExpandedChange(expanded: Boolean) {
        _editForm.update { it?.copy(isSizeMenuExpanded = expanded) }
    }

    fun onEditFragileChange(value: Boolean) {
        _editForm.update { it?.copy(isFragile = value) }
    }

    fun onEditBarcodeChange(value: String) {
        _editForm.update { it?.copy(barcode = value, errorMessage = null) }
    }

    fun saveEdit() {
        val form = _editForm.value ?: return
        val clientName = form.clientName.trim()
        val address = form.address.trim()
        if (clientName.isBlank() || address.isBlank()) {
            _editForm.update { it?.copy(errorMessage = "Completá cliente y dirección.") }
            return
        }

        viewModelScope.launch {
            _editForm.update { it?.copy(isSaving = true, errorMessage = null) }
            val existing = packageRepository.getPackageById(form.packageId)
            if (existing == null) {
                _editForm.update {
                    it?.copy(isSaving = false, errorMessage = "Paquete no encontrado.")
                }
                return@launch
            }

            val updated = existing.copy(
                clientName = clientName,
                address = address,
                size = form.size,
                isFragile = form.isFragile,
                barcode = form.barcode.trim()
            )

            packageRepository.updatePackage(updated).fold(
                onSuccess = {
                    _editForm.value = null
                    _feedbackMessage.value = "Paquete actualizado."
                },
                onFailure = {
                    _editForm.update {
                        it?.copy(
                            isSaving = false,
                            errorMessage = "No se pudo guardar. Reintentá."
                        )
                    }
                }
            )
        }
    }

    fun requestDelete(pkg: Package) {
        if (!pkg.isEditableByWarehouse()) {
            _feedbackMessage.value = "Solo podés eliminar paquetes en depósito o asignados."
            return
        }
        _packageToDelete.value = pkg
    }

    fun dismissDelete() {
        _packageToDelete.value = null
    }

    fun confirmDelete() {
        val pkg = _packageToDelete.value ?: return
        viewModelScope.launch {
            packageRepository.deletePackage(pkg.id).fold(
                onSuccess = {
                    _packageToDelete.value = null
                    _feedbackMessage.value = "Paquete eliminado."
                },
                onFailure = {
                    _packageToDelete.value = null
                    _feedbackMessage.value = "No se pudo eliminar el paquete."
                }
            )
        }
    }

    fun consumeFeedbackMessage() {
        _feedbackMessage.value = null
    }
}
