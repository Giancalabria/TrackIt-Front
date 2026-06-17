package com.trackit.feature.admin.assign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.repository.IFleetRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private val ARGENTINA_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

/** Packages already loaded onto the truck (or beyond) can't be unassigned from here. */
private fun Package.isLocked(): Boolean = when (status) {
    PackageStatus.CARGADO, PackageStatus.EN_CAMINO, PackageStatus.ENTREGADO, PackageStatus.FALLIDO -> true
    else -> false
}

data class AssignRouteUiState(
    val driverName: String = "",
    val availablePackages: List<Package> = emptyList(),
    val currentRoutePackages: List<Package> = emptyList(),
    val selectedPackageIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AssignRouteViewModel(
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository,
    private val fleetRepository: IFleetRepository = SupabaseLocator.fleetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignRouteUiState())
    val uiState: StateFlow<AssignRouteUiState> = _uiState.asStateFlow()

    private var driverId: String? = null
    private var dataJob: Job? = null

    fun initialize(id: String) {
        if (this.driverId == id) return
        this.driverId = id
        loadData(id)
    }

    private fun loadData(id: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val truck = fleetRepository.trucks.value.find { it.driverId == id }
            val driverName = truck?.driverName ?: "Chofer"

            val today = LocalDate.now(ARGENTINA_ZONE)

            packageRepository.packages.collect { allPackages ->
                // Only today's depot packages are available to assign for today's route.
                val inWarehouse = allPackages.filter {
                    it.status == PackageStatus.EN_DEPOSITO && it.scheduledDate == today
                }
                val assignedToDriver = allPackages
                    .filter { it.assignedDriverId == id && it.status != PackageStatus.ENTREGADO }
                    .sortedWith(compareBy<Package, Int?>(nullsLast()) { it.routeOrder }.thenBy { it.eta })

                _uiState.update { state ->
                    // Preserve any manual reordering while membership of the route is unchanged.
                    val sameMembership =
                        state.currentRoutePackages.map { it.id }.toSet() == assignedToDriver.map { it.id }.toSet()
                    val current = if (sameMembership && state.currentRoutePackages.isNotEmpty()) {
                        // Refresh fields (status, etc.) but keep the user's order.
                        val byId = assignedToDriver.associateBy { it.id }
                        state.currentRoutePackages.mapNotNull { byId[it.id] }
                    } else {
                        assignedToDriver
                    }

                    state.copy(
                        driverName = driverName,
                        availablePackages = inWarehouse,
                        currentRoutePackages = current,
                        selectedPackageIds = current.map { it.id }.toSet() +
                            state.selectedPackageIds.intersect(inWarehouse.map { it.id }.toSet()),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun togglePackageSelection(packageId: String) {
        _uiState.update { state ->
            // Don't allow unselecting a package that is already loaded (or beyond).
            val locked = state.currentRoutePackages.any { it.id == packageId && it.isLocked() }
            if (locked) return@update state

            val newSelection = if (state.selectedPackageIds.contains(packageId)) {
                state.selectedPackageIds - packageId
            } else {
                state.selectedPackageIds + packageId
            }
            state.copy(selectedPackageIds = newSelection)
        }
    }

    fun moveUp(packageId: String) = move(packageId, -1)
    fun moveDown(packageId: String) = move(packageId, +1)

    private fun move(packageId: String, delta: Int) {
        _uiState.update { state ->
            val list = state.currentRoutePackages.toMutableList()
            val index = list.indexOfFirst { it.id == packageId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= list.size) return@update state
            val item = list.removeAt(index)
            list.add(target, item)
            state.copy(currentRoutePackages = list)
        }
    }

    fun saveChanges() {
        val id = driverId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

            val currentState = _uiState.value
            val currentAssignedIds = currentState.currentRoutePackages.map { it.id }.toSet()
            val newSelectionIds = currentState.selectedPackageIds
            // Locked packages (CARGADO+) can never be unassigned from here.
            val lockedIds = currentState.currentRoutePackages.filter { it.isLocked() }.map { it.id }.toSet()

            val toUnassign = (currentAssignedIds - newSelectionIds) - lockedIds
            var failedCount = 0

            toUnassign.forEach { pkgId ->
                val result = packageRepository.updateStatus(pkgId, PackageStatus.EN_DEPOSITO)
                if (result.isFailure) failedCount++
            }

            val toAssign = newSelectionIds - currentAssignedIds
            if (toAssign.isNotEmpty()) {
                val result = packageRepository.assignPackagesToDriver(toAssign.toList(), id)
                if (result.isFailure) failedCount += toAssign.size
            }

            // Persist the manual visit order (route_order) for the packages staying in the route.
            val orderedKept = currentState.currentRoutePackages.filter {
                it.id in newSelectionIds && it.id !in toUnassign
            }
            orderedKept.forEachIndexed { index, pkg ->
                val desiredOrder = index + 1
                if (pkg.routeOrder != desiredOrder) {
                    val result = packageRepository.updatePackage(
                        pkg.copy(routeOrder = desiredOrder, assignedDriverId = id)
                    )
                    if (result.isFailure) failedCount++
                }
            }

            if (failedCount > 0) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudieron guardar todos los cambios. Reintentá."
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
