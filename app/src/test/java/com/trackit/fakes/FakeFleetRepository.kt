package com.trackit.fakes

import com.trackit.data.model.Truck
import com.trackit.data.repository.IFleetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFleetRepository(
    initialTrucks: List<Truck> = emptyList()
) : IFleetRepository {

    private val _trucks = MutableStateFlow(initialTrucks)
    override val trucks: StateFlow<List<Truck>> = _trucks.asStateFlow()

    override suspend fun getActiveTruckCount(): Int = _trucks.value.size

    override suspend fun getTruckForDriver(driverId: String): Truck? =
        _trucks.value.find { it.driverId == driverId }

    override suspend fun createTruck(
        driverId: String,
        driverName: String,
        plate: String,
        routeStartLat: Double,
        routeStartLon: Double,
        routeStartLabel: String?
    ): Truck? = null

    override suspend fun updateRouteStartLocation(
        truckId: String,
        lat: Double,
        lon: Double,
        label: String?
    ): Result<Unit> = Result.success(Unit)
}
