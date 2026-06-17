package com.trackit.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trackit.data.model.User
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.SupabaseLocator

/**
 * Startup gate. Resolves any persisted Supabase session before deciding where to go:
 * an existing session lands on the role's home (skipping login), otherwise login.
 */
@Composable
fun SplashScreen(
    onResolved: (User?) -> Unit,
    authRepository: IAuthRepository = SupabaseLocator.authRepository
) {
    LaunchedEffect(Unit) {
        val user = runCatching { authRepository.resolveUserFromSession() }.getOrNull()
        onResolved(user)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
