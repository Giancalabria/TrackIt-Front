package com.trackit.data.model

/**
 * Domain result of triggering the `daily-route-optimizer` Edge Function.
 * Surfaced to the admin UI so it can report how many packages were actually assigned.
 */
data class RouteOptimizationResult(
    val ok: Boolean,
    val targetDate: String,
    val assigned: Int,
    val unassigned: Int,
    val reason: String? = null,
    val error: String? = null
)
