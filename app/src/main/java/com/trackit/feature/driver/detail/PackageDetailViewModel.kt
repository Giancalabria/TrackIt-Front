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
    val scanCompleted: Boolean = false
)

class PackageDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val packageId: String = checkNotNull(savedStateHandle["packageId"])
    private val packageRepository: IPackageRepository = PackageRepository.getInstance()

    private val _uiState = MutableStateFlow(PackageDetailUiState())
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    init {
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

    fun simulateScan() {
        viewModelScope.launch {
            packageRepository.updateStatus(packageId, PackageStatus.ENTREGADO)
            val updatedPackage = packageRepository.getPackageById(packageId)
            _uiState.update {
                it.copy(
                    packageItem = updatedPackage,
                    scanCompleted = true
                )
            }
        }
    }
}
