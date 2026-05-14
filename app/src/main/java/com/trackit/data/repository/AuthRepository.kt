package com.trackit.data.repository

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun login(email: String, password: String): User? {
        if (password.isBlank()) {
            return null
        }

        val normalizedEmail = email.trim().lowercase()
        val user = when (normalizedEmail) {
            "chofer@trackit.com" -> User(
                email = normalizedEmail,
                displayName = "Juan Chofer",
                role = UserRole.DRIVER
            )

            "deposito@trackit.com" -> User(
                email = normalizedEmail,
                displayName = "María Depósito",
                role = UserRole.WAREHOUSE
            )

            "admin@trackit.com" -> User(
                email = normalizedEmail,
                displayName = "Ana Administradora",
                role = UserRole.ADMIN
            )

            else -> return null
        }

        _currentUser.value = user
        return user
    }

    fun logout() {
        _currentUser.value = null
    }
}
