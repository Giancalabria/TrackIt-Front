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

data class Geometry(
    val type: String,
    val coordinates: Any // Can be List<Double> for Point or List<List<Double>> for LineString
) {
    @Suppress("UNCHECKED_CAST")
    fun asPoint(): List<Double> = coordinates as List<Double>

    @Suppress("UNCHECKED_CAST")
    fun asLineString(): List<List<Double>> = coordinates as List<List<Double>>
}
