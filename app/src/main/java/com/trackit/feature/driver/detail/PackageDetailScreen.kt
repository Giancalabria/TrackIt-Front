package com.trackit.feature.driver.detail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.BarcodeScannerSheet
import com.trackit.core.ui.components.MapPlaceholder
import com.trackit.core.ui.components.PackageStatusChip
import com.trackit.BuildConfig
import com.trackit.core.ui.theme.TerracottaOrange
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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

    val pad = 11f * context.resources.displayMetrics.density
    val left = pad
    val top = pad + (1f * context.resources.displayMetrics.density)
    val right = sizePx - pad
    val bottom = sizePx - pad

    canvas.drawRoundRect(
        RectF(left, top, right, bottom),
        4f * context.resources.displayMetrics.density,
        4f * context.resources.displayMetrics.density,
        iconPaint
    )
    canvas.drawLine(sizePx / 2f, top, sizePx / 2f, bottom, iconPaint)
    canvas.drawLine(left, top + (6f * context.resources.displayMetrics.density), right, top + (6f * context.resources.displayMetrics.density), iconPaint)

    return BitmapDrawable(context.resources, bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDetailScreen(
    onBack: () -> Unit,
    viewModel: PackageDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.scanCompleted) {
        if (uiState.scanCompleted) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Paquete") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val pkg = uiState.packageItem
            if (pkg != null && !uiState.scanCompleted) {
                if (pkg.status == PackageStatus.ASIGNADO || pkg.status == PackageStatus.EN_CAMINO) {
                    ExtendedFloatingActionButton(
                        onClick = viewModel::openScanner,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Escanear")
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.packageItem == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Paquete no encontrado.")
                }
            }

            else -> {
                val packageItem = uiState.packageItem!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        val lat = packageItem.destinationLat
                        val lon = packageItem.destinationLon
                        if (lat != null && lon != null) {
                            LaunchedEffect(Unit) {
                                Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
                            }

                            val mapView = androidx.compose.runtime.remember { MapView(context) }
                            AndroidView(
                                factory = {
                                    mapView.apply {
                                        setTileSource(TileSourceFactory.MAPNIK)
                                        setMultiTouchControls(false)
                                        controller.setZoom(16.0)
                                        controller.setCenter(GeoPoint(lat, lon))
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { mv ->
                                    val markerOverlays = mv.overlays.filterIsInstance<Marker>()
                                    mv.overlays.removeAll(markerOverlays)

                                    val marker = Marker(mv).apply {
                                        position = GeoPoint(lat, lon)
                                        title = packageItem.clientName
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        icon = createBoxMarkerDrawable(
                                            context = context,
                                            backgroundColor = TerracottaOrange.toArgb()
                                        )
                                    }
                                    mv.overlays.add(marker)
                                    mv.invalidate()
                                }
                            )
                        } else {
                            MapPlaceholder(label = "Ubicación no disponible")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = packageItem.clientName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = packageItem.address,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Tamaño: ${packageItem.size.toLabel()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (packageItem.isFragile) {
                            SuggestionChip(
                                onClick = {},
                                enabled = false,
                                label = { Text("Frágil") }
                            )
                        }
                        PackageStatusChip(status = packageItem.status)
                    }
                }
            }
        }
    }

    if (uiState.isScannerOpen) {
        val scannerTitle = when (uiState.packageItem?.status) {
            PackageStatus.ASIGNADO -> "Escanear para Cargar"
            PackageStatus.EN_CAMINO -> "Escanear para Entregar"
            else -> "Escanear Paquete"
        }

        BarcodeScannerSheet(
            onCodeScanned = viewModel::onCodeScanned,
            onDismiss = viewModel::closeScanner,
            title = scannerTitle
        )
    }
}

private fun PackageSize.toLabel(): String = when (this) {
    PackageSize.SMALL -> "Pequeño"
    PackageSize.MEDIUM -> "Mediano"
    PackageSize.LARGE -> "Grande"
}
