package com.trackit.data.repository

import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object PackageRepository {
    private val initialPackages = listOf(
        Package(
            id = "PKG-001",
            clientName = "Laura Méndez",
            address = "Av. Corrientes 1234, CABA",
            eta = "10:30",
            size = PackageSize.MEDIUM,
            isFragile = false,
            status = PackageStatus.IN_TRANSIT
        ),
        Package(
            id = "PKG-002",
            clientName = "Carlos Ruiz",
            address = "Av. Santa Fe 2450, CABA",
            eta = "11:00",
            size = PackageSize.SMALL,
            isFragile = true,
            status = PackageStatus.IN_TRANSIT
        ),
        Package(
            id = "PKG-003",
            clientName = "Sofía Pérez",
            address = "Defensa 890, CABA",
            eta = "11:45",
            size = PackageSize.LARGE,
            isFragile = false,
            status = PackageStatus.PENDING
        ),
        Package(
            id = "PKG-004",
            clientName = "Diego López",
            address = "Cabildo 3200, CABA",
            eta = "12:15",
            size = PackageSize.MEDIUM,
            isFragile = false,
            status = PackageStatus.IN_TRANSIT
        ),
        Package(
            id = "PKG-005",
            clientName = "Valentina Gómez",
            address = "Rivadavia 4500, CABA",
            eta = "13:00",
            size = PackageSize.SMALL,
            isFragile = true,
            status = PackageStatus.PENDING
        ),
        Package(
            id = "PKG-006",
            clientName = "Martín Acosta",
            address = "Scalabrini Ortiz 1800, CABA",
            eta = "13:30",
            size = PackageSize.MEDIUM,
            isFragile = false,
            status = PackageStatus.DELIVERED
        ),
        Package(
            id = "PKG-007",
            clientName = "Paula Fernández",
            address = "Juramento 2100, CABA",
            eta = "14:00",
            size = PackageSize.LARGE,
            isFragile = false,
            status = PackageStatus.IN_TRANSIT
        ),
        Package(
            id = "PKG-008",
            clientName = "Tomás Herrera",
            address = "Larrea 950, CABA",
            eta = "14:30",
            size = PackageSize.SMALL,
            isFragile = false,
            status = PackageStatus.PENDING
        )
    )

    private val _packages = MutableStateFlow(initialPackages)
    val packages: StateFlow<List<Package>> = _packages.asStateFlow()

    fun getPackageById(id: String): Package? {
        return _packages.value.firstOrNull { it.id == id }
    }

    fun getDriverPackages(): List<Package> {
        return _packages.value.filter { it.status != PackageStatus.DELIVERED }
    }

    fun getWarehousePackages(): List<Package> {
        return _packages.value.filter { it.registeredByWarehouse }
    }

    fun updateStatus(id: String, status: PackageStatus) {
        _packages.update { packages ->
            packages.map { packageItem ->
                if (packageItem.id == id) {
                    packageItem.copy(status = status)
                } else {
                    packageItem
                }
            }
        }
    }

    fun addPackage(
        clientName: String,
        address: String,
        size: PackageSize,
        isFragile: Boolean
    ) {
        val nextId = "PKG-${(_packages.value.size + 1).toString().padStart(3, '0')}"
        val newPackage = Package(
            id = nextId,
            clientName = clientName,
            address = address,
            eta = "Por asignar",
            size = size,
            isFragile = isFragile,
            status = PackageStatus.PENDING,
            registeredByWarehouse = true
        )
        _packages.update { it + newPackage }
    }

    fun getDeliveredCount(): Int {
        return _packages.value.count { it.status == PackageStatus.DELIVERED }
    }

    fun getPendingCount(): Int {
        return _packages.value.count { it.status != PackageStatus.DELIVERED }
    }
}
