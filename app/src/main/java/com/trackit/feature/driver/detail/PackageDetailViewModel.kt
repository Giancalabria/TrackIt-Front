package com.trackit.feature.driver.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PackageDetailUiState(
    val packageItem: Package? = null,
    val isLoading: Boolean = true,
    val scanCompleted: Boolean = false,
    val isScannerOpen: Boolean = false
)

class PackageDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val packageId: String = checkNotNull(savedStateHandle["packageId"])
    private val packageRepository: IPackageRepository = PackageRepository.getInstance()

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
                    isLoading = false
                )
            }
        }
    }

    fun openScanner() {
        _uiState.update { it.copy(isScannerOpen = true) }
    }

    fun closeScanner() {
        _uiState.update { it.copy(isScannerOpen = false) }
    }

    fun onCodeScanned(code: String) {
        val currentStatus = _uiState.value.packageItem?.status ?: return
        
        viewModelScope.launch {
            val nextStatus = when (currentStatus) {
                PackageStatus.ASIGNADO -> PackageStatus.CARGADO
                PackageStatus.EN_CAMINO -> PackageStatus.ENTREGADO
                else -> currentStatus
            }
            
            if (nextStatus != currentStatus) {
                packageRepository.updateStatus(packageId, nextStatus)
                _uiState.update { it.copy(scanCompleted = true, isScannerOpen = false) }
            } else {
                _uiState.update { it.copy(isScannerOpen = false) }
            }
        }
    }
}
