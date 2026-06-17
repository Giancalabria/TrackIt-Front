package com.trackit.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Raw JSON returned by the `daily-route-optimizer` Edge Function.
 * All fields are optional/defaulted because the function returns different
 * shapes for success, "no_jobs", and "no_vehicles" cases.
 */
@Serializable
data class RouteOptimizerResponse(
    val ok: Boolean = false,
    val targetDate: String = "",
    val vehicles: Int = 0,
    val jobs: Int = 0,
    val assigned: Int = 0,
    val unassigned: Int = 0,
    val reason: String? = null,
    val error: String? = null
)

private val routeOptimizerJson = Json { ignoreUnknownKeys = true }

/**
 * Invokes the daily route optimizer for [targetDate] (ISO `yyyy-MM-dd`) and decodes
 * its JSON response. The caller's admin JWT is forwarded automatically by the SDK.
 */
suspend fun SupabaseClient.invokeRouteOptimizer(targetDate: String): RouteOptimizerResponse {
    val response = functions.invoke(
        "daily-route-optimizer",
        mapOf("targetDate" to targetDate)
    )
    val raw = response.bodyAsText()
    return if (raw.isBlank()) {
        RouteOptimizerResponse(ok = true, targetDate = targetDate)
    } else {
        runCatching { routeOptimizerJson.decodeFromString<RouteOptimizerResponse>(raw) }
            .getOrElse { RouteOptimizerResponse(ok = true, targetDate = targetDate) }
    }
}
