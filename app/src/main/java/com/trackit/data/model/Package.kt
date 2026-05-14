package com.trackit.data.model

enum class PackageSize {
    SMALL,
    MEDIUM,
    LARGE
}

enum class PackageStatus {
    PENDING,
    IN_TRANSIT,
    DELIVERED
}

data class Package(
    val id: String,
    val clientName: String,
    val address: String,
    val eta: String,
    val size: PackageSize,
    val isFragile: Boolean,
    val status: PackageStatus,
    val registeredByWarehouse: Boolean = false
)
