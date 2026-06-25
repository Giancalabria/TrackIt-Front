package com.trackit.feature.warehouse.loadtruck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.core.ui.filters.PackageFilterUiState
import com.trackit.core.ui.filters.PackageFilters
import com.trackit.core.ui.filters.filterBySearchAndFilters
import com.trackit.core.ui.filters.loadTruckVisibleStatuses
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.Truck
import com.trackit.data.model.canLoadOntoTruck
import com.trackit.data.model.isAlreadyLoadedOrBeyond
import com.trackit.data.model.matchesCode
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private val ARGENTINA_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

enum class LoadTruckStep {
    SELECT_TRUCK,
    LOADING,
    PACKAGE_DETAIL
}

data class TruckLoadInfo(
    val truck: Truck,
    val pendingCount: Int,
    val loadedCount: Int
) {
    val totalCount: Int get() = pendingCount + loadedCount
}

data class LoadTruckUiState(
    val step: LoadTruckStep = LoadTruckStep.SELECT_TRUCK,
    val trucks: List<TruckLoadInfo> = emptyList(),
    val selectedTruck: Truck? = null,
    val selectedPackage: Package? = null,
    val pendingPackages: List<Package> = emptyList(),
    val loadedPackages: List<Package> = emptyList(),
    val searchQuery: String = "",
    val filterUiState: PackageFilterUiState = PackageFilterUiState(),
    val isScannerOpen: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

class LoadTruckViewModel(
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository,
    private val fleetRepository: IFleetRepository = SupabaseLocator.fleetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoadTruckUiState())
    val uiState: StateFlow<LoadTruckUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _step = MutableStateFlow(LoadTruckStep.SELECT_TRUCK)
    private val _selectedTruck = MutableStateFlow<Truck?>(null)
    private val _filterUiState = MutableStateFlow(PackageFilterUiState())
    private var allPackages: List<Package> = emptyList()

    init {
        combine(
            combine(
                packageRepository.packages,
                fleetRepository.trucks,
                _searchQuery
            ) { packages, trucks, query ->
                Triple(packages, trucks, query)
            },
            combine(_step, _selectedTruck, _filterUiState) { step, selectedTruck, filterUiState ->
                Triple(step, selectedTruck, filterUiState)
            }
        ) { (packages, trucks, query), (step, selectedTruck, filterUiState) ->
            allPackages = packages
            val today = LocalDate.now(ARGENTINA_ZONE)

            val truckInfos = trucks.map { truck ->
                val driverPackages = packages.filter {
                    it.assignedDriverId == truck.driverId &&
                        it.scheduledDate == today &&
                        (it.status in loadTruckVisibleStatuses || it.status == PackageStatus.EN_DEPOSITO)
                }
                TruckLoadInfo(
                    truck = truck,
                    pendingCount = driverPackages.count { 
                        it.status == PackageStatus.ASIGNADO || it.status == PackageStatus.EN_DEPOSITO 
                    },
                    loadedCount = driverPackages.count {
                        it.status == PackageStatus.CARGADO || it.status == PackageStatus.EN_CAMINO
                    }
                )
            }.sortedByDescending { it.pendingCount }

            val (pending, loaded) = if (selectedTruck != null && step == LoadTruckStep.LOADING) {
                buildFilteredLists(
                    packages = packages,
                    truck = selectedTruck,
                    today = today,
                    query = query,
                    filters = filterUiState.applied
                )
            } else {
                emptyList<Package>() to emptyList()
            }

            DataSnapshot(
                trucks = truckInfos,
                step = step,
                selectedTruck = selectedTruck,
                pendingPackages = pending,
                loadedPackages = loaded,
                searchQuery = query,
                filterUiState = filterUiState
            )
        }
            .onEach { snapshot ->
                _uiState.update { current ->
                    current.copy(
                        trucks = snapshot.trucks,
                        step = snapshot.step,
                        selectedTruck = snapshot.selectedTruck,
                        pendingPackages = snapshot.pendingPackages,
                        loadedPackages = snapshot.loadedPackages,
                        searchQuery = snapshot.searchQuery,
                        filterUiState = snapshot.filterUiState
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private data class DataSnapshot(
        val trucks: List<TruckLoadInfo>,
        val step: LoadTruckStep,
        val selectedTruck: Truck?,
        val pendingPackages: List<Package>,
        val loadedPackages: List<Package>,
        val searchQuery: String,
        val filterUiState: PackageFilterUiState
    )

    private fun buildFilteredLists(
        packages: List<Package>,
        truck: Truck,
        today: LocalDate,
        query: String,
        filters: PackageFilters
    ): Pair<List<Package>, List<Package>> {
        val eligible = packages.filter {
            it.assignedDriverId == truck.driverId &&
                it.scheduledDate == today &&
                (it.status in loadTruckVisibleStatuses || it.status == PackageStatus.EN_DEPOSITO)
        }
        val filtered = eligible.filterBySearchAndFilters(query, filters)
        val pending = filtered
            .filter { it.status == PackageStatus.ASIGNADO || it.status == PackageStatus.EN_DEPOSITO }
            .sortedWith(compareBy<Package, Int?>(nullsLast()) { it.routeOrder }.thenBy { it.eta })
        val loaded = filtered
            .filter { it.status == PackageStatus.CARGADO || it.status == PackageStatus.EN_CAMINO }
            .sortedWith(compareBy<Package, Int?>(nullsLast()) { it.routeOrder }.thenBy { it.eta })
        return pending to loaded
    }

    fun selectTruck(truck: Truck) {
        _searchQuery.value = ""
        _filterUiState.value = PackageFilterUiState()
        _selectedTruck.value = truck
        _step.value = LoadTruckStep.LOADING
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectPackage(pkg: Package) {
        _step.value = LoadTruckStep.PACKAGE_DETAIL
        _uiState.update { it.copy(selectedPackage = pkg, errorMessage = null) }
    }

    fun backToLoading() {
        _step.value = LoadTruckStep.LOADING
        _uiState.update { it.copy(selectedPackage = null, errorMessage = null) }
    }

    fun backToTruckSelection() {
        _searchQuery.value = ""
        _filterUiState.value = PackageFilterUiState()
        _selectedTruck.value = null
        _step.value = LoadTruckStep.SELECT_TRUCK
        _uiState.update { it.copy(isScannerOpen = false, errorMessage = null, selectedPackage = null) }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openFilterSheet() {
        _filterUiState.update {
            it.copy(showSheet = true, draft = it.applied)
        }
    }

    fun dismissFilterSheet() {
        _filterUiState.update { it.copy(showSheet = false) }
    }

    fun toggleDraftStatus(status: PackageStatus) {
        _filterUiState.update { state ->
            val nextStatuses = state.draft.statuses.toMutableSet()
            if (status in nextStatuses) nextStatuses.remove(status) else nextStatuses.add(status)
            state.copy(draft = state.draft.copy(statuses = nextStatuses))
        }
    }

    fun applyFilters() {
        _filterUiState.update {
            it.copy(applied = it.draft, showSheet = false)
        }
    }

    fun clearFilters() {
        _filterUiState.value = PackageFilterUiState(showSheet = false)
    }

    fun openScanner() {
        _uiState.update { it.copy(isScannerOpen = true, errorMessage = null) }
    }

    fun closeScanner() {
        _uiState.update { it.copy(isScannerOpen = false) }
    }

    fun onBarcodeScanned(code: String) {
        val selectedPkg = _uiState.value.selectedPackage ?: return
        _uiState.update { it.copy(isScannerOpen = false) }

        if (selectedPkg.matchesCode(code)) {
            loadPackage(selectedPkg)
        } else {
            _uiState.update { it.copy(errorMessage = "El código escaneado no coincide con este paquete.") }
        }
    }

    private fun loadPackage(pkg: Package) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = packageRepository.updatePackage(pkg.copy(status = PackageStatus.CARGADO))
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudo cargar el paquete. Reintentá."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "${pkg.clientName} cargado correctamente.",
                        step = LoadTruckStep.LOADING,
                        selectedPackage = null
                    )
                }
            }
        }
    }

    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
