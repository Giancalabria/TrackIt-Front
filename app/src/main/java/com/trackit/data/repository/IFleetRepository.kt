package com.trackit.data.repository

import com.trackit.data.model.Truck
import kotlinx.coroutines.flow.StateFlow

interface IFleetRepository {
    val trucks: StateFlow<List<Truck>>
    suspend fun getActiveTruckCount(): Int
}
