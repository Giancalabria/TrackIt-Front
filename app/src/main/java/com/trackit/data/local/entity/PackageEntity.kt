package com.trackit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room representation of a package. This is the single source of truth for the UI.
 * [pendingSync] marks rows changed locally that still need to be pushed to Supabase,
 * and [deletedLocally] is a tombstone for offline deletions.
 */
@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val id: String,
    val clientName: String,
    val address: String,
    val destinationLat: Double?,
    val destinationLon: Double?,
    val eta: String,
    val size: String,
    val isFragile: Boolean,
    val status: String,
    val scheduledDate: String,
    val assignedDriverId: String?,
    val registeredByWarehouse: Boolean,
    val barcode: String,
    val routeOrder: Int?,
    val updatedAtMillis: Long,
    val deliveredAtMillis: Long?,
    val assignedDriverName: String?,
    val pendingSync: Boolean,
    val deletedLocally: Boolean
)
