package com.trackit.data.repository

import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class SupabasePackageRepository(
    private val supabase: SupabaseClient
) : IPackageRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _packages = MutableStateFlow<List<Package>>(emptyList())
    override val packages: StateFlow<List<Package>> = _packages.asStateFlow()

    init {
        scope.launch {
            refreshPackages()
        }
    }

    private suspend fun refreshPackages() {
        try {
            val all = supabase.from("packages").select().decodeList<Package>()
            _packages.value = all
        } catch (e: Exception) {
            e.printStackTrace()
            _packages.value = emptyList()
        }
    }

    override suspend fun getPackageById(id: String): Package? {
        if (_packages.value.isEmpty()) refreshPackages()
        return _packages.value.firstOrNull { it.id == id }
    }

    override suspend fun getPackagesByStatus(status: PackageStatus): List<Package> {
        if (_packages.value.isEmpty()) refreshPackages()
        return _packages.value.filter { it.status == status }
    }

    override suspend fun getDriverPackages(driverId: String): List<Package> {
        if (_packages.value.isEmpty()) refreshPackages()
        return _packages.value
            .filter { it.assignedDriverId == driverId }
            .sortedBy { it.eta }
    }

    override suspend fun updateStatus(id: String, status: PackageStatus): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val updates = if (status == PackageStatus.EN_DEPOSITO) {
                mapOf("status" to status.name, "assigned_driver_id" to null)
            } else {
                mapOf("status" to status.name)
            }
            supabase.from("packages").update(updates) {
                filter { eq("id", id) }
            }
            refreshPackages()
        }
    }

    override suspend fun assignPackagesToDriver(packageIds: List<String>, driverId: String): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                packageIds.forEach { pkgId ->
                    supabase.from("packages").update(
                        mapOf(
                            "status" to PackageStatus.ASIGNADO.name,
                            "assigned_driver_id" to driverId
                        )
                    ) {
                        filter { eq("id", pkgId) }
                    }
                }
                refreshPackages()
            }
        }

    override suspend fun triggerRouteOptimization(targetDate: LocalDate): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            supabase.functions.invoke(
                "daily-route-optimizer",
                body = mapOf("targetDate" to targetDate.toString())
            )
            refreshPackages()
        }
    }

    override suspend fun addPackage(
        clientName: String,
        address: String,
        destinationLat: Double?,
        destinationLon: Double?,
        size: PackageSize,
        isFragile: Boolean,
        scheduledDate: LocalDate
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val newPackage = Package(
                id = UUID.randomUUID().toString(),
                clientName = clientName,
                address = address,
                destinationLat = destinationLat,
                destinationLon = destinationLon,
                eta = "",
                size = size,
                isFragile = isFragile,
                status = PackageStatus.EN_DEPOSITO,
                scheduledDate = scheduledDate,
                registeredByWarehouse = true
            )
            supabase.from("packages").insert(newPackage)
            refreshPackages()
        }
    }

    override suspend fun updatePackage(updatedPackage: Package): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            supabase.from("packages").update(updatedPackage) {
                filter { eq("id", updatedPackage.id) }
            }
            refreshPackages()
        }
    }

    override suspend fun deletePackage(packageId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            supabase.from("packages").delete {
                filter { eq("id", packageId) }
            }
            refreshPackages()
        }
    }

    override suspend fun getDeliveredCount(): Int {
        if (_packages.value.isEmpty()) refreshPackages()
        return _packages.value.count { it.status == PackageStatus.ENTREGADO }
    }

    override suspend fun getPendingCount(): Int {
        if (_packages.value.isEmpty()) refreshPackages()
        return _packages.value.count { it.status != PackageStatus.ENTREGADO }
    }
}
