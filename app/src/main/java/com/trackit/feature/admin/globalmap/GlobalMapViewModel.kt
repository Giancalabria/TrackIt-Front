package com.trackit.feature.admin.globalmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Truck
import com.trackit.data.repository.FleetRepository
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GlobalMapUiState(
    val deliveredCount: Int = 0,
    val pendingCount: Int = 0,
    val activeTrucks: Int = 0,
    val trucks: List<Truck> = emptyList(),
    val isLoading: Boolean = true
)

class GlobalMapViewModel(
    private val packageRepository: IPackageRepository = PackageRepository.getInstance(),
    private val fleetRepository: IFleetRepository = FleetRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalMapUiState())
    val uiState: StateFlow<GlobalMapUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
        observeTrucks()
    }

    private fun observeTrucks() {
        fleetRepository.trucks
            .onEach { trucks ->
                _uiState.update { it.copy(trucks = trucks) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            val delivered = packageRepository.getDeliveredCount()
            val pending = packageRepository.getPendingCount()
            val trucks = fleetRepository.getActiveTruckCount()
            
            _uiState.update {
                it.copy(
                    deliveredCount = delivered,
                    pendingCount = pending,
                    activeTrucks = trucks,
                    isLoading = false
                )
            }
        }
    }
}
