package com.trackit.data.repository

import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.RouteOptimizationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface IPackageRepository {
    val packages: StateFlow<List<Package>>
    suspend fun getPackageById(id: String): Package?
    suspend fun getPackageByIdFlow(id: String): Flow<Package?>
    suspend fun getPackagesByStatus(status: PackageStatus): List<Package>
    suspend fun getDriverPackages(driverId: String): List<Package>
    suspend fun updateStatus(id: String, status: PackageStatus): Result<Unit>
    suspend fun assignPackagesToDriver(packageIds: List<String>, driverId: String): Result<Unit>
    suspend fun triggerRouteOptimization(targetDate: LocalDate): Result<RouteOptimizationResult>
    suspend fun addPackage(
        clientName: String,
        address: String,
        destinationLat: Double?,
        destinationLon: Double?,
        size: PackageSize,
        isFragile: Boolean,
        scheduledDate: LocalDate,
        barcode: String
    ): Result<Unit>
    suspend fun updatePackage(updatedPackage: Package): Result<Unit>
    suspend fun deletePackage(packageId: String): Result<Unit>
    suspend fun getDeliveredCount(): Int
    suspend fun getPendingCount(): Int
}
