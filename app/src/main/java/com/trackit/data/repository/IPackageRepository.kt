package com.trackit.data.repository

import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface IPackageRepository {
    val packages: StateFlow<List<Package>>
    suspend fun getPackageById(id: String): Package?
    suspend fun getPackagesByStatus(status: PackageStatus): List<Package>
    suspend fun getDriverPackages(driverId: String): List<Package>
    suspend fun updateStatus(id: String, status: PackageStatus): Result<Unit>
    suspend fun assignPackagesToDriver(packageIds: List<String>, driverId: String): Result<Unit>
    suspend fun loadPackagesOntoTruck(packageIds: List<String>, driverId: String): Result<Unit>
    suspend fun triggerRouteOptimization(targetDate: LocalDate): Result<Unit>
    suspend fun addPackage(
        barcode: String,
        clientName: String,
        address: String,
        destinationLat: Double?,
        destinationLon: Double?,
        size: PackageSize,
        isFragile: Boolean,
        scheduledDate: LocalDate
    ): Result<Unit>
    suspend fun updatePackage(updatedPackage: Package): Result<Unit>
    suspend fun deletePackage(packageId: String): Result<Unit>
    suspend fun getDeliveredCount(): Int
    suspend fun getPendingCount(): Int
}
