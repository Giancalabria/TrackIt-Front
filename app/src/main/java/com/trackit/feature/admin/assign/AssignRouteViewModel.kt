package com.trackit.feature.admin.assign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseFleetRepository
import com.trackit.data.repository.SupabaseLocator
import com.trackit.data.repository.SupabasePackageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AssignRouteUiState(
    val driverName: String = "",
    val availablePackages: List<Package> = emptyList(),
    val currentRoutePackages: List<Package> = emptyList(),
    val selectedPackageIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AssignRouteViewModel(
    private val packageRepository: IPackageRepository = SupabasePackageRepository(SupabaseLocator.client),
    private val fleetRepository: IFleetRepository = SupabaseFleetRepository(SupabaseLocator.client)
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignRouteUiState())
    val uiState: StateFlow<AssignRouteUiState> = _uiState.asStateFlow()

    private var driverId: String? = null
    private var dataJob: Job? = null

    fun initialize(id: String) {
        if (this.driverId == id) return
        this.driverId = id
        loadData(id)
    }

    private fun loadData(id: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val truck = fleetRepository.trucks.value.find { it.driverId == id }
            val driverName = truck?.driverName ?: "Chofer"

            packageRepository.packages.collect { allPackages ->
                val inWarehouse = allPackages.filter { it.status == PackageStatus.EN_DEPOSITO }
                val assignedToDriver = allPackages.filter {
                    it.assignedDriverId == id && it.status != PackageStatus.ENTREGADO
                }

                _uiState.update { state ->
                    state.copy(
                        driverName = driverName,
                        availablePackages = inWarehouse,
                        currentRoutePackages = assignedToDriver,
                        selectedPackageIds = assignedToDriver.map { it.id }.toSet(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun togglePackageSelection(packageId: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedPackageIds.contains(packageId)) {
                state.selectedPackageIds - packageId
            } else {
                state.selectedPackageIds + packageId
            }
            state.copy(selectedPackageIds = newSelection)
        }
    }

    fun saveChanges() {
        val id = driverId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

            val currentState = _uiState.value
            val currentAssignedIds = currentState.currentRoutePackages.map { it.id }.toSet()
            val newSelectionIds = currentState.selectedPackageIds

            val toUnassign = currentAssignedIds - newSelectionIds
            var failedCount = 0

            toUnassign.forEach { pkgId ->
                val result = packageRepository.updateStatus(pkgId, PackageStatus.EN_DEPOSITO)
                if (result.isFailure) failedCount++
            }

            val toAssign = newSelectionIds - currentAssignedIds
            if (toAssign.isNotEmpty()) {
                val result = packageRepository.assignPackagesToDriver(toAssign.toList(), id)
                if (result.isFailure) failedCount += toAssign.size
            }

            if (failedCount > 0) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudieron guardar todos los cambios. Reintentá."
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
