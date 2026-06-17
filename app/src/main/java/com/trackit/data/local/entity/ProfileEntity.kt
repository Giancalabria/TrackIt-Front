package com.trackit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val role: String,
    val updatedAtMillis: Long,
    val pendingSync: Boolean
)
