package com.trackit.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackit.data.model.Truck
import com.trackit.data.model.hasRouteStart
import com.trackit.feature.driver.truck.RouteStartLocationFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRouteStartSection(
    truck: Truck?,
    isSaving: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onSaveLocation: (lat: Double, lon: Double, label: String?) -> Unit,
    onClearMessages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var saveRequested by remember { mutableStateOf(false) }

    var draftLabel by remember(truck?.id, truck?.routeStartLabel) {
        mutableStateOf(truck?.routeStartLabel.orEmpty())
    }
    var draftLat by remember(truck?.id, truck?.routeStartLat) { mutableStateOf(truck?.routeStartLat) }
    var draftLon by remember(truck?.id, truck?.routeStartLon) { mutableStateOf(truck?.routeStartLon) }
    var locationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isSaving, saveRequested, successMessage, errorMessage) {
        if (saveRequested && !isSaving) {
            saveRequested = false
            if (errorMessage == null && successMessage != null) {
                showEditSheet = false
            }
        }
    }

    fun resetDraftFromTruck() {
        draftLabel = truck?.routeStartLabel.orEmpty()
        draftLat = truck?.routeStartLat
        draftLon = truck?.routeStartLon
        locationError = null
    }

    fun openEditSheet() {
        onClearMessages()
        resetDraftFromTruck()
        showEditSheet = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Ubicación inicial de ruta",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (truck == null || !truck.hasRouteStart()) {
            Text(
                text = "Completá el registro de tu camión para configurar la ubicación inicial.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val lat = truck.routeStartLat!!
        val lon = truck.routeStartLon!!
        val label = truck.routeStartLabel?.takeIf { it.isNotBlank() }

        Text(
            text = label ?: formatRouteStartCoords(lat, lon),
            style = MaterialTheme.typography.bodyLarge
        )
        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatRouteStartCoords(lat, lon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = ::openEditSheet,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            Text("Editar ubicación")
        }

        successMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showEditSheet && truck != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                if (!isSaving) {
                    showEditSheet = false
                    resetDraftFromTruck()
                }
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Editar ubicación inicial",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Buscá un punto o usá tu ubicación actual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                RouteStartLocationFields(
                    label = draftLabel,
                    onLabelChange = { draftLabel = it },
                    selectedLat = draftLat,
                    selectedLon = draftLon,
                    onLocationSelected = { lat, lon ->
                        draftLat = lat
                        draftLon = lon
                    },
                    enabled = !isSaving,
                    locationError = locationError ?: errorMessage,
                    onLocationErrorChange = { locationError = it },
                    showHeader = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showEditSheet = false
                            resetDraftFromTruck()
                            onClearMessages()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val saveLat = draftLat
                            val saveLon = draftLon
                            if (saveLat == null || saveLon == null) {
                                locationError = "Buscá una dirección o usá tu ubicación actual antes de guardar."
                                return@Button
                            }
                            locationError = null
                            saveRequested = true
                            onSaveLocation(saveLat, saveLon, draftLabel.ifBlank { null })
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }
    }
}

private fun formatRouteStartCoords(lat: Double, lon: Double): String =
    String.format(Locale.getDefault(), "%.5f, %.5f", lat, lon)
