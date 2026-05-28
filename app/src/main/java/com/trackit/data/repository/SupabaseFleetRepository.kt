package com.trackit.data.repository

import com.trackit.data.model.Truck
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
        val all = supabase.from("trucks").select().decodeList<Truck>()
        _trucks.value = all
    }

    override suspend fun getActiveTruckCount(): Int {
        if (_trucks.value.isEmpty()) refreshTrucks()
        return _trucks.value.size
    }

    override suspend fun updateTruckLocation(truckId: String, lat: Double, lon: Double) {
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

