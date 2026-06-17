package com.trackit.data.model

import java.time.Instant
import com.trackit.data.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Truck(
    val id: String,
    @SerialName("driver_id")
    val driverId: String,
    @SerialName("driver_name")
    val driverName: String = "",
    val plate: String,
    @SerialName("last_lat")
    val lastLat: Double? = null,
    @SerialName("last_lon")
    val lastLon: Double? = null,
    @Serializable(with = InstantSerializer::class)
    @SerialName("last_location_at")
    val lastLocationAt: Instant? = null,
    // Computed client-side from packages; never persisted in the `trucks` table.
    @Transient
    val deliveredCount: Int = 0,
    @Transient
    val totalCount: Int = 0
)
