package com.trackit.data.repository

import com.trackit.data.model.Truck
import com.trackit.data.model.TruckInsertRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SupabaseFleetRepository(
    private val supabase: SupabaseClient
) : IFleetRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _trucks = MutableStateFlow<List<Truck>>(emptyList())
    override val trucks: StateFlow<List<Truck>> = _trucks.asStateFlow()

    init {
        scope.launch {
            refreshTrucks()
        }
    }

    private suspend fun refreshTrucks() {
        try {
            val all = supabase.from("trucks").select().decodeList<Truck>()
            _trucks.value = all
        } catch (e: Exception) {
            e.printStackTrace()
            _trucks.value = emptyList()
        }
    }

    override suspend fun getActiveTruckCount(): Int {
        if (_trucks.value.isEmpty()) refreshTrucks()
        return _trucks.value.size
    }

    override suspend fun getTruckForDriver(driverId: String): Truck? = withContext(Dispatchers.IO) {
        try {
            supabase.from("trucks")
                .select {
                    filter { eq("driver_id", driverId) }
                }
                .decodeList<Truck>()
                .firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun createTruck(
        driverId: String,
        driverName: String,
        plate: String
    ): Truck? = withContext(Dispatchers.IO) {
        runCatching {
            val existing = getTruckForDriver(driverId)
            if (existing != null) return@runCatching existing

            supabase.from("trucks").insert(
                TruckInsertRow(
                    id = UUID.randomUUID().toString(),
                    driverId = driverId,
                    driverName = driverName,
                    plate = plate.trim().uppercase()
                )
            )
            refreshTrucks()
            getTruckForDriver(driverId)
        }.getOrElse {
            it.printStackTrace()
            null
        }
    }

    override suspend fun updateTruckLocation(truckId: String, lat: Double, lon: Double): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                supabase.from("trucks").update(
                    mapOf(
                        "last_lat" to lat,
                        "last_lon" to lon
                    )
                ) {
                    filter { eq("id", truckId) }
                }
                refreshTrucks()
            }
        }
}
