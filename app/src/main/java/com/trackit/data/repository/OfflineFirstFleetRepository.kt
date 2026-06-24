package com.trackit.data.repository

import android.content.Context
import com.trackit.data.local.TrackItDatabase
import com.trackit.data.local.mapper.toDomain
import com.trackit.data.local.mapper.toEntity
import com.trackit.data.model.Truck
import com.trackit.data.sync.SyncScheduler
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.util.UUID

class OfflineFirstFleetRepository(
    private val supabase: SupabaseClient,
    private val database: TrackItDatabase,
    private val context: Context
) : IFleetRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override val trucks: StateFlow<List<Truck>> = database.truckDao().observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun getActiveTruckCount(): Int {
        return trucks.value.size
    }

    override suspend fun getTruckForDriver(driverId: String): Truck? {
        return database.truckDao().getByDriverId(driverId)?.toDomain()
    }

    override suspend fun createTruck(
        driverId: String,
        driverName: String,
        plate: String,
        routeStartLat: Double,
        routeStartLon: Double,
        routeStartLabel: String?
    ): Truck? {
        val normalizedPlate = plate.trim().uppercase()
        val normalizedLabel = routeStartLabel?.trim()?.takeIf { it.isNotEmpty() }
        val now = Instant.now()

        // Deduplicate by driver: a driver owns at most one truck. If one already exists,
        // update its plate/name/route start instead of creating a duplicate row.
        val existing = database.truckDao().getByDriverId(driverId)?.toDomain()
        val truck = if (existing != null) {
            existing.copy(
                plate = normalizedPlate,
                driverName = driverName,
                routeStartLat = routeStartLat,
                routeStartLon = routeStartLon,
                routeStartLabel = normalizedLabel,
                routeStartUpdatedAt = now
            )
        } else {
            Truck(
                id = UUID.randomUUID().toString(),
                driverId = driverId,
                driverName = driverName,
                plate = normalizedPlate,
                routeStartLat = routeStartLat,
                routeStartLon = routeStartLon,
                routeStartLabel = normalizedLabel,
                routeStartUpdatedAt = now
            )
        }
        database.truckDao().upsert(truck.toEntity(pendingSync = true))
        SyncScheduler.enqueue(context)
        return truck
    }

    override suspend fun updateRouteStartLocation(
        truckId: String,
        lat: Double,
        lon: Double,
        label: String?
    ): Result<Unit> {
        return runCatching {
            val entity = database.truckDao().getById(truckId)
                ?: return Result.failure(Exception("Truck not found"))
            val updated = entity.toDomain().copy(
                routeStartLat = lat,
                routeStartLon = lon,
                routeStartLabel = label?.trim()?.takeIf { it.isNotEmpty() },
                routeStartUpdatedAt = Instant.now()
            )
            database.truckDao().upsert(updated.toEntity(pendingSync = true))
            SyncScheduler.enqueue(context)
        }
    }
}
