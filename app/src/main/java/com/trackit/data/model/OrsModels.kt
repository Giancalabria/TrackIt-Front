package com.trackit.data.model

import com.google.gson.annotations.SerializedName

data class OrsRequest(
    val coordinates: List<List<Double>>
)

data class OrsResponse(
    val features: List<OrsFeature>
)

data class OrsFeature(
    val geometry: Geometry
)

data class OrsOptimizationRequest(
    val vehicles: List<OrsVehicle>,
    val jobs: List<OrsJob>
)

data class OrsVehicle(
    val id: Int,
    val start: List<Double>,
    val end: List<Double>? = null,
    val capacity: List<Int>? = null
)

data class OrsJob(
    val id: Int,
    val location: List<Double>,
    val amount: List<Int>? = null
)

data class OrsOptimizationResponse(
    val routes: List<OrsOptimizedRoute> = emptyList(),
    val unassigned: List<OrsUnassignedJob> = emptyList()
)

data class OrsOptimizedRoute(
    @SerializedName("vehicle")
    val vehicleId: Int,
    val steps: List<OrsOptimizedStep> = emptyList()
)

data class OrsOptimizedStep(
    val type: String,
    val job: Int? = null,
    val location: List<Double>? = null
)

data class OrsUnassignedJob(
    val id: Int,
    val reason: String? = null
)
