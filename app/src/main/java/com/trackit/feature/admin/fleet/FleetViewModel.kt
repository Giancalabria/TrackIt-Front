package com.trackit.feature.admin.fleet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.Truck
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private val ARGENTINA_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

data class FleetUiState(
    val trucks: List<Truck> = emptyList(),
    val isCronJobRunning: Boolean = false,
    val cronJobSuccess: Boolean = false,
    val cronResultMessage: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class FleetViewModel(
    private val fleetRepository: IFleetRepository = SupabaseLocator.fleetRepository,
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FleetUiState())
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    init {
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
            _uiState.update {
                it.copy(
                    isCronJobRunning = true,
                    errorMessage = null,
                    cronJobSuccess = false,
                    cronResultMessage = null
                )
            }

            // Use the same date the cron uses (today in Argentina time) so the manual
            // "Generar rutas del día" button stays aligned with the scheduled job.
            val today = LocalDate.now(ARGENTINA_ZONE)
            packageRepository.triggerRouteOptimization(today)
                .onSuccess { result ->
                    val message = when {
                        !result.ok && result.error != null ->
                            "No se pudo optimizar: ${result.error}"
                        result.reason == "no_jobs" ->
                            "No hay paquetes en depósito para el ${result.targetDate}."
                        result.reason == "no_vehicles" || result.error == "no_vehicles_with_route_start" ->
                            "No hay choferes con camión y ubicación inicial configurada."
                        else ->
                            "Rutas generadas: ${result.assigned} asignados, ${result.unassigned} sin asignar."
                    }
                    _uiState.update {
                        it.copy(
                            isCronJobRunning = false,
                            cronJobSuccess = result.ok,
                            cronResultMessage = message
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isCronJobRunning = false,
                            errorMessage = "No se pudo ejecutar la optimización de rutas."
                        )
                    }
                }
        }
    }

    fun consumeCronJobSuccess() {
        _uiState.update { it.copy(cronJobSuccess = false) }
    }

    fun consumeCronResultMessage() {
        _uiState.update { it.copy(cronResultMessage = null) }
    }
}
