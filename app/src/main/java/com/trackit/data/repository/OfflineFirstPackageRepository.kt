package com.trackit.data.repository

import android.content.Context
import com.trackit.data.local.TrackItDatabase
import com.trackit.data.local.mapper.toDomain
import com.trackit.data.local.mapper.toEntity
import com.trackit.data.model.Package
import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.RouteOptimizationResult
import com.trackit.data.sync.SyncScheduler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class OfflineFirstPackageRepository(
    private val supabase: SupabaseClient,
    private val database: TrackItDatabase,
    private val context: Context
) : IPackageRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override val packages: StateFlow<List<Package>> = database.packageDao().observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun getPackageById(id: String): Package? {
        return database.packageDao().getById(id)?.toDomain()
    }

    override suspend fun getPackageByIdFlow(id: String): Flow<Package?> {
        return database.packageDao().observeById(id).map { it?.toDomain() }
    }

    override suspend fun getPackagesByStatus(status: PackageStatus): List<Package> {
        // This would ideally be a DAO query, but for simplicity:
        return packages.value.filter { it.status == status }
    }

    override suspend fun getDriverPackages(driverId: String): List<Package> {
        // Show the driver's stops in the optimizer's visit order (routeOrder asc,
        // packages without an order go last), then by eta as a tiebreaker.
        return packages.value
            .filter { it.assignedDriverId == driverId }
            .sortedWith(
                compareBy<Package, Int?>(nullsLast()) { it.routeOrder }
                    .thenBy { it.eta }
            )
    }

    override suspend fun updateStatus(id: String, status: PackageStatus): Result<Unit> {
        return runCatching {
            val entity = database.packageDao().getById(id)
                ?: throw IllegalStateException("Paquete no encontrado: $id")
            val current = entity.toDomain()
            val updated = current.copy(
                status = status,
                // Moving back to depot frees the driver assignment.
                assignedDriverId = if (status == PackageStatus.EN_DEPOSITO) null else current.assignedDriverId,
                // Stamp delivery time so we can derive truck location + metrics.
                deliveredAt = if (status == PackageStatus.ENTREGADO) Instant.now() else current.deliveredAt
            )
            database.packageDao().upsert(updated.toEntity(pendingSync = true))

            // On delivery, derive the truck's last known location from the delivered package
            // (NOT realtime: it's just "where the truck last dropped something, and when").
            if (status == PackageStatus.ENTREGADO) {
                val driverId = current.assignedDriverId
                val lat = current.destinationLat
                val lon = current.destinationLon
                if (driverId != null && lat != null && lon != null) {
                    database.truckDao().getByDriverId(driverId)?.let { truckEntity ->
                        val updatedTruck = truckEntity.toDomain().copy(
                            lastLat = lat,
                            lastLon = lon,
                            lastLocationAt = Instant.now()
                        )
                        database.truckDao().upsert(updatedTruck.toEntity(pendingSync = true))
                    }
                }
            }

            SyncScheduler.enqueue(context)
        }
    }

    override suspend fun assignPackagesToDriver(packageIds: List<String>, driverId: String): Result<Unit> {
        return runCatching {
            packageIds.forEach { id ->
                val entity = database.packageDao().getById(id)
                if (entity != null) {
                    val domain = entity.toDomain().copy(assignedDriverId = driverId)
                    database.packageDao().upsert(domain.toEntity(pendingSync = true))
                }
            }
            SyncScheduler.enqueue(context)
        }
    }

    override suspend fun triggerRouteOptimization(targetDate: LocalDate): Result<RouteOptimizationResult> {
        return runCatching {
            val response = supabase.invokeRouteOptimizer(targetDate.toString())
            // Pull the freshly-assigned packages into Room.
            SyncScheduler.enqueue(context)
            RouteOptimizationResult(
                ok = response.ok,
                targetDate = response.targetDate.ifBlank { targetDate.toString() },
                assigned = response.assigned,
                unassigned = response.unassigned,
                reason = response.reason,
                error = response.error
            )
        }
    }

    override suspend fun addPackage(
        clientName: String,
        address: String,
        destinationLat: Double?,
        destinationLon: Double?,
        size: PackageSize,
        isFragile: Boolean,
        scheduledDate: LocalDate,
        barcode: String
    ): Result<Unit> {
        return runCatching {
            val newPackage = Package(
                id = UUID.randomUUID().toString(),
                clientName = clientName,
                address = address,
                destinationLat = destinationLat,
                destinationLon = destinationLon,
                size = size,
                isFragile = isFragile,
                scheduledDate = scheduledDate,
                status = PackageStatus.EN_DEPOSITO,
                barcode = barcode,
                registeredByWarehouse = true,
                eta = "" // Placeholder for eta
            )
            database.packageDao().upsert(newPackage.toEntity(pendingSync = true))
            SyncScheduler.enqueue(context)
        }
    }

    override suspend fun updatePackage(updatedPackage: Package): Result<Unit> {
        return runCatching {
            database.packageDao().upsert(updatedPackage.toEntity(pendingSync = true))
            SyncScheduler.enqueue(context)
        }
    }

    override suspend fun deletePackage(packageId: String): Result<Unit> {
        return runCatching {
            val entity = database.packageDao().getById(packageId)
            if (entity != null) {
                database.packageDao().upsert(entity.copy(deletedLocally = true, pendingSync = true))
                SyncScheduler.enqueue(context)
            }
        }
    }

    override suspend fun getDeliveredCount(): Int {
        return packages.value.count { it.status == PackageStatus.ENTREGADO }
    }

    override suspend fun getPendingCount(): Int {
        return packages.value.count { it.status != PackageStatus.ENTREGADO }
    }
}
