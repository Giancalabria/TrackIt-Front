package com.trackit.feature.admin.createuser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.UserRole
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateUserUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole = UserRole.DRIVER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdName: String? = null
)

class CreateUserViewModel(
    private val authRepository: IAuthRepository = SupabaseLocator.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUserUiState())
    val uiState: StateFlow<CreateUserUiState> = _uiState.asStateFlow()

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRoleChange(role: UserRole) {
        _uiState.update { it.copy(role = role, errorMessage = null) }
    }

    fun createUser() {
        val s = _uiState.value
        val email = s.email.trim()
        val displayName = s.displayName.trim()

        if (displayName.isBlank() || email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completá todos los campos.") }
            return
        }
        if (s.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.createUserAsAdmin(
                email = email,
                password = s.password,
                displayName = displayName,
                role = s.role
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        CreateUserUiState(createdName = displayName)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "No se pudo crear el usuario."
                        )
                    }
                }
            )
        }
    }

    fun consumeCreatedUser() {
        _uiState.update { it.copy(createdName = null) }
    }
}
