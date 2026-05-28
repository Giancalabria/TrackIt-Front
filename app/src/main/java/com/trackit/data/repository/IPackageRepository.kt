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
    suspend fun updateStatus(id: String, status: PackageStatus)
    suspend fun assignPackagesToDriver(packageIds: List<String>, driverId: String)
    suspend fun simulateDailyCronJob(currentDate: LocalDate, driverIds: List<String>)
    suspend fun addPackage(
        clientName: String,
        address: String,
        destinationLat: Double?,
        destinationLon: Double?,
        size: PackageSize,
        isFragile: Boolean,
        scheduledDate: LocalDate
    )
    suspend fun updatePackage(updatedPackage: Package)
    suspend fun deletePackage(packageId: String)
    suspend fun getDeliveredCount(): Int
    suspend fun getPendingCount(): Int
}
