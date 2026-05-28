package com.trackit.data.repository

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository : IAuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): User? {
        if (password.isBlank()) {
            return null
        }

        val normalizedEmail = email.trim().lowercase()
        val user = when (normalizedEmail) {
            "chofer@trackit.com" -> User(
                id = "USR-001",
                email = normalizedEmail,
                displayName = "Juan Chofer",
                role = UserRole.DRIVER
            )

            "deposito@trackit.com" -> User(
                id = "USR-002",
                email = normalizedEmail,
                displayName = "María Depósito",
                role = UserRole.WAREHOUSE
            )

            "admin@trackit.com" -> User(
                id = "USR-003",
                email = normalizedEmail,
                displayName = "Ana Administradora",
                role = UserRole.ADMIN
            )

            else -> return null
        }

        _currentUser.value = user
        return user
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    companion object {
        // Singleton pattern for manual DI
        private var instance: AuthRepository? = null
        fun getInstance(): AuthRepository {
            if (instance == null) {
                instance = AuthRepository()
            }
            return instance!!
        }
    }
}
