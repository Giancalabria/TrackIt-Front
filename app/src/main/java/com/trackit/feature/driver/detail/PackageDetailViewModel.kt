package com.trackit.feature.driver.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import com.trackit.data.repository.SupabasePackageRepository
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
    private val packageRepository: IPackageRepository = SupabasePackageRepository(SupabaseLocator.client)
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
        val currentStatus = _uiState.value.packageItem?.status ?: return

        viewModelScope.launch {
            val nextStatus = when (currentStatus) {
                PackageStatus.CARGADO -> PackageStatus.ENTREGADO
                PackageStatus.EN_CAMINO -> PackageStatus.ENTREGADO
                else -> currentStatus
            }

            if (nextStatus != currentStatus) {
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
