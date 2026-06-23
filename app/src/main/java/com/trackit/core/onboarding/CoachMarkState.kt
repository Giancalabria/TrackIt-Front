package com.trackit.core.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot

val LocalCoachMarkRegistry = compositionLocalOf<CoachMarkRegistry> {
    error("CoachMarkRegistry not provided")
}

@Stable
class CoachMarkRegistry {
    private val targets = mutableStateMapOf<String, LayoutCoordinates>()
    var rootCoordinates by mutableStateOf<LayoutCoordinates?>(null)
        private set

    fun onRootPositioned(coordinates: LayoutCoordinates) {
        rootCoordinates = coordinates
    }

    fun register(key: String, coordinates: LayoutCoordinates) {
        targets[key] = coordinates
    }

    fun unregister(key: String) {
        targets.remove(key)
    }

    fun boundsFor(key: String): Rect? {
        val target = targets[key] ?: return null
        val root = rootCoordinates ?: return null
        if (!target.isAttached || !root.isAttached) return null
        return try {
            target.boundsInRoot()
        } catch (_: Exception) {
            null
        }
    }
}

@Stable
class CoachMarkController(
    val steps: List<CoachMarkStep>,
    private val onNavigate: (String) -> Unit,
    private val onFinished: () -> Unit
) {
    var currentIndex by mutableIntStateOf(0)
        private set

    val isActive: Boolean
        get() = steps.isNotEmpty() && currentIndex < steps.size

    val currentStep: CoachMarkStep?
        get() = steps.getOrNull(currentIndex)

    fun start() {
        currentIndex = 0
        currentStep?.navigateToRoute?.let(onNavigate)
    }

    fun next() {
        if (currentIndex >= steps.lastIndex) {
            onFinished()
            currentIndex = steps.size
        } else {
            currentIndex++
            currentStep?.navigateToRoute?.let(onNavigate)
        }
    }

    fun skip() {
        onFinished()
        currentIndex = steps.size
    }
}

@Composable
fun rememberCoachMarkController(
    steps: List<CoachMarkStep>,
    onNavigate: (String) -> Unit,
    onFinished: () -> Unit
): CoachMarkController {
    return androidx.compose.runtime.remember(steps, onNavigate, onFinished) {
        CoachMarkController(steps, onNavigate, onFinished)
    }
}
