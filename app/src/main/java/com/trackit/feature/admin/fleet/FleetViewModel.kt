package com.trackit.feature.admin.fleet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.Truck
import com.trackit.data.repository.FleetRepository
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FleetUiState(
    val trucks: List<Truck> = emptyList(),
    val isCronJobRunning: Boolean = false,
    val cronJobSuccess: Boolean = false,
    val isLoading: Boolean = true
)

class FleetViewModel(
    private val fleetRepository: IFleetRepository = FleetRepository.getInstance(),
    private val packageRepository: IPackageRepository = PackageRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FleetUiState())
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    init {
        // Combinamos la lista de camiones con la lista de paquetes para calcular estadísticas reales
        combine(
            fleetRepository.trucks,
            packageRepository.packages
        ) { trucks, allPackages ->
            val updatedTrucks = trucks.map { truck ->
                val driverPackages = allPackages.filter { it.assignedDriverId == truck.driverId }
                truck.copy(
                    deliveredCount = driverPackages.count { it.status == PackageStatus.ENTREGADO },
                    totalCount = driverPackages.size
                )
            }
            _uiState.update { it.copy(trucks = updatedTrucks, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun runDailyCronJob() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCronJobRunning = true) }
            val driverIds = _uiState.value.trucks.map { it.driverId }
            packageRepository.simulateDailyCronJob(LocalDate.now(), driverIds)
            _uiState.update { it.copy(isCronJobRunning = false, cronJobSuccess = true) }
        }
    }

    fun consumeCronJobSuccess() {
        _uiState.update { it.copy(cronJobSuccess = false) }
    }
}
