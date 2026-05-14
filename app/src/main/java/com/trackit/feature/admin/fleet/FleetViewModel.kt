package com.trackit.feature.admin.fleet

import androidx.lifecycle.ViewModel
import com.trackit.data.model.Truck
import com.trackit.data.repository.FleetRepository
import kotlinx.coroutines.flow.StateFlow

class FleetViewModel : ViewModel() {
    val trucks: StateFlow<List<Truck>> = FleetRepository.trucks
}
