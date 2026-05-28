package com.trackit.data.repository

import com.trackit.data.model.Truck
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FleetRepository : IFleetRepository {
    private val _trucks = MutableStateFlow(
        listOf(
            Truck(
                id = "TRK-01",
                driverId = "USR-001",
                driverName = "Juan Chofer",
                plate = "AB123CD",
                lastLat = -34.6039,
                lastLon = -58.3923,
                deliveredCount = 15,
                totalCount = 20
            ),
            Truck(
                id = "TRK-02",
                driverId = "USR-004", // Hypothetical other driver
                driverName = "Pedro Sosa",
                plate = "AC456EF",
                lastLat = -34.5964,
                lastLon = -58.4089,
                deliveredCount = 8,
                totalCount = 18
            ),
            Truck(
                id = "TRK-03",
                driverId = "USR-005",
                driverName = "Lucía Torres",
                plate = "AD789GH",
                lastLat = -34.6124,
                lastLon = -58.3708,
                deliveredCount = 12,
                totalCount = 16
            ),
            Truck(
                id = "TRK-04",
                driverId = "USR-006",
                driverName = "Marcos Vega",
                plate = "AE321IJ",
                lastLat = -34.5882,
                lastLon = -58.4390,
                deliveredCount = 5,
                totalCount = 14
            )
        )
    )

    override val trucks: StateFlow<List<Truck>> = _trucks.asStateFlow()

    override suspend fun getActiveTruckCount(): Int {
        delay(300)
        return _trucks.value.size
    }

    companion object {
        private var instance: FleetRepository? = null
        fun getInstance(): FleetRepository {
            if (instance == null) {
                instance = FleetRepository()
            }
            return instance!!
        }
    }
}
