package com.trackit.data.repository

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): User?

    /**
     * Creates a new user (any role) through the `admin-create-user` Edge Function.
     * Only callable by an authenticated ADMIN; the function validates the caller's role
     * server-side. Does NOT change the currently logged-in admin's session.
     */
    suspend fun createUserAsAdmin(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<Unit>

    suspend fun resolveUserFromSession(): User?
    suspend fun logout()
}
