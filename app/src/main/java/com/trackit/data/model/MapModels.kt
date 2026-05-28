package com.trackit.data.model

import com.google.gson.annotations.SerializedName

// Photon API Models
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
    val city: String?,
    val country: String?
) {
    fun getDisplayName(): String {
        return listOfNotNull(name, street, city).joinToString(", ")
    }
}

// OpenRouteService API Models
data class OrsRequest(
    val coordinates: List<List<Double>>
)

data class OrsResponse(
    val features: List<OrsFeature>
)

data class OrsFeature(
    val geometry: Geometry
)

// Shared Geometry
data class Geometry(
    val type: String,
    val coordinates: Any // Can be List<Double> for Point or List<List<Double>> for LineString
) {
    @Suppress("UNCHECKED_CAST")
    fun asPoint(): List<Double> = coordinates as List<Double>
    
    @Suppress("UNCHECKED_CAST")
    fun asLineString(): List<List<Double>> = coordinates as List<List<Double>>
}
