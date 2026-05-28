package com.trackit.data.model

import com.google.gson.annotations.SerializedName

data class PhotonResponse(
    val features: List<PhotonFeature>
)

data class PhotonFeature(
    val properties: PhotonProperties,
    val geometry: Geometry
)

data class PhotonProperties(
    val name: String?,
    val street: String?,
    @SerializedName("housenumber")
    val houseNumber: String?,
    val city: String?,
    val country: String?
) {
    fun getDisplayName(): String {
        val streetWithNumber = when {
            street.isNullOrBlank() -> null
            houseNumber.isNullOrBlank() -> street
            else -> "$street $houseNumber"
        }
        return listOfNotNull(name, streetWithNumber, city).joinToString(", ")
    }
}

data class OrsRequest(
    val coordinates: List<List<Double>>
)

data class OrsResponse(
    val features: List<OrsFeature>
)

data class OrsFeature(
    val geometry: Geometry
)

data class Geometry(
    val type: String,
    val coordinates: Any // Can be List<Double> for Point or List<List<Double>> for LineString
) {
    @Suppress("UNCHECKED_CAST")
    fun asPoint(): List<Double> = coordinates as List<Double>
    
    @Suppress("UNCHECKED_CAST")
    fun asLineString(): List<List<Double>> = coordinates as List<List<Double>>
}

// -----------------------------------------------------------------------------
// OpenRouteService Optimization (VROOM) DTOs
// -----------------------------------------------------------------------------

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
