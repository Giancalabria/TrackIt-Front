package com.trackit.feature.warehouse.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.core.AppConfig
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PhotonFeature
import com.trackit.data.repository.IMapRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.MapRepository
import com.trackit.data.repository.SupabaseLocator
import com.trackit.data.repository.SupabasePackageRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class IntakeUiState(
    val clientName: String = "",
    val address: String = "",
    val destinationLat: Double? = null,
    val destinationLon: Double? = null,
    val addressSearchResults: List<PhotonFeature> = emptyList(),
    val isSearchingAddress: Boolean = false,
    val size: PackageSize = PackageSize.MEDIUM,
    val isFragile: Boolean = false,
    val scheduledDate: LocalDate = LocalDate.now().plusDays(1),
    val barcode: String = "",
    val isSizeMenuExpanded: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isScannerOpen: Boolean = false,
    val isSubmitting: Boolean = false
)

@OptIn(FlowPreview::class)
class IntakeViewModel(
    private val packageRepository: IPackageRepository = SupabasePackageRepository(SupabaseLocator.client),
    private val mapRepository: IMapRepository = MapRepository.getInstance()
) : ViewModel() {
    private val _uiState = MutableStateFlow(IntakeUiState())
    val uiState: StateFlow<IntakeUiState> = _uiState.asStateFlow()

    private val _addressQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _addressQuery
                .debounce(500L)
                .filter { it.isNotBlank() && it.length > 3 }
                .distinctUntilChanged()
                .collect { query ->
                    performAddressSearch(query)
                }
        }
    }

    fun onClientNameChange(value: String) {
        _uiState.update { it.copy(clientName = value, errorMessage = null) }
    }

    fun onAddressChange(value: String) {
        _addressQuery.value = value
        _uiState.update { it.copy(address = value, errorMessage = null) }
        if (value.isBlank()) {
            _uiState.update { it.copy(addressSearchResults = emptyList()) }
        }
    }

    private suspend fun performAddressSearch(query: String) {
        _uiState.update { it.copy(isSearchingAddress = true, errorMessage = null) }
        try {
            val response = mapRepository.searchAddress(
                query = query,
                lat = AppConfig.DEFAULT_GEOCODER_LAT,
                lon = AppConfig.DEFAULT_GEOCODER_LON
            )
            _uiState.update {
                it.copy(addressSearchResults = response.features, isSearchingAddress = false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(
                    isSearchingAddress = false,
                    errorMessage = "No se pudo buscar la dirección. Verificá tu conexión."
                )
            }
        }
    }

    fun onAddressSelected(feature: PhotonFeature) {
        val coords = feature.geometry.asPoint() // [lon, lat]
        val fullAddress = feature.properties.getDisplayName()
        _uiState.update {
            it.copy(
                address = fullAddress,
                destinationLat = coords.getOrNull(1),
                destinationLon = coords.getOrNull(0),
                addressSearchResults = emptyList()
            )
        }
        _addressQuery.value = "" // Stop searching
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
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            packageRepository.addPackage(
                clientName = currentState.clientName.trim(),
                address = currentState.address.trim(),
                destinationLat = currentState.destinationLat,
                destinationLon = currentState.destinationLon,
                size = currentState.size,
                isFragile = currentState.isFragile,
                scheduledDate = currentState.scheduledDate
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            clientName = "",
                            address = "",
                            destinationLat = null,
                            destinationLon = null,
                            addressSearchResults = emptyList(),
                            barcode = "",
                            size = PackageSize.MEDIUM,
                            isFragile = false,
                            scheduledDate = LocalDate.now().plusDays(1),
                            successMessage = "Paquete registrado correctamente con código: ${currentState.barcode}",
                            errorMessage = null,
                            isSubmitting = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "No se pudo registrar el paquete. Reintentá."
                        )
                    }
                }
            )
        }
    }

    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
