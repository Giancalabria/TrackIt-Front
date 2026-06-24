package com.trackit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trucks")
data class TruckEntity(
    @PrimaryKey val id: String,
    val driverId: String,
    val driverName: String,
    val plate: String,
    val lastLat: Double?,
    val lastLon: Double?,
    val lastLocationAtMillis: Long?,
    val routeStartLat: Double?,
    val routeStartLon: Double?,
    val routeStartLabel: String?,
    val routeStartUpdatedAtMillis: Long?,
    val updatedAtMillis: Long,
    val pendingSync: Boolean
)
