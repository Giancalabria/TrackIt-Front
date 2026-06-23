package com.trackit.fakes

import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.RouteOptimizationResult
import com.trackit.data.repository.IPackageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakePackageRepository(
    initialPackages: List<Package> = emptyList()
) : IPackageRepository {

    private val _packages = MutableStateFlow(initialPackages)
    override val packages: StateFlow<List<Package>> = _packages.asStateFlow()

    var lastUpdatedPackage: Package? = null
    var lastDeletedId: String? = null
    var updateShouldFail: Boolean = false
    var deleteShouldFail: Boolean = false

    override suspend fun getPackageById(id: String): Package? =
        _packages.value.find { it.id == id }

    override suspend fun getPackageByIdFlow(id: String): Flow<Package?> =
        _packages.map { list -> list.find { it.id == id } }

    override suspend fun getPackagesByStatus(status: PackageStatus): List<Package> =
        _packages.value.filter { it.status == status }

    override suspend fun getDriverPackages(driverId: String): List<Package> =
        _packages.value.filter { it.assignedDriverId == driverId }

    override suspend fun updateStatus(id: String, status: PackageStatus): Result<Unit> =
        Result.success(Unit)

    override suspend fun assignPackagesToDriver(
        packageIds: List<String>,
        driverId: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun triggerRouteOptimization(
        targetDate: LocalDate
    ): Result<RouteOptimizationResult> = Result.success(
        RouteOptimizationResult(
            ok = true,
            targetDate = targetDate.toString(),
            assigned = 0,
            unassigned = 0
        )
    )

    override suspend fun addPackage(
        clientName: String,
        address: String,
        destinationLat: Double?,
        destinationLon: Double?,
        size: PackageSize,
        isFragile: Boolean,
        scheduledDate: LocalDate,
        barcode: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updatePackage(updatedPackage: Package): Result<Unit> {
        if (updateShouldFail) return Result.failure(IllegalStateException("fail"))
        lastUpdatedPackage = updatedPackage
        _packages.value = _packages.value.map {
            if (it.id == updatedPackage.id) updatedPackage else it
        }
        return Result.success(Unit)
    }

    override suspend fun deletePackage(packageId: String): Result<Unit> {
        if (deleteShouldFail) return Result.failure(IllegalStateException("fail"))
        lastDeletedId = packageId
        _packages.value = _packages.value.filterNot { it.id == packageId }
        return Result.success(Unit)
    }

    override suspend fun getDeliveredCount(): Int =
        _packages.value.count { it.status == PackageStatus.ENTREGADO }

    override suspend fun getPendingCount(): Int =
        _packages.value.count { it.status != PackageStatus.ENTREGADO }
}
