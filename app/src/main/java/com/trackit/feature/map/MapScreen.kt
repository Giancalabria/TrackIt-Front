package com.trackit.feature.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.BuildConfig
import com.trackit.core.ui.theme.TerracottaOrange
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private fun createBoxMarkerDrawable(
    context: android.content.Context,
    backgroundColor: Int
): BitmapDrawable {
    val sizePx = (40 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = (2.5f * context.resources.displayMetrics.density)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    val radius = 12f * context.resources.displayMetrics.density
    canvas.drawRoundRect(
        RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
        radius,
        radius,
        bgPaint
    )

    // Simple “package” icon (box) drawn with strokes
    val pad = 11f * context.resources.displayMetrics.density
    val left = pad
    val top = pad + (1f * context.resources.displayMetrics.density)
    val right = sizePx - pad
    val bottom = sizePx - pad

    // Outer box
    canvas.drawRoundRect(
        RectF(left, top, right, bottom),
        4f * context.resources.displayMetrics.density,
        4f * context.resources.displayMetrics.density,
        iconPaint
    )
    // Vertical seam
    canvas.drawLine(
        sizePx / 2f,
        top,
        sizePx / 2f,
        bottom,
        iconPaint
    )
    // Top flap
    canvas.drawLine(left, top + (6f * context.resources.displayMetrics.density), right, top + (6f * context.resources.displayMetrics.density), iconPaint)

    return BitmapDrawable(context.resources, bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Initialize Osmdroid Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    val mapView = remember { MapView(context) }
    val myLocationOverlay = remember(hasLocationPermission) {
        if (!hasLocationPermission) return@remember null
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Para mostrar tu ubicación, necesitamos permiso de ubicación.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Text("Permitir ubicación")
                    }
                }
            }
        } else {
            // 1. Mapa Base (Osmdroid)
            AndroidView(
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)

                        // Añadir capa de ubicación del usuario
                        myLocationOverlay?.let { overlays.add(it) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    myLocationOverlay?.myLocation?.let { loc ->
                        viewModel.updateUserLocation(loc.latitude, loc.longitude)
                    }

                    // Limpiar solo rutas, manteniendo el overlay de ubicación
                    val overlaysToRemove = mv.overlays.filterIsInstance<Polyline>()
                    mv.overlays.removeAll(overlaysToRemove)

                    // Limpiar marcadores (paquetes) para re-dibujar el estado actual
                    val markerOverlays = mv.overlays.filterIsInstance<Marker>()
                    mv.overlays.removeAll(markerOverlays)

                    // Dibujar Paquetes (Markers)
                    uiState.assignedPackages.forEach { pkg ->
                        val lat = pkg.destinationLat
                        val lon = pkg.destinationLon
                        if (lat != null && lon != null) {
                            val marker = Marker(mv).apply {
                                position = GeoPoint(lat, lon)
                                title = pkg.clientName
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = createBoxMarkerDrawable(
                                    context = context,
                                    backgroundColor = TerracottaOrange.toArgb()
                                )
                            }
                            mv.overlays.add(marker)
                        }
                    }

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
                    myLocationOverlay?.myLocation?.let {
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
                                    val currentLoc = myLocationOverlay?.myLocation
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
