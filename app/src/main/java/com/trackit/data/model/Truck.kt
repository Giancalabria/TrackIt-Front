package com.trackit.data.model

data class Truck(
    val id: String,
    val driverId: String,
    val driverName: String,
    val plate: String,
    val lastLat: Double? = null,
    val lastLon: Double? = null,
    val deliveredCount: Int,
    val totalCount: Int
)
