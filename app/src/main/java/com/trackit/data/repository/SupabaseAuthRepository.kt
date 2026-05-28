package com.trackit.data.repository

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
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

        try {
            supabase.auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = password
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val authedUser = supabase.auth.currentUserOrNull() ?: return null

        // Try to get data from profiles table
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
                role = when (profile.role.uppercase()) {
                    "DRIVER" -> UserRole.DRIVER
                    "WAREHOUSE" -> UserRole.WAREHOUSE
                    "ADMIN" -> UserRole.ADMIN
                    else -> null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Profile table fetch failed (likely RLS), we'll try metadata next
        }

        // Fallback to metadata if profile fetch failed or role is null
        if (role == null) {
            val metadata = authedUser.userMetadata
            displayName = metadata?.get("display_name")?.toString()?.removeSurrounding("\"") ?: "Usuario"
            val roleStr = metadata?.get("role")?.toString()?.removeSurrounding("\"")
            role = when (roleStr?.uppercase()) {
                "DRIVER" -> UserRole.DRIVER
                "WAREHOUSE" -> UserRole.WAREHOUSE
                "ADMIN" -> UserRole.ADMIN
                else -> UserRole.DRIVER // Default fallback
            }
        }

        val user = User(
            id = authedUser.id,
            email = trimmedEmail.lowercase(),
            displayName = displayName ?: "Usuario",
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

        try {
            supabase.auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = password
                // Store in metadata as fallback
                data = kotlinx.serialization.json.buildJsonObject {
                    put("display_name", kotlinx.serialization.json.JsonPrimitive(trimmedName))
                    put("role", kotlinx.serialization.json.JsonPrimitive(role.name))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Let ViewModel handle registration errors (e.g. user already exists)
        }

        val authedUser = supabase.auth.currentUserOrNull() ?: return null

        // Try to insert into profiles table, but don't fail if RLS prevents it
        try {
            supabase.from("profiles").insert(
                ProfileRow(
                    id = authedUser.id,
                    displayName = trimmedName,
                    role = role.name
                )
            )
        } catch (e: Exception) {
            // Log RLS or other database errors but allow registration to succeed
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

    override suspend fun logout() {
        supabase.auth.signOut()
        _currentUser.value = null
    }
}
