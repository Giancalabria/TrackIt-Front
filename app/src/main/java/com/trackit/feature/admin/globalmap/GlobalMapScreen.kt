package com.trackit.feature.admin.globalmap

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.R
import com.trackit.BuildConfig
import com.trackit.core.ui.theme.DeepForestGreen
import com.trackit.core.ui.theme.LightBlue
import com.trackit.core.ui.theme.TerracottaOrange
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val lastSeenFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

private fun formatLastSeen(instant: Instant?): String =
    instant?.let { "última vez: ${lastSeenFormatter.format(it)}" } ?: "sin ubicación reciente"

private fun createTruckMarkerDrawable(
    context: android.content.Context
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val sizePx = (48 * d).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val iconDrawable = context.getDrawable(R.drawable.icono_camion)
    if (iconDrawable != null) {
        // Redondeamos los bordes de la imagen del camión
        val radius = 12f * d
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

private fun createAdminLocationIcon(context: android.content.Context, fillColor: Int): Bitmap {
    val d = context.resources.displayMetrics.density
    val size = (44 * d).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val strokeW = 4.5f * d
    val radius = size / 2f - strokeW
    val cx = size / 2f
    val cy = size / 2f
    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor; style = Paint.Style.FILL
    })
    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = strokeW
    })
    canvas.drawCircle(cx, cy, 3.2f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; style = Paint.Style.FILL
    })
    return bitmap
}

@Composable
fun GlobalMapScreen(
    viewModel: GlobalMapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
            if (!hasLocationPermission) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        val mapView = remember { MapView(context) }

        androidx.compose.runtime.LaunchedEffect(uiState.trucks) {
            val points = uiState.trucks.mapNotNull { truck ->
                val lat = truck.lastLat
                val lon = truck.lastLon
                if (lat != null && lon != null) GeoPoint(lat, lon) else null
            }

            if (points.isNotEmpty()) {
                if (points.size == 1) {
                    mapView.controller.setZoom(13.0)
                    mapView.controller.setCenter(points.first())
                } else {
                    mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 120)
                }
            }
        }

        val myLocationOverlay = remember(hasLocationPermission) {
            if (!hasLocationPermission) return@remember null
            MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                val icon = createAdminLocationIcon(context, LightBlue.toArgb())
                setPersonIcon(icon)
                setDirectionArrow(icon, icon)
                enableMyLocation()
                enableFollowLocation()
            }
        }

        androidx.compose.runtime.DisposableEffect(myLocationOverlay) {
            myLocationOverlay?.let {
                if (!mapView.overlays.contains(it)) {
                    mapView.overlays.add(it)
                }
            }
            onDispose {
                myLocationOverlay?.let {
                    it.disableMyLocation()
                    mapView.overlays.remove(it)
                }
            }
        }

        androidx.compose.runtime.DisposableEffect(context) {
            onDispose {
                mapView.onDetach()
            }
        }

        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(10.0)
                    controller.setCenter(GeoPoint(-34.6037, -58.3816))
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                // Remove only truck markers — keep MyLocationOverlay untouched.
                val markerOverlays = mv.overlays.filterIsInstance<Marker>()
                mv.overlays.removeAll(markerOverlays)

                uiState.trucks.forEach { truck ->
                    val lat = truck.lastLat
                    val lon = truck.lastLon
                    if (lat != null && lon != null) {
                        val point = GeoPoint(lat, lon)
                        val marker = Marker(mv).apply {
                            position = point
                            title = "${truck.driverName} · ${truck.plate}"
                            snippet = formatLastSeen(truck.lastLocationAt)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createTruckMarkerDrawable(
                                context = context
                            )
                        }
                        mv.overlays.add(marker)
                    }
                }

                mv.invalidate()
            }
        )

        FloatingActionButton(
            onClick = {
                myLocationOverlay?.myLocation?.let { mapView.controller.animateTo(it) }
                    ?: permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
        }

        if (!uiState.isLoading) {
            MetricsOverlay(
                delivered = uiState.deliveredCount,
                pending = uiState.pendingCount,
                trucks = uiState.activeTrucks,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun MetricsOverlay(
    delivered: Int,
    pending: Int,
    trucks: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem("Entregados", delivered.toString(), DeepForestGreen)
            VerticalDivider(modifier = Modifier.height(24.dp))
            MetricItem("Pendientes", pending.toString(), TerracottaOrange)
            VerticalDivider(modifier = Modifier.height(24.dp))
            MetricItem("Camiones", trucks.toString(), MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
