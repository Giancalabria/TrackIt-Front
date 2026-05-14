package com.trackit.data.repository

import com.trackit.data.model.User
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): User?
    suspend fun logout()
}
