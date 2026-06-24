package com.trackit.feature.driver.truck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DriverTruckSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: DriverTruckSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.setupComplete, uiState.isCheckingExisting) {
        if (!uiState.isCheckingExisting && uiState.setupComplete && !hasNavigated) {
            hasNavigated = true
            onSetupComplete()
            viewModel.consumeSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isCheckingExisting) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Verificando tu camión…")
        } else {
            Text(
                text = "Registrar tu camión",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hola, ${uiState.driverName}. Completá los datos de tu vehículo y tu punto de salida para aparecer en la flota.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.plate,
                onValueChange = viewModel::onPlateChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Patente") },
                placeholder = { Text("Ej: AB123CD") },
                singleLine = true,
                enabled = !uiState.isSaving
            )

            Spacer(modifier = Modifier.height(24.dp))

            RouteStartLocationFields(
                label = uiState.routeStartLabel,
                onLabelChange = viewModel::onRouteStartLabelChange,
                selectedLat = uiState.routeStartLat,
                selectedLon = uiState.routeStartLon,
                onLocationSelected = viewModel::onRouteStartSelected,
                enabled = !uiState.isSaving,
                locationError = uiState.locationError,
                onLocationErrorChange = viewModel::onLocationErrorChange
            )

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::saveTruck,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar y continuar")
                }
            }
        }
    }
}
