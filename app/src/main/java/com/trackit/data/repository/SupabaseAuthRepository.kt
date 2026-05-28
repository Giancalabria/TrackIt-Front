package com.trackit.data.repository

import com.trackit.data.model.ProfileRow
import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import com.trackit.data.model.toUserRoleOrNull
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class SupabaseAuthRepository(
    private val supabase: SupabaseClient
) : IAuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): User? {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) return null

        try {
            supabase.auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = password
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val user = resolveUserFromSession(trimmedEmail.lowercase()) ?: return null
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

        try {
            supabase.auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = password
                data = buildJsonObject {
                    put("display_name", JsonPrimitive(trimmedName))
                    put("role", JsonPrimitive(role.name))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        val authedUser = supabase.auth.currentUserOrNull() ?: return null

        try {
            supabase.from("profiles").insert(
                ProfileRow(
                    id = authedUser.id,
                    displayName = trimmedName,
                    role = role.name
                )
            )
        } catch (e: Exception) {
            println("Warning: Could not insert into profiles table: ${e.message}")
            e.printStackTrace()
        }

        val user = User(
            id = authedUser.id,
            email = trimmedEmail.lowercase(),
            displayName = trimmedName,
            role = role
        )
        _currentUser.value = user
        return user
    }

    override suspend fun resolveUserFromSession(): User? {
        val authedUser = supabase.auth.currentUserOrNull() ?: return null
        val email = authedUser.email?.trim()?.lowercase().orEmpty()
        return resolveUserFromSession(email)
    }

    private suspend fun resolveUserFromSession(fallbackEmail: String): User? {
        val authedUser = supabase.auth.currentUserOrNull() ?: return null

        var displayName: String? = null
        var role: UserRole? = null

        try {
            val profile = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", authedUser.id)
                    }
                }
                .decodeSingleOrNull<ProfileRow>()

            if (profile != null) {
                displayName = profile.displayName
                role = profile.role.toUserRoleOrNull()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (role == null) {
            println("TrackIt: profiles table unavailable or empty; using auth metadata fallback for user ${authedUser.id}")
            val metadata = authedUser.userMetadata
            displayName = metadata?.get("display_name")?.toString()?.removeSurrounding("\"") ?: "Usuario"
            val roleStr = metadata?.get("role")?.toString()?.removeSurrounding("\"")
            role = roleStr.toUserRoleOrNull() ?: UserRole.DRIVER
        }

        return User(
            id = authedUser.id,
            email = fallbackEmail.ifBlank { authedUser.email?.trim()?.lowercase().orEmpty() },
            displayName = displayName ?: "Usuario",
            role = role ?: UserRole.DRIVER
        )
    }

    override suspend fun logout() {
        supabase.auth.signOut()
        _currentUser.value = null
    }
}
