package com.trackit.fakes

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import com.trackit.data.repository.IAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository : IAuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    var loginResult: User? = null

    override suspend fun login(email: String, password: String): User? = loginResult

    override suspend fun createUserAsAdmin(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun resolveUserFromSession(): User? = _currentUser.value

    override suspend fun logout() {
        _currentUser.value = null
    }
}
