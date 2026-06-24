package com.trackit.data.repository

import com.trackit.data.model.Truck
import kotlinx.coroutines.flow.StateFlow

interface IFleetRepository {
    val trucks: StateFlow<List<Truck>>
    suspend fun getActiveTruckCount(): Int
    suspend fun getTruckForDriver(driverId: String): Truck?
    suspend fun createTruck(
        driverId: String,
        driverName: String,
        plate: String,
        routeStartLat: Double,
        routeStartLon: Double,
        routeStartLabel: String?
    ): Truck?
    suspend fun updateRouteStartLocation(
        truckId: String,
        lat: Double,
        lon: Double,
        label: String?
    ): Result<Unit>
}
