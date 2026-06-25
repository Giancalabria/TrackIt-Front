package com.trackit.data.model

import java.time.Instant
import java.time.LocalDate
import com.trackit.data.serialization.InstantSerializer
import com.trackit.data.serialization.LocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PackageSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Serializable
enum class PackageStatus {
    EN_DEPOSITO,
    ASIGNADO,
    CARGADO,
    EN_CAMINO,
    ENTREGADO,
    FALLIDO
}

@Serializable
data class Package(
    val id: String,
    @SerialName("client_name")
    val clientName: String,
    val address: String,
    @SerialName("destination_lat")
    val destinationLat: Double? = null,
    @SerialName("destination_lon")
    val destinationLon: Double? = null,
    val eta: String,
    val size: PackageSize,
    @SerialName("is_fragile")
    val isFragile: Boolean,
    val status: PackageStatus,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("scheduled_date")
    val scheduledDate: LocalDate = LocalDate.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires")),
    @SerialName("assigned_driver_id")
    val assignedDriverId: String? = null,
    @SerialName("registered_by_warehouse")
    val registeredByWarehouse: Boolean = false,
    @SerialName("barcode")
    val barcode: String = "",
    @SerialName("route_order")
    val routeOrder: Int? = null,
    @Serializable(with = InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    @SerialName("delivered_at")
    val deliveredAt: Instant? = null,
    @SerialName("assigned_driver_name")
    val assignedDriverName: String? = null
)
