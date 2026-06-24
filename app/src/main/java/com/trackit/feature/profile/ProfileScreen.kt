package com.trackit.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.data.model.UserRole

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onCreateUser: (() -> Unit)? = null,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = user?.displayName ?: "Usuario",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user?.role?.toLabel() ?: "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        ThemeAppearanceSection(
            themeMode = uiState.themeMode,
            useDynamicColor = uiState.useDynamicColor,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor
        )

        if (user?.role == UserRole.DRIVER) {
            Spacer(modifier = Modifier.height(32.dp))
            DriverRouteStartSection(
                truck = uiState.driverTruck,
                isSaving = uiState.isSavingRouteStart,
                successMessage = uiState.routeStartMessage,
                errorMessage = uiState.routeStartError,
                onSaveLocation = viewModel::saveRouteStartLocation,
                onClearMessages = viewModel::clearRouteStartMessages
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (onCreateUser != null) {
            OutlinedButton(
                onClick = onCreateUser,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Crear usuario")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { viewModel.logout(onLoggedOut = onLogout) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text("Cerrar sesión")
        }
    }
}

private fun UserRole.toLabel(): String = when (this) {
    UserRole.DRIVER -> "Chofer"
    UserRole.WAREHOUSE -> "Empleado de depósito"
    UserRole.ADMIN -> "Administrador"
}
