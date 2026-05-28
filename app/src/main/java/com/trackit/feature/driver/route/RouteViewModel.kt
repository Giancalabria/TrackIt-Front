package com.trackit.feature.driver.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.AuthRepository
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RouteUiState(
    val packages: List<Package> = emptyList(),
    val isLoading: Boolean = true
)

class RouteViewModel : ViewModel() {

    private val packageRepository: IPackageRepository = PackageRepository.getInstance()
    private val authRepository: IAuthRepository = AuthRepository.getInstance()

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    init {
        observePackages()
    }

    private fun observePackages() {
        combine(
            packageRepository.packages,
            authRepository.currentUser
        ) { allPackages, currentUser ->
            if (currentUser == null) {
                _uiState.update { it.copy(isLoading = false, packages = emptyList()) }
                return@combine
            }
            
            val driverPackages = allPackages
                .filter { it.assignedDriverId == currentUser.id }
                .sortedWith(compareBy<Package> { 
                    when (it.status) {
                        PackageStatus.EN_CAMINO -> 0
                        PackageStatus.CARGADO -> 1
                        PackageStatus.ASIGNADO -> 2
                        PackageStatus.ENTREGADO -> 3
                        else -> 4
                    }
                }.thenBy { it.eta })
            
            _uiState.update { state ->
                state.copy(
                    packages = driverPackages,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun deliverPackage(packageId: String, code: String) {
        viewModelScope.launch {
            packageRepository.updateStatus(packageId, PackageStatus.ENTREGADO)
        }
    }
}
