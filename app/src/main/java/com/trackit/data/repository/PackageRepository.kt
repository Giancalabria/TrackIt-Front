package com.trackit.data.repository

import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class PackageRepository : IPackageRepository {
    private val initialPackages = listOf(
        Package(
            id = "PKG-001",
            clientName = "Laura Méndez",
            address = "Av. Corrientes 1234, CABA",
            eta = "10:30",
            size = PackageSize.MEDIUM,
            isFragile = false,
            status = PackageStatus.EN_CAMINO,
            scheduledDate = LocalDate.now(),
            assignedDriverId = "USR-001"
        ),
        Package(
            id = "PKG-002",
            clientName = "Carlos Ruiz",
            address = "Av. Santa Fe 2450, CABA",
            eta = "11:00",
            size = PackageSize.SMALL,
            isFragile = true,
            status = PackageStatus.CARGADO,
            scheduledDate = LocalDate.now(),
            assignedDriverId = "USR-001"
        ),
        Package(
            id = "PKG-003",
            clientName = "Sofía Pérez",
            address = "Defensa 890, CABA",
            eta = "11:45",
            size = PackageSize.LARGE,
            isFragile = false,
            status = PackageStatus.EN_DEPOSITO,
            scheduledDate = LocalDate.now()
        ),
        Package(
            id = "PKG-004",
            clientName = "Diego López",
            address = "Cabildo 3200, CABA",
            eta = "12:15",
            size = PackageSize.MEDIUM,
            isFragile = false,
            status = PackageStatus.EN_DEPOSITO,
            scheduledDate = LocalDate.now()
        ),
        Package(
            id = "PKG-005",
            clientName = "Valentina Gómez",
            address = "Rivadavia 4500, CABA",
            eta = "13:00",
            size = PackageSize.SMALL,
            isFragile = true,
            status = PackageStatus.EN_DEPOSITO,
            scheduledDate = LocalDate.now()
        ),
        Package(
            id = "PKG-006",
            clientName = "Martín Acosta",
            address = "Scalabrini Ortiz 1800, CABA",
            eta = "13:30",
            size = PackageSize.MEDIUM,
            isFragile = false,
            status = PackageStatus.ENTREGADO,
            scheduledDate = LocalDate.now().minusDays(1),
            assignedDriverId = "USR-001"
        )
    )

    private val _packages = MutableStateFlow(initialPackages)
    override val packages: StateFlow<List<Package>> = _packages.asStateFlow()

    override suspend fun getPackageById(id: String): Package? {
        delay(300)
        return _packages.value.firstOrNull { it.id == id }
    }

    override suspend fun getPackagesByStatus(status: PackageStatus): List<Package> {
        delay(300)
        return _packages.value.filter { it.status == status }
    }

    override suspend fun getDriverPackages(driverId: String): List<Package> {
        delay(300)
        return _packages.value
            .filter { it.assignedDriverId == driverId }
            .sortedBy { it.eta }
    }

    override suspend fun updateStatus(id: String, status: PackageStatus) {
        delay(300)
        _packages.update { packages ->
            packages.map { packageItem ->
                when {
                    packageItem.id == id -> {
                        // Si el estado vuelve a EN_DEPOSITO, quitamos el chofer asignado
                        val driverId = if (status == PackageStatus.EN_DEPOSITO) null else packageItem.assignedDriverId
                        packageItem.copy(status = status, assignedDriverId = driverId)
                    }
                    // Mantener la exclusividad de EN_CAMINO para el mismo chofer
                    status == PackageStatus.EN_CAMINO && 
                    packageItem.status == PackageStatus.EN_CAMINO && 
                    packageItem.assignedDriverId == _packages.value.find { it.id == id }?.assignedDriverId -> {
                        packageItem.copy(status = PackageStatus.CARGADO)
                    }
                    else -> packageItem
                }
            }
        }
    }

    override suspend fun assignPackagesToDriver(packageIds: List<String>, driverId: String) {
        delay(500)
        _packages.update { packages ->
            packages.map { pkg ->
                if (pkg.id in packageIds) {
                    pkg.copy(status = PackageStatus.ASIGNADO, assignedDriverId = driverId)
                } else {
                    pkg
                }
            }
        }
    }

    override suspend fun simulateDailyCronJob(currentDate: LocalDate, driverIds: List<String>) {
        delay(1000)
        if (driverIds.isEmpty()) return

        _packages.update { packages ->
            val packagesToAssign = packages.filter { 
                it.status == PackageStatus.EN_DEPOSITO && it.scheduledDate == currentDate 
            }
            
            if (packagesToAssign.isEmpty()) return@update packages

            val updatedPackages = packages.toMutableList()
            packagesToAssign.forEachIndexed { index, pkg ->
                val driverId = driverIds[index % driverIds.size]
                val pkgIndex = updatedPackages.indexOfFirst { it.id == pkg.id }
                if (pkgIndex != -1) {
                    updatedPackages[pkgIndex] = updatedPackages[pkgIndex].copy(
                        status = PackageStatus.ASIGNADO,
                        assignedDriverId = driverId
                    )
                }
            }
            updatedPackages
        }
    }

    override suspend fun addPackage(
        clientName: String,
        address: String,
        size: PackageSize,
        isFragile: Boolean,
        scheduledDate: LocalDate
    ) {
        delay(300)
        val nextId = "PKG-${(_packages.value.size + 1).toString().padStart(3, '0')}"
        val newPackage = Package(
            id = nextId,
            clientName = clientName,
            address = address,
            eta = "15:00",
            size = size,
            isFragile = isFragile,
            status = PackageStatus.EN_DEPOSITO,
            scheduledDate = scheduledDate,
            registeredByWarehouse = true
        )
        _packages.update { it + newPackage }
    }

    override suspend fun getDeliveredCount(): Int {
        return _packages.value.count { it.status == PackageStatus.ENTREGADO }
    }

    override suspend fun getPendingCount(): Int {
        return _packages.value.count { it.status != PackageStatus.ENTREGADO }
    }

    companion object {
        private var instance: PackageRepository? = null
        fun getInstance(): PackageRepository {
            if (instance == null) {
                instance = PackageRepository()
            }
            return instance!!
        }
    }
}
