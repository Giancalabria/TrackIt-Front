package com.trackit.feature.admin.globalmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Truck
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseFleetRepository
import com.trackit.data.repository.SupabaseLocator
import com.trackit.data.repository.SupabasePackageRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import com.trackit.data.model.PackageStatus

data class GlobalMapUiState(
    val deliveredCount: Int = 0,
    val pendingCount: Int = 0,
    val activeTrucks: Int = 0,
    val trucks: List<Truck> = emptyList(),
    val isLoading: Boolean = true
)

class GlobalMapViewModel(
    private val packageRepository: IPackageRepository = SupabasePackageRepository(SupabaseLocator.client),
    private val fleetRepository: IFleetRepository = SupabaseFleetRepository(SupabaseLocator.client)
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalMapUiState())
    val uiState: StateFlow<GlobalMapUiState> = _uiState.asStateFlow()

    init {
        observeGlobalMapState()
    }

    private fun observeGlobalMapState() {
        combine(
            packageRepository.packages,
            fleetRepository.trucks
        ) { packages, trucks ->
            val delivered = packages.count { it.status == PackageStatus.ENTREGADO }
            val pending = packages.count { it.status != PackageStatus.ENTREGADO }
            GlobalMapUiState(
                deliveredCount = delivered,
                pendingCount = pending,
                activeTrucks = trucks.size,
                trucks = trucks,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }
}
