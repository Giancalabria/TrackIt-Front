package com.trackit.feature.driver.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PackageDetailUiState(
    val packageItem: Package? = null,
    val isLoading: Boolean = true,
    val scanCompleted: Boolean = false
)

class PackageDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val packageId: String = checkNotNull(savedStateHandle["packageId"])

    private val _uiState = MutableStateFlow(PackageDetailUiState())
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    init {
        val packageItem = PackageRepository.getPackageById(packageId)
        _uiState.update {
            it.copy(
                packageItem = packageItem,
                isLoading = false
            )
        }
    }

    fun simulateScan() {
        PackageRepository.updateStatus(packageId, PackageStatus.DELIVERED)
        _uiState.update {
            it.copy(
                packageItem = PackageRepository.getPackageById(packageId),
                scanCompleted = true
            )
        }
    }
}
