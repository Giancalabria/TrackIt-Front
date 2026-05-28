package com.trackit.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.SupabaseAuthRepository
import com.trackit.data.repository.SupabaseLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true
)

class ProfileViewModel(
    private val supabase: SupabaseClient = SupabaseLocator.client,
    private val authRepository: IAuthRepository = SupabaseAuthRepository(SupabaseLocator.client)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true)
            val user = resolveCurrentUser()
            _uiState.value = ProfileUiState(user = user, isLoading = false)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    private suspend fun resolveCurrentUser(): User? {
        val authedUser = supabase.auth.currentUserOrNull() ?: return null

        var displayName: String? = null
        var role: UserRole? = null

        try {
            val profile = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", authedUser.id)
                    }
                }
                .decodeSingleOrNull<ProfileRow>()

            if (profile != null) {
                displayName = profile.displayName
                role = parseRole(profile.role)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (role == null) {
            val metadata = authedUser.userMetadata
            displayName = metadata?.get("display_name")?.toString()?.removeSurrounding("\"") ?: "Usuario"
            val roleStr = metadata?.get("role")?.toString()?.removeSurrounding("\"")
            role = parseRole(roleStr) ?: UserRole.DRIVER
        }

        val email = authedUser.email?.trim()?.lowercase().orEmpty()

        return User(
            id = authedUser.id,
            email = email,
            displayName = displayName ?: "Usuario",
            role = role ?: UserRole.DRIVER
        )
    }

    private fun parseRole(roleStr: String?): UserRole? = when (roleStr?.uppercase()) {
        "DRIVER" -> UserRole.DRIVER
        "WAREHOUSE" -> UserRole.WAREHOUSE
        "ADMIN" -> UserRole.ADMIN
        else -> null
    }

    @Serializable
    private data class ProfileRow(
        val id: String,
        @SerialName("display_name") val displayName: String,
        val role: String
    )
}
