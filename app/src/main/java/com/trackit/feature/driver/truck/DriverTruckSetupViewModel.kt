package com.trackit.feature.driver.truck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.SupabaseFleetRepository
import com.trackit.data.repository.SupabaseLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class DriverTruckSetupUiState(
    val driverName: String = "",
    val plate: String = "",
    val isCheckingExisting: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val setupComplete: Boolean = false
)

class DriverTruckSetupViewModel(
    private val supabase: SupabaseClient = SupabaseLocator.client,
    private val fleetRepository: IFleetRepository = SupabaseFleetRepository(SupabaseLocator.client)
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverTruckSetupUiState())
    val uiState: StateFlow<DriverTruckSetupUiState> = _uiState.asStateFlow()

    init {
        checkExistingTruck()
    }

    fun onPlateChange(value: String) {
        _uiState.update { it.copy(plate = value, errorMessage = null) }
    }

    fun saveTruck() {
        val plate = _uiState.value.plate.trim()
        if (plate.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresá la patente del camión.") }
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

            val driverName = resolveDriverName(authedUser.id)
            val truck = fleetRepository.createTruck(
                driverId = authedUser.id,
                driverName = driverName,
                plate = plate
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

            val driverName = resolveDriverName(authedUser.id)
            val existing = fleetRepository.getTruckForDriver(authedUser.id)

            _uiState.update {
                it.copy(
                    driverName = driverName,
                    isCheckingExisting = false,
                    setupComplete = existing != null
                )
            }
        }
    }

    private suspend fun resolveDriverName(userId: String): String {
        val authedUser = supabase.auth.currentUserOrNull() ?: return "Chofer"

        try {
            val profile = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<ProfileRow>()

            if (profile != null) return profile.displayName
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return authedUser.userMetadata
            ?.get("display_name")
            ?.toString()
            ?.removeSurrounding("\"")
            ?: "Chofer"
    }

    @Serializable
    private data class ProfileRow(
        val id: String,
        @SerialName("display_name") val displayName: String,
        val role: String
    )
}
