package com.trackit.feature.driver.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import com.trackit.feature.driver.detail.matchesCode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RouteUiState(
    val packages: List<Package> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class RouteViewModel(
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository,
    private val authRepository: IAuthRepository = SupabaseLocator.authRepository
) : ViewModel() {

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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packages = emptyList(),
                        errorMessage = "Sesión no válida. Volvé a iniciar sesión."
                    )
                }
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
                    isLoading = false,
                    errorMessage = null
                )
            }
        }.launchIn(viewModelScope)
    }

    fun deliverPackage(packageId: String, scannedCode: String) {
        val pkg = _uiState.value.packages.firstOrNull { it.id == packageId } ?: return

        // The scanned/typed code must match the package before marking it delivered.
        if (!pkg.matchesCode(scannedCode)) {
            _uiState.update {
                it.copy(errorMessage = "El código no coincide con el paquete a entregar.")
            }
            return
        }

        viewModelScope.launch {
            packageRepository.updateStatus(packageId, PackageStatus.ENTREGADO)
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo marcar el paquete como entregado.")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
