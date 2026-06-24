package com.trackit.feature.driver.truck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.hasRouteStart
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.SupabaseLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverTruckSetupUiState(
    val driverName: String = "",
    val plate: String = "",
    val routeStartLabel: String = "",
    val routeStartLat: Double? = null,
    val routeStartLon: Double? = null,
    val locationError: String? = null,
    val isCheckingExisting: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val setupComplete: Boolean = false
)

class DriverTruckSetupViewModel(
    private val supabase: SupabaseClient = SupabaseLocator.client,
    private val fleetRepository: IFleetRepository = SupabaseLocator.fleetRepository,
    private val authRepository: IAuthRepository = SupabaseLocator.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverTruckSetupUiState())
    val uiState: StateFlow<DriverTruckSetupUiState> = _uiState.asStateFlow()

    init {
        checkExistingTruck()
    }

    fun onPlateChange(value: String) {
        _uiState.update { it.copy(plate = value, errorMessage = null) }
    }

    fun onRouteStartLabelChange(value: String) {
        _uiState.update { it.copy(routeStartLabel = value, errorMessage = null) }
    }

    fun onRouteStartSelected(lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                routeStartLat = lat,
                routeStartLon = lon,
                locationError = null,
                errorMessage = null
            )
        }
    }

    fun onLocationErrorChange(message: String?) {
        _uiState.update { it.copy(locationError = message) }
    }

    fun saveTruck() {
        val state = _uiState.value
        val plate = state.plate.trim()
        if (plate.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresá la patente del camión.") }
            return
        }
        if (state.routeStartLat == null || state.routeStartLon == null) {
            _uiState.update {
                it.copy(errorMessage = "Buscá una dirección o usá tu ubicación actual antes de continuar.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val authedUser = supabase.auth.currentUserOrNull()
            if (authedUser == null) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Sesión no válida. Volvé a iniciar sesión.")
                }
                return@launch
            }

            val driverName = resolveDriverName()
            val truck = fleetRepository.createTruck(
                driverId = authedUser.id,
                driverName = driverName,
                plate = plate,
                routeStartLat = state.routeStartLat,
                routeStartLon = state.routeStartLon,
                routeStartLabel = state.routeStartLabel.ifBlank { null }
            )

            if (truck == null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudo registrar el camión. Verificá permisos en Supabase."
                    )
                }
            } else {
                _uiState.update { it.copy(isSaving = false, setupComplete = true) }
            }
        }
    }

    fun consumeSetupComplete() {
        _uiState.update { it.copy(setupComplete = false) }
    }

    private fun checkExistingTruck() {
        viewModelScope.launch {
            try {
                val authedUser = supabase.auth.currentUserOrNull()
                if (authedUser == null) {
                    _uiState.update {
                        it.copy(
                            isCheckingExisting = false,
                            errorMessage = "Sesión no válida. Volvé a iniciar sesión."
                        )
                    }
                    return@launch
                }

                val driverName = resolveDriverName()
                val existing = fleetRepository.getTruckForDriver(authedUser.id)
                val isComplete = existing?.hasRouteStart() == true

                _uiState.update {
                    it.copy(
                        driverName = driverName,
                        plate = if (isComplete) it.plate else existing?.plate.orEmpty(),
                        routeStartLabel = if (isComplete) it.routeStartLabel else existing?.routeStartLabel.orEmpty(),
                        routeStartLat = if (isComplete) it.routeStartLat else existing?.routeStartLat,
                        routeStartLon = if (isComplete) it.routeStartLon else existing?.routeStartLon,
                        isCheckingExisting = false,
                        setupComplete = isComplete
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isCheckingExisting = false,
                        errorMessage = "No se pudo verificar tu camión. Reintentá en unos segundos."
                    )
                }
            }
        }
    }

    private suspend fun resolveDriverName(): String {
        val profileUser = authRepository.resolveUserFromSession()
        if (profileUser != null) return profileUser.displayName

        return supabase.auth.currentUserOrNull()
            ?.userMetadata
            ?.get("display_name")
            ?.toString()
            ?.removeSurrounding("\"")
            ?: "Chofer"
    }
}
