package com.trackit.data.local.mapper

import com.trackit.data.local.entity.PackageEntity
import com.trackit.data.local.entity.TruckEntity
import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.Truck
import java.time.Instant
import java.time.LocalDate

fun PackageEntity.toDomain(): Package = Package(
    id = id,
    clientName = clientName,
    address = address,
    destinationLat = destinationLat,
    destinationLon = destinationLon,
    eta = eta,
    size = runCatching { PackageSize.valueOf(size) }.getOrDefault(PackageSize.MEDIUM),
    isFragile = isFragile,
    status = runCatching { PackageStatus.valueOf(status) }.getOrDefault(PackageStatus.EN_DEPOSITO),
    scheduledDate = runCatching { LocalDate.parse(scheduledDate) }.getOrDefault(LocalDate.now()),
    assignedDriverId = assignedDriverId,
    registeredByWarehouse = registeredByWarehouse,
    barcode = barcode,
    routeOrder = routeOrder,
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
    deliveredAt = deliveredAtMillis?.let { Instant.ofEpochMilli(it) }
)

fun Package.toEntity(
    pendingSync: Boolean,
    updatedAtMillis: Long = updatedAt?.toEpochMilli() ?: System.currentTimeMillis(),
    deliveredAtMillis: Long? = deliveredAt?.toEpochMilli(),
    deletedLocally: Boolean = false
): PackageEntity = PackageEntity(
    id = id,
    clientName = clientName,
    address = address,
    destinationLat = destinationLat,
    destinationLon = destinationLon,
    eta = eta,
    size = size.name,
    isFragile = isFragile,
    status = status.name,
    scheduledDate = scheduledDate.toString(),
    assignedDriverId = assignedDriverId,
    registeredByWarehouse = registeredByWarehouse,
    barcode = barcode,
    routeOrder = routeOrder,
    updatedAtMillis = updatedAtMillis,
    deliveredAtMillis = deliveredAtMillis,
    pendingSync = pendingSync,
    deletedLocally = deletedLocally
)

fun TruckEntity.toDomain(): Truck = Truck(
    id = id,
    driverId = driverId,
    driverName = driverName,
    plate = plate,
    lastLat = lastLat,
    lastLon = lastLon,
    lastLocationAt = lastLocationAtMillis?.let { Instant.ofEpochMilli(it) }
)

fun Truck.toEntity(
    pendingSync: Boolean,
    updatedAtMillis: Long = System.currentTimeMillis()
): TruckEntity = TruckEntity(
    id = id,
    driverId = driverId,
    driverName = driverName,
    plate = plate,
    lastLat = lastLat,
    lastLon = lastLon,
    lastLocationAtMillis = lastLocationAt?.toEpochMilli(),
    updatedAtMillis = updatedAtMillis,
    pendingSync = pendingSync
)
