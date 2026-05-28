package com.trackit.data.model

import java.time.LocalDate

enum class PackageSize {
    SMALL,
    MEDIUM,
    LARGE
}

enum class PackageStatus {
    EN_DEPOSITO,
    ASIGNADO,
    CARGADO,
    EN_CAMINO,
    ENTREGADO,
    FALLIDO
}

data class Package(
    val id: String,
    val clientName: String,
    val address: String,
    val destinationLat: Double? = null,
    val destinationLon: Double? = null,
    val eta: String,
    val size: PackageSize,
    val isFragile: Boolean,
    val status: PackageStatus,
    val scheduledDate: LocalDate = LocalDate.now(),
    val assignedDriverId: String? = null,
    val registeredByWarehouse: Boolean = false
)
