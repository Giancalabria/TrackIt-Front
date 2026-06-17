package com.trackit.feature.warehouse.loadtruck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.Truck
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoadTruckStep {
    SELECT_PACKAGES,
    SELECT_TRUCK
}

data class LoadTruckUiState(
    val packages: List<Package> = emptyList(),
    val trucks: List<Truck> = emptyList(),
    val selectedPackageIds: Set<String> = emptySet(),
    val step: LoadTruckStep = LoadTruckStep.SELECT_PACKAGES,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

class LoadTruckViewModel(
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository,
    private val fleetRepository: IFleetRepository = SupabaseLocator.fleetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoadTruckUiState())
    val uiState: StateFlow<LoadTruckUiState> = _uiState.asStateFlow()

    init {
        combine(
            packageRepository.packages,
            fleetRepository.trucks
        ) { allPackages, trucks ->
            allPackages to trucks
        }
            .onEach { (allPackages, trucks) ->
                _uiState.update { state ->
                    // The warehouse can load packages that are still in the depot OR already
                    // assigned by the optimizer (handoff: ASIGNADO -> CARGADO).
                    val warehousePackages = allPackages
                        .filter { packageItem ->
                            packageItem.status == PackageStatus.EN_DEPOSITO ||
                                packageItem.status == PackageStatus.ASIGNADO
                        }
                        .sortedBy { packageItem -> packageItem.eta }
                    val availablePackageIds = warehousePackages
                        .map { packageItem -> packageItem.id }
                        .toSet()

                    state.copy(
                        packages = warehousePackages,
                        trucks = trucks,
                        selectedPackageIds = state.selectedPackageIds.intersect(availablePackageIds)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun togglePackage(packageId: String) {
        _uiState.update { state ->
            val selectedPackageIds = if (packageId in state.selectedPackageIds) {
                state.selectedPackageIds - packageId
            } else {
                state.selectedPackageIds + packageId
            }
            state.copy(selectedPackageIds = selectedPackageIds)
        }
    }

    fun confirmPackages() {
        if (_uiState.value.selectedPackageIds.isEmpty()) return
        _uiState.update { it.copy(step = LoadTruckStep.SELECT_TRUCK, errorMessage = null) }
    }

    fun backToPackageSelection() {
        _uiState.update { it.copy(step = LoadTruckStep.SELECT_PACKAGES) }
    }

    fun loadSelectedPackages(truck: Truck) {
        val selectedIds = _uiState.value.selectedPackageIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val selectedPackages = _uiState.value.packages.filter { packageItem ->
                packageItem.id in selectedIds
            }

            var failedCount = 0
            selectedPackages.forEach { packageItem ->
                val result = packageRepository.updatePackage(
                    packageItem.copy(
                        status = PackageStatus.CARGADO,
                        assignedDriverId = truck.driverId
                    )
                )
                if (result.isFailure) failedCount++
            }

            if (failedCount > 0) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudieron cargar $failedCount paquete(s). Reintentá."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        selectedPackageIds = emptySet(),
                        step = LoadTruckStep.SELECT_PACKAGES,
                        successMessage = "${selectedPackages.size} paquete(s) cargado(s) en ${truck.plate}",
                        isSaving = false
                    )
                }
            }
        }
    }

    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
