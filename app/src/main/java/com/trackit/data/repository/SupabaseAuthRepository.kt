package com.trackit.data.repository

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAuthRepository(
    private val supabase: SupabaseClient
) : IAuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    @Serializable
    private data class ProfileRow(
        val id: String,
        @SerialName("display_name") val displayName: String,
        val role: String
    )

    override suspend fun login(email: String, password: String): User? {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) return null

        supabase.auth.signInWith(Email) {
            this.email = trimmedEmail
            this.password = password
        }

        val authedUser = supabase.auth.currentUserOrNull() ?: return null

        // RLS ensures non-admin users will only see their own profile row.
        val profiles = supabase.from("profiles").select().decodeList<ProfileRow>()
        val profile = profiles.firstOrNull { it.id == authedUser.id } ?: return null

        val role = when (profile.role.uppercase()) {
            "DRIVER" -> UserRole.DRIVER
            "WAREHOUSE" -> UserRole.WAREHOUSE
            "ADMIN" -> UserRole.ADMIN
            else -> return null
        }

        val user = User(
            id = authedUser.id,
            email = trimmedEmail.lowercase(),
            displayName = profile.displayName,
            role = role
        )
        _currentUser.value = user
        return user
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): User? {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()
        if (trimmedEmail.isBlank() || password.isBlank() || trimmedName.isBlank()) return null

        supabase.auth.signUpWith(Email) {
            this.email = trimmedEmail
            this.password = password
        }

        val authedUser = supabase.auth.currentUserOrNull() ?: return null

        // Insert profile row for the new user.
        supabase.from("profiles").insert(
            mapOf(
                "id" to authedUser.id,
                "display_name" to trimmedName,
                "role" to role.name
            )
        )

        val user = User(
            id = authedUser.id,
            email = trimmedEmail.lowercase(),
            displayName = trimmedName,
            role = role
        )
        _currentUser.value = user
        return user
    }

    override suspend fun logout() {
        supabase.auth.signOut()
        _currentUser.value = null
    }
}

