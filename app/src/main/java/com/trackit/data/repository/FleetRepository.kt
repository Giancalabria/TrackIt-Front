package com.trackit.data.repository

import com.trackit.data.model.Truck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FleetRepository {
    private val _trucks = MutableStateFlow(
        listOf(
            Truck(
                id = "TRK-01",
                driverName = "Juan Chofer",
                plate = "AB123CD",
                deliveredCount = 15,
                totalCount = 20
            ),
            Truck(
                id = "TRK-02",
                driverName = "Pedro Sosa",
                plate = "AC456EF",
                deliveredCount = 8,
                totalCount = 18
            ),
            Truck(
                id = "TRK-03",
                driverName = "Lucía Torres",
                plate = "AD789GH",
                deliveredCount = 12,
                totalCount = 16
            ),
            Truck(
                id = "TRK-04",
                driverName = "Marcos Vega",
                plate = "AE321IJ",
                deliveredCount = 5,
                totalCount = 14
            )
        )
    )

    val trucks: StateFlow<List<Truck>> = _trucks.asStateFlow()

    fun getActiveTruckCount(): Int = _trucks.value.size
}
