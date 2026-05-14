package com.trackit.feature.driver.route

import androidx.lifecycle.ViewModel
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

class RouteViewModel : ViewModel() {
    val packages: StateFlow<List<Package>> = PackageRepository.packages
        .map { packages -> packages.filter { it.status != PackageStatus.DELIVERED } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
