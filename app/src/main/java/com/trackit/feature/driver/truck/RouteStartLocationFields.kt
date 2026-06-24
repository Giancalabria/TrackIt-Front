package com.trackit.feature.driver.truck

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trackit.core.location.readBestLastKnownLocation
import com.trackit.core.ui.components.AddressSearchField
import com.trackit.data.repository.IMapRepository
import com.trackit.data.repository.MapRepository
import kotlinx.coroutines.launch

@Composable
fun RouteStartLocationFields(
    label: String,
    onLabelChange: (String) -> Unit,
    selectedLat: Double?,
    selectedLon: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    enabled: Boolean,
    locationError: String?,
    onLocationErrorChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    mapRepository: IMapRepository = remember { MapRepository.getInstance() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCaptureAfterPermission by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isResolvingGpsAddress by remember { mutableStateOf(false) }

    fun applyReadableAddress(readableAddress: String) {
        searchQuery = readableAddress
        if (label.isBlank()) {
            onLabelChange(readableAddress)
        }
    }

    fun applyGpsLocation(lat: Double, lon: Double) {
        onLocationErrorChange(null)
        onLocationSelected(lat, lon)
        scope.launch {
            isResolvingGpsAddress = true
            try {
                val response = mapRepository.reverseGeocode(lat, lon)
                val readable = response.features.firstOrNull()?.properties?.getReadableAddress()
                if (!readable.isNullOrBlank()) {
                    applyReadableAddress(readable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onLocationErrorChange(
                    "Ubicación guardada, pero no pudimos obtener la dirección. Podés buscarla manualmente."
                )
            } finally {
                isResolvingGpsAddress = false
            }
        }
    }

    fun captureAndResolveCurrentLocation() {
        val location = readBestLastKnownLocation(context)
        if (location != null) {
            applyGpsLocation(location.first, location.second)
        } else {
            onLocationErrorChange("No pudimos obtener tu ubicación. Activá el GPS e intentá de nuevo.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it }
        if (granted && pendingCaptureAfterPermission) {
            captureAndResolveCurrentLocation()
        } else if (pendingCaptureAfterPermission) {
            onLocationErrorChange("Necesitamos permiso de ubicación para usar tu ubicación actual.")
        }
        pendingCaptureAfterPermission = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Text(
                text = "Ubicación inicial de ruta",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Obligatorio. Buscá un punto en el mapa o usá tu ubicación actual.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        AddressSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onPlaceSelected = { feature ->
                val coords = feature.geometry.asPoint()
                val lat = coords.getOrNull(1) ?: return@AddressSearchField
                val lon = coords.getOrNull(0) ?: return@AddressSearchField
                onLocationErrorChange(null)
                onLocationSelected(lat, lon)
                applyReadableAddress(feature.properties.getReadableAddress())
            },
            label = "Buscar dirección o lugar",
            enabled = enabled && !isResolvingGpsAddress
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "o",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = {
                val fineGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (fineGranted || coarseGranted) {
                    captureAndResolveCurrentLocation()
                } else {
                    pendingCaptureAfterPermission = true
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !isResolvingGpsAddress
        ) {
            if (isResolvingGpsAddress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Usar mi ubicación actual")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Etiqueta (opcional)") },
            placeholder = { Text("Ej: Mi casa, depósito norte") },
            singleLine = true,
            enabled = enabled && !isResolvingGpsAddress
        )

        locationError?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
