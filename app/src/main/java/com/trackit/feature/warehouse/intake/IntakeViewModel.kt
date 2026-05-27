package com.trackit.feature.warehouse.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.PackageSize
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class IntakeUiState(
    val clientName: String = "",
    val address: String = "",
    val size: PackageSize = PackageSize.MEDIUM,
    val isFragile: Boolean = false,
    val scheduledDate: LocalDate = LocalDate.now().plusDays(1),
    val barcode: String = "",
    val isSizeMenuExpanded: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isScannerOpen: Boolean = false
)

class IntakeViewModel(
    private val packageRepository: IPackageRepository = PackageRepository.getInstance()
) : ViewModel() {
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
    
    fun onScheduledDateChange(date: LocalDate) {
        _uiState.update { it.copy(scheduledDate = date) }
    }

    fun openScanner() {
        if (_uiState.value.clientName.isBlank() || _uiState.value.address.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completá cliente y dirección antes de escanear.") }
            return
        }
        _uiState.update { it.copy(isScannerOpen = true) }
    }

    fun closeScanner() {
        _uiState.update { it.copy(isScannerOpen = false) }
    }

    fun onBarcodeScanned(code: String) {
        _uiState.update { it.copy(barcode = code, isScannerOpen = false) }
        submitPackage()
    }

    private fun submitPackage() {
        val currentState = _uiState.value
        
        viewModelScope.launch {
            // En una implementación real, guardaríamos el barcode también
            packageRepository.addPackage(
                clientName = currentState.clientName.trim(),
                address = currentState.address.trim(),
                size = currentState.size,
                isFragile = currentState.isFragile,
                scheduledDate = currentState.scheduledDate
            )

            _uiState.update {
                it.copy(
                    clientName = "",
                    address = "",
                    barcode = "",
                    size = PackageSize.MEDIUM,
                    isFragile = false,
                    scheduledDate = LocalDate.now().plusDays(1),
                    successMessage = "Paquete registrado correctamente con código: ${currentState.barcode}",
                    errorMessage = null
                )
            }
        }
    }

    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
