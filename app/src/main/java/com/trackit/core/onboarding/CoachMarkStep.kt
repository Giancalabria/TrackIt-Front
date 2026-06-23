package com.trackit.core.onboarding

data class CoachMarkStep(
    val targetKey: String,
    val title: String,
    val message: String,
    /** Navigate to this route before highlighting the target (e.g. switch bottom-tab). */
    val navigateToRoute: String? = null
)
