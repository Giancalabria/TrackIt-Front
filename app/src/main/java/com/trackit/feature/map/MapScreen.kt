package com.trackit.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.BuildConfig
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Initialize Osmdroid Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    val mapView = remember { MapView(context) }
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Mapa Base (Osmdroid)
        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    
                    // Añadir capa de ubicación del usuario
                    overlays.add(myLocationOverlay)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                // Limpiar solo rutas, manteniendo el overlay de ubicación
                val overlaysToRemove = mv.overlays.filterIsInstance<Polyline>()
                mv.overlays.removeAll(overlaysToRemove)

                // Dibujar Ruta (Polyline)
                if (uiState.routePoints.isNotEmpty()) {
                    val polyline = Polyline()
                    polyline.setPoints(uiState.routePoints)
                    polyline.outlinePaint.color = android.graphics.Color.BLUE
                    polyline.outlinePaint.strokeWidth = 10f
                    mv.overlays.add(polyline)
                    
                    mv.zoomToBoundingBox(polyline.bounds, true)
                }

                mv.invalidate()
            }
        )

        // Botón para centrar en mi ubicación
        FloatingActionButton(
            onClick = {
                myLocationOverlay.myLocation?.let {
                    mapView.controller.animateTo(it)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
        }

        // 2. Componente de Búsqueda
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar dirección...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                singleLine = true
            )

            if (uiState.searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(uiState.searchResults) { result ->
                            ListItem(
                                headlineContent = { Text(result.properties.getDisplayName()) },
                                modifier = Modifier.clickable {
                                    val currentLoc = myLocationOverlay.myLocation
                                    viewModel.selectDestination(
                                        result, 
                                        currentLoc?.latitude ?: -34.6037,
                                        currentLoc?.longitude ?: -58.3816
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Indicador de carga de ruta
        if (uiState.isLoadingRoute) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
