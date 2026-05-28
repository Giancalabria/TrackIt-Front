package com.trackit.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.User
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.SupabaseAuthRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true
)

class ProfileViewModel(
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
            val user = authRepository.resolveUserFromSession()
            _uiState.value = ProfileUiState(user = user, isLoading = false)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
