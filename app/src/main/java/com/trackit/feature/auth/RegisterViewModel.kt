package com.trackit.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.SupabaseAuthRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: UserRole = UserRole.DRIVER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registeredUser: User? = null
)

class RegisterViewModel(
    private val authRepository: IAuthRepository = SupabaseAuthRepository(SupabaseLocator.client)
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onRoleChange(role: UserRole) {
        _uiState.update { it.copy(role = role, errorMessage = null) }
    }

    fun register() {
        val s = _uiState.value
        val email = s.email.trim()
        val displayName = s.displayName.trim()

        if (displayName.isBlank() || email.isBlank() || s.password.isBlank() || s.confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completá todos los campos.") }
            return
        }
        if (s.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }
        if (s.password != s.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val user = authRepository.register(
                email = email,
                password = s.password,
                displayName = displayName,
                role = s.role
            )
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No se pudo crear la cuenta.") }
            } else {
                _uiState.update { it.copy(isLoading = false, registeredUser = user) }
            }
        }
    }

    fun consumeRegisterSuccess() {
        _uiState.update { it.copy(registeredUser = null) }
    }
}

