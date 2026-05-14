package com.trackit.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.User
import com.trackit.data.repository.AuthRepository
import com.trackit.data.repository.IAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: User? = null
)

class LoginViewModel(
    private val authRepository: IAuthRepository = AuthRepository.getInstance()
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresá email y contraseña.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val user = authRepository.login(currentState.email, currentState.password)
            if (user == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Credenciales inválidas. Probá con chofer@trackit.com, deposito@trackit.com o admin@trackit.com."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loggedInUser = user
                    )
                }
            }
        }
    }

    fun consumeLoginSuccess() {
        _uiState.update { it.copy(loggedInUser = null) }
    }
}
