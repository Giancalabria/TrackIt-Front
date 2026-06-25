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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.R
import com.trackit.BuildConfig
import com.trackit.core.ui.components.BarcodeScannerSheet
import com.trackit.core.ui.theme.LightBlue
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private fun createCustomMarker(
    context: android.content.Context
): BitmapDrawable {
    val sizePx = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val iconDrawable = context.getDrawable(R.drawable.icono_deliver)
    if (iconDrawable != null) {
        val radius = 12f * context.resources.displayMetrics.density
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        
        val path = android.graphics.Path().apply {
            addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(path)
        iconDrawable.setBounds(0, 0, sizePx, sizePx)
        iconDrawable.draw(canvas)
        canvas.restore()
    }

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserLocationIcon(
    context: android.content.Context,
    fillColor: Int
): Bitmap {
    val sizePx = (44 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = 4.5f * context.resources.displayMetrics.density
    val radius = (sizePx / 2f) - strokeWidth

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }

    val cx = sizePx / 2f
    val cy = sizePx / 2f

    canvas.drawCircle(cx, cy, radius, fillPaint)
    canvas.drawCircle(cx, cy, radius, strokePaint)
    canvas.drawCircle(cx, cy, 3.2f * context.resources.displayMetrics.density, centerDotPaint)

    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var scanningPackage by remember { mutableStateOf<Package?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

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

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    val mapView = remember { MapView(context) }
    val myLocationOverlay = remember(hasLocationPermission) {
        if (!hasLocationPermission) return@remember null
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            val person = createUserLocationIcon(
                context = context,
                fillColor = LightBlue.toArgb()
            )
            setPersonIcon(person)
            setDirectionArrow(person, person)
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
            AndroidView(
                factory = {
                    mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)

                    myLocationOverlay?.let { overlays.add(it) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    myLocationOverlay?.myLocation?.let { loc ->
                        viewModel.updateUserLocation(loc.latitude, loc.longitude)
                    }

                    val overlaysToRemove = mv.overlays.filterIsInstance<Polyline>()
                    mv.overlays.removeAll(overlaysToRemove)

                    val markerOverlays = mv.overlays.filterIsInstance<Marker>()
                    mv.overlays.removeAll(markerOverlays)

                    // Only show markers for packages that are NOT delivered or failed
                    uiState.assignedPackages
                        .filter { it.status != PackageStatus.ENTREGADO && it.status != PackageStatus.FALLIDO }
                        .forEach { pkg ->
                            val lat = pkg.destinationLat
                            val lon = pkg.destinationLon
                            if (lat != null && lon != null) {
                                val marker = Marker(mv).apply {
                                    position = GeoPoint(lat, lon)
                                    title = pkg.clientName
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    icon = createCustomMarker(
                                        context = context
                                    )
                                }
                                mv.overlays.add(marker)
                            }
                        }

                    if (uiState.routeSegments.isNotEmpty()) {
                        val allPoints = mutableListOf<GeoPoint>()
                        uiState.routeSegments.forEach { segment ->
                            val polyline = Polyline()
                            polyline.setPoints(segment)
                            polyline.outlinePaint.color = android.graphics.Color.BLUE // Single color
                            polyline.outlinePaint.strokeWidth = 10f
                            mv.overlays.add(polyline)
                            allPoints.addAll(segment)
                        }

                        if (allPoints.isNotEmpty()) {
                            val boundingBox = BoundingBox.fromGeoPoints(allPoints)
                            mv.zoomToBoundingBox(boundingBox, true)
                        }
                    }

                    mv.invalidate()
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.assignedPackages.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val loc = myLocationOverlay?.myLocation
                            viewModel.startTrip(
                                loc?.latitude ?: -34.6037,
                                loc?.longitude ?: -58.3816
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar trayecto")
                    }
                }

                FloatingActionButton(
                    onClick = {
                        myLocationOverlay?.myLocation?.let {
                            mapView.controller.animateTo(it)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                }
            }
        }

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

        uiState.nextPackage?.let { pkg ->
            NextDeliveryPanel(
                pkg = pkg,
                onDeliverClick = { scanningPackage = pkg },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp) // Below search bar
                    .padding(horizontal = 16.dp)
            )
        }

        scanningPackage?.let { pkg ->
            BarcodeScannerSheet(
                title = "Escanear para Entregar",
                onCodeScanned = { code ->
                    viewModel.deliverPackage(pkg.id, code)
                    scanningPackage = null
                },
                onDismiss = { scanningPackage = null }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun NextDeliveryPanel(
    pkg: Package,
    onDeliverClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Próxima Entrega",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = pkg.clientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = pkg.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onDeliverClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Entregar")
            }
        }
    }
}
