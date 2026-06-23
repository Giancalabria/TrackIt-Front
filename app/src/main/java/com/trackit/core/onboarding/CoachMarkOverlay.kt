package com.trackit.core.onboarding

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun Modifier.coachMarkTarget(key: String): Modifier = composed {
    val registry = LocalCoachMarkRegistry.current
    onGloballyPositioned { coordinates ->
        registry.register(key, coordinates)
    }
}

@Composable
fun CoachMarkOverlay(
    controller: CoachMarkController,
    registry: CoachMarkRegistry,
    modifier: Modifier = Modifier
) {
    val step = controller.currentStep ?: return
    var targetBounds by remember(step.targetKey, controller.currentIndex) {
        mutableStateOf<Rect?>(null)
    }

    LaunchedEffect(step.targetKey, controller.currentIndex) {
        repeat(8) {
            delay(80L)
            targetBounds = registry.boundsFor(step.targetKey)
            if (targetBounds != null) return@LaunchedEffect
        }
    }

    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { /* Block taps on the scrim during the tour. */ }
            }
            .onGloballyPositioned { registry.onRootPositioned(it) }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.72f))

            val bounds = targetBounds
            if (bounds != null) {
                val padding = 8.dp.toPx()
                val hole = Rect(
                    left = bounds.left - padding,
                    top = bounds.top - padding,
                    right = bounds.right + padding,
                    bottom = bounds.bottom + padding
                )
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(hole.left, hole.top),
                    size = Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        CoachMarkTooltip(
            step = step,
            targetBounds = targetBounds,
            stepIndex = controller.currentIndex,
            totalSteps = controller.steps.size,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            onNext = controller::next,
            onSkip = controller::skip
        )
    }
}

@Composable
private fun CoachMarkTooltip(
    step: CoachMarkStep,
    targetBounds: Rect?,
    stepIndex: Int,
    totalSteps: Int,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val cardWidthPx = min(screenWidthPx * 0.88f, with(density) { 340.dp.toPx() })
    val cardWidthDp = with(density) { cardWidthPx.toDp() }
    val cardHeightEstimatePx = with(density) { 180.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }

    val cardOffsetPx = remember(targetBounds, stepIndex) {
        if (targetBounds == null) {
            Offset(
                x = (screenWidthPx - cardWidthPx) / 2f,
                y = (screenHeightPx - cardHeightEstimatePx) / 2f
            )
        } else {
            val centerX = (screenWidthPx - cardWidthPx) / 2f
            val belowY = targetBounds.bottom + marginPx
            val aboveY = targetBounds.top - cardHeightEstimatePx - marginPx
            val y = if (belowY + cardHeightEstimatePx <= screenHeightPx - marginPx) {
                belowY
            } else {
                max(marginPx, aboveY)
            }
            Offset(centerX, y)
        }
    }

    Card(
        modifier = Modifier
            .offset {
                IntOffset(cardOffsetPx.x.roundToInt(), cardOffsetPx.y.roundToInt())
            }
            .widthIn(max = cardWidthDp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Paso ${stepIndex + 1} de $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = step.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("Omitir tour")
                }
                Button(onClick = onNext) {
                    Text(if (stepIndex >= totalSteps - 1) "Listo" else "Siguiente")
                }
            }
        }
    }
}
