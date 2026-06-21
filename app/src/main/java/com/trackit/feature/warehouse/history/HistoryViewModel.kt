package com.trackit.feature.warehouse.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class HistoryViewModel(
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredPackages: StateFlow<List<Package>> = combine(
        packageRepository.packages,
        _searchQuery
    ) { allPackages, query ->
        allPackages
            .filter { it.registeredByWarehouse }
            .filter { it.clientName.contains(query, ignoreCase = true) }
            .sortedWith(
                compareBy<Package> { pkg ->
                    when (pkg.status) {
                        PackageStatus.EN_CAMINO -> 0
                        PackageStatus.CARGADO -> 1
                        PackageStatus.ASIGNADO -> 2
                        PackageStatus.EN_DEPOSITO -> 3
                        else -> 4
                    }
                }.thenByDescending { it.updatedAt ?: Instant.MIN }
            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

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
