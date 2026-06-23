package com.trackit.testutil

import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import java.time.LocalDate

fun samplePackage(
    id: String = "pkg-1",
    clientName: String = "Juan Pérez",
    address: String = "Av. Corrientes 1234",
    status: PackageStatus = PackageStatus.EN_DEPOSITO,
    registeredByWarehouse: Boolean = true,
    barcode: String = "ABC123"
): Package = Package(
    id = id,
    clientName = clientName,
    address = address,
    destinationLat = -34.6,
    destinationLon = -58.4,
    eta = "",
    size = PackageSize.MEDIUM,
    isFragile = false,
    status = status,
    scheduledDate = LocalDate.now().plusDays(1),
    registeredByWarehouse = registeredByWarehouse,
    barcode = barcode
)
