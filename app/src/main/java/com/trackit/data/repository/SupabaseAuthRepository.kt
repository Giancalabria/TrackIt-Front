package com.trackit.data.repository

import com.trackit.data.model.ProfileRow
import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import com.trackit.data.model.toUserRoleOrNull
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
private data class AdminCreateUserResponse(
    val ok: Boolean = false,
    val error: String? = null
)

private val adminCreateUserJson = Json { ignoreUnknownKeys = true }

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

    override suspend fun createUserAsAdmin(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<Unit> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = displayName.trim()
        if (trimmedEmail.isBlank() || password.isBlank() || trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Completá todos los campos."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres."))
        }

        return try {
            // The SDK forwards the logged-in admin's JWT automatically and never touches
            // the admin's session (the new user is created server-side with service role).
            val response = supabase.functions.invoke(
                "admin-create-user",
                buildJsonObject {
                    put("email", JsonPrimitive(trimmedEmail))
                    put("password", JsonPrimitive(password))
                    put("displayName", JsonPrimitive(trimmedName))
                    put("role", JsonPrimitive(role.name))
                }
            )
            val raw = response.bodyAsText()
            val parsed = runCatching {
                adminCreateUserJson.decodeFromString<AdminCreateUserResponse>(raw)
            }.getOrNull()

            if (parsed?.ok == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(mapCreateUserError(parsed?.error ?: raw)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(mapCreateUserError(e.message)))
        }
    }

    private fun mapCreateUserError(error: String?): String {
        val code = error.orEmpty()
        return when {
            code.contains("forbidden_not_admin") -> "No tenés permisos de administrador para crear usuarios."
            code.contains("missing_auth") || code.contains("invalid_token") ->
                "Tu sesión expiró. Cerrá sesión y volvé a ingresar."
            code.contains("weak_password") -> "La contraseña debe tener al menos 6 caracteres."
            code.contains("invalid_role") -> "El rol seleccionado no es válido."
            code.contains("missing_email_or_password") -> "Completá email y contraseña."
            code.contains("already been registered", ignoreCase = true) ||
                code.contains("already registered", ignoreCase = true) ||
                code.contains("already exists", ignoreCase = true) -> "Ese email ya está registrado."
            code.contains("network", ignoreCase = true) -> "Error de conexión. Verificá tu internet."
            code.isBlank() -> "No se pudo crear el usuario."
            else -> "No se pudo crear el usuario: $code"
        }
    }

    override suspend fun resolveUserFromSession(): User? {
        // Wait until the SDK finishes loading any persisted session from storage,
        // so a returning user is recognized without re-logging in. Guarded with a
        // timeout so the splash never hangs if initialization stalls.
        withTimeoutOrNull(5_000) { supabase.auth.awaitInitialization() }
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
            role = roleStr?.toUserRoleOrNull() ?: UserRole.DRIVER
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
