package com.trackit.feature.warehouse.intake

import androidx.lifecycle.ViewModel
import com.trackit.data.model.PackageSize
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class IntakeUiState(
    val clientName: String = "",
    val address: String = "",
    val size: PackageSize = PackageSize.MEDIUM,
    val isFragile: Boolean = false,
    val isSizeMenuExpanded: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class IntakeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(IntakeUiState())
    val uiState: StateFlow<IntakeUiState> = _uiState.asStateFlow()

    fun onClientNameChange(value: String) {
        _uiState.update { it.copy(clientName = value, errorMessage = null) }
    }

    fun onAddressChange(value: String) {
        _uiState.update { it.copy(address = value, errorMessage = null) }
    }

    fun onSizeSelected(size: PackageSize) {
        _uiState.update { it.copy(size = size, isSizeMenuExpanded = false) }
    }

    fun onSizeMenuExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isSizeMenuExpanded = expanded) }
    }

    fun onFragileChange(value: Boolean) {
        _uiState.update { it.copy(isFragile = value) }
    }

    fun submitPackage() {
        val currentState = _uiState.value
        if (currentState.clientName.isBlank() || currentState.address.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completá cliente y dirección.") }
            return
        }

        PackageRepository.addPackage(
            clientName = currentState.clientName.trim(),
            address = currentState.address.trim(),
            size = currentState.size,
            isFragile = currentState.isFragile
        )

        _uiState.update {
            it.copy(
                clientName = "",
                address = "",
                size = PackageSize.MEDIUM,
                isFragile = false,
                successMessage = "Paquete registrado correctamente.",
                errorMessage = null
            )
        }
    }

    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
