package com.trackit.data.repository

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): User?
    suspend fun register(email: String, password: String, displayName: String, role: UserRole): User?
    suspend fun resolveUserFromSession(): User?
    suspend fun logout()
}
