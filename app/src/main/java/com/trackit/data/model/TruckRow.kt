package com.trackit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TruckInsertRow(
    val id: String,
    @SerialName("driver_id") val driverId: String,
    @SerialName("driver_name") val driverName: String,
    val plate: String
)
