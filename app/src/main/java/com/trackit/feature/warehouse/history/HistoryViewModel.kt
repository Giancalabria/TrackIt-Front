package com.trackit.feature.warehouse.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.PackageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val packageRepository: IPackageRepository = PackageRepository.getInstance()
) : ViewModel() {
    val packages: StateFlow<List<Package>> = packageRepository.packages
        .map { packages -> packages.filter { it.registeredByWarehouse } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun updatePackage(pkg: Package) {
        viewModelScope.launch {
            packageRepository.updatePackage(pkg)
        }
    }

    fun deletePackage(packageId: String) {
        viewModelScope.launch {
            packageRepository.deletePackage(packageId)
        }
    }
}
