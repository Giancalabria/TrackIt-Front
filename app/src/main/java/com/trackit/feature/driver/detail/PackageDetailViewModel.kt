package com.trackit.feature.driver.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.matchesCode
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PackageDetailUiState(
    val packageItem: Package? = null,
    val isLoading: Boolean = true,
    val scanCompleted: Boolean = false,
    val isScannerOpen: Boolean = false,
    val errorMessage: String? = null
)

class PackageDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository
) : ViewModel() {
    private val packageId: String = checkNotNull(savedStateHandle["packageId"])

    private val _uiState = MutableStateFlow(PackageDetailUiState())
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    init {
        loadPackageDetail()
    }

    private fun loadPackageDetail() {
        viewModelScope.launch {
            val packageItem = packageRepository.getPackageById(packageId)
            _uiState.update {
                it.copy(
                    packageItem = packageItem,
                    isLoading = false,
                    errorMessage = if (packageItem == null) "Paquete no encontrado." else null
                )
            }
        }
    }

    fun openScanner() {
        _uiState.update { it.copy(isScannerOpen = true, errorMessage = null) }
    }

    fun closeScanner() {
        _uiState.update { it.copy(isScannerOpen = false) }
    }

    fun onCodeScanned(code: String) {
        val pkg = _uiState.value.packageItem ?: return

        // Validate the scanned/typed code against this package before changing state.
        if (!pkg.matchesCode(code)) {
            _uiState.update {
                it.copy(
                    isScannerOpen = false,
                    errorMessage = "El código no coincide con este paquete."
                )
            }
            return
        }

        viewModelScope.launch {
            val nextStatus = when (pkg.status) {
                PackageStatus.CARGADO -> PackageStatus.ENTREGADO
                PackageStatus.EN_CAMINO -> PackageStatus.ENTREGADO
                else -> pkg.status
            }

            if (nextStatus != pkg.status) {
                packageRepository.updateStatus(packageId, nextStatus)
                    .onSuccess {
                        _uiState.update {
                            it.copy(scanCompleted = true, isScannerOpen = false, errorMessage = null)
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                isScannerOpen = false,
                                errorMessage = "No se pudo actualizar el estado del paquete."
                            )
                        }
                    }
            } else {
                _uiState.update { it.copy(isScannerOpen = false) }
            }
        }
    }
}
