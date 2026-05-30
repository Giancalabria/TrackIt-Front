package com.trackit.data.repository

import com.trackit.data.model.Truck
import kotlinx.coroutines.flow.StateFlow

interface IFleetRepository {
    val trucks: StateFlow<List<Truck>>
    suspend fun getActiveTruckCount(): Int
    suspend fun getTruckForDriver(driverId: String): Truck?
    suspend fun createTruck(driverId: String, driverName: String, plate: String): Truck?
    suspend fun updateTruckLocation(truckId: String, lat: Double, lon: Double): Result<Unit>
}
