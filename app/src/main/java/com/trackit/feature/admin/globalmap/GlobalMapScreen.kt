package com.trackit.feature.admin.globalmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.BuildConfig
import com.trackit.core.ui.theme.DeepForestGreen
import com.trackit.core.ui.theme.TerracottaOrange
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private fun createTruckMarkerDrawable(
    context: android.content.Context,
    backgroundColor: Int
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val widthPx = (44 * d).toInt().coerceAtLeast(1)
    val heightPx = (54 * d).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * d
    }

    val centerX = widthPx / 2f
    val circleRadius = 16f * d
    val circleCenterY = 20f * d
    val tipY = heightPx - (4f * d)

    val pinPath = Path().apply {
        addCircle(centerX, circleCenterY, circleRadius, Path.Direction.CW)
        moveTo(centerX - circleRadius, circleCenterY + (4f * d))
        quadTo(centerX, tipY, centerX + circleRadius, circleCenterY + (4f * d))
        close()
    }
    canvas.drawPath(pinPath, bgPaint)
    canvas.drawPath(pinPath, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 18f * d
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val textY = circleCenterY + (textPaint.textSize / 3f)
    canvas.drawText("T", centerX, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun GlobalMapScreen(
    viewModel: GlobalMapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        }

        val mapView = androidx.compose.runtime.remember { MapView(context) }

        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(-34.6037, -58.3816))
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                val markerOverlays = mv.overlays.filterIsInstance<Marker>()
                mv.overlays.removeAll(markerOverlays)

                uiState.trucks.forEach { truck ->
                    val lat = truck.lastLat
                    val lon = truck.lastLon
                    if (lat != null && lon != null) {
                        val marker = Marker(mv).apply {
                            position = GeoPoint(lat, lon)
                            title = "${truck.id} · ${truck.plate}"
                            snippet = truck.driverName
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createTruckMarkerDrawable(
                                context = context,
                                backgroundColor = DeepForestGreen.toArgb()
                            )
                        }
                        mv.overlays.add(marker)
                    }
                }

                mv.invalidate()
            }
        )

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
