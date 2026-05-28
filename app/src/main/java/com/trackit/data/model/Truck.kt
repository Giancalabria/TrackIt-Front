package com.trackit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Truck(
    val id: String,
    @SerialName("driver_id")
    val driverId: String,
    @SerialName("driver_name")
    val driverName: String,
    val plate: String,
    @SerialName("last_lat")
    val lastLat: Double? = null,
    @SerialName("last_lon")
    val lastLon: Double? = null,
    val deliveredCount: Int = 0,
    val totalCount: Int = 0
)
