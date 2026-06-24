package com.trackit.data.model

import com.trackit.data.model.PackageStatus.ASIGNADO
import com.trackit.data.model.PackageStatus.CARGADO
import com.trackit.data.model.PackageStatus.EN_CAMINO
import com.trackit.data.model.PackageStatus.EN_DEPOSITO
import com.trackit.data.model.PackageStatus.ENTREGADO
import com.trackit.data.model.PackageStatus.FALLIDO

fun Package.isEditableByWarehouse(): Boolean =
    status == EN_DEPOSITO || status == ASIGNADO

/**
 * A scanned/typed code matches a package when it equals its barcode (preferred) or its id.
 * If the package has no barcode, the id is the expected value.
 */
fun Package.matchesCode(code: String): Boolean {
    val input = code.trim()
    if (input.isBlank()) return false
    val expected = barcode.ifBlank { id }
    return input.equals(expected, ignoreCase = true) || input.equals(id, ignoreCase = true)
}

fun Package.canLoadOntoTruck(truck: Truck): Boolean =
    status == ASIGNADO && assignedDriverId == truck.driverId

fun Package.isAlreadyLoadedOrBeyond(): Boolean = when (status) {
    CARGADO, EN_CAMINO, ENTREGADO, FALLIDO -> true
    else -> false
}
