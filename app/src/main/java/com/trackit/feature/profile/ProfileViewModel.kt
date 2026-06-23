package com.trackit.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.core.ui.theme.ThemeLocator
import com.trackit.core.ui.theme.ThemeMode
import com.trackit.core.ui.theme.ThemePreferences
import com.trackit.data.model.User
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true
)

class ProfileViewModel(
    private val authRepository: IAuthRepository = SupabaseLocator.authRepository,
    private val themePreferences: ThemePreferences = ThemeLocator.preferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        viewModelScope.launch {
            themePreferences.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        themeMode = settings.mode,
                        useDynamicColor = settings.useDynamicColor
                    )
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.resolveUserFromSession()
            _uiState.update { it.copy(user = user, isLoading = false) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDynamicColor(enabled)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
