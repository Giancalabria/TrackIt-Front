package com.trackit.core.ui.filters

import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import java.time.LocalDate

data class PackageFilters(
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val statuses: Set<PackageStatus> = emptySet()
) {
    val isActive: Boolean
        get() = dateFrom != null || dateTo != null || statuses.isNotEmpty()

    fun matches(pkg: Package): Boolean {
        dateFrom?.let { from ->
            if (pkg.scheduledDate.isBefore(from)) return false
        }
        dateTo?.let { to ->
            if (pkg.scheduledDate.isAfter(to)) return false
        }
        if (statuses.isNotEmpty() && pkg.status !in statuses) return false
        return true
    }
}

fun PackageFilters.activeSummary(): String {
    if (!isActive) return ""
    val parts = mutableListOf<String>()
    if (dateFrom != null || dateTo != null) {
        parts += when {
            dateFrom != null && dateTo != null -> "$dateFrom — $dateTo"
            dateFrom != null -> "desde $dateFrom"
            else -> "hasta $dateTo"
        }
    }
    if (statuses.isNotEmpty()) {
        parts += statuses.joinToString(", ") { it.toFilterLabel() }
    }
    return parts.joinToString(" · ")
}

fun PackageStatus.toFilterLabel(): String = when (this) {
    PackageStatus.EN_DEPOSITO -> "En depósito"
    PackageStatus.ASIGNADO -> "Asignado"
    PackageStatus.CARGADO -> "Cargado"
    PackageStatus.EN_CAMINO -> "En camino"
    PackageStatus.ENTREGADO -> "Entregado"
    PackageStatus.FALLIDO -> "Fallido"
}

val historyFilterableStatuses: List<PackageStatus> = listOf(
    PackageStatus.EN_DEPOSITO,
    PackageStatus.ASIGNADO,
    PackageStatus.CARGADO,
    PackageStatus.EN_CAMINO,
    PackageStatus.ENTREGADO,
    PackageStatus.FALLIDO
)

/** Statuses visible on the load-truck screen (assigned or loaded, not yet delivered). */
val loadTruckFilterableStatuses: List<PackageStatus> = listOf(
    PackageStatus.ASIGNADO,
    PackageStatus.CARGADO,
    PackageStatus.EN_CAMINO
)

val loadTruckVisibleStatuses: Set<PackageStatus> = loadTruckFilterableStatuses.toSet()

fun List<Package>.filterBySearchAndFilters(
    query: String,
    filters: PackageFilters
): List<Package> = filter { pkg ->
    pkg.clientName.contains(query, ignoreCase = true) && filters.matches(pkg)
}

data class PackageFilterUiState(
    val showSheet: Boolean = false,
    val draft: PackageFilters = PackageFilters(),
    val applied: PackageFilters = PackageFilters()
)

data class PackageFilterSheetConfig(
    val title: String,
    val subtitle: String,
    val showDateFilters: Boolean,
    val availableStatuses: List<PackageStatus>
) {
    companion object {
        fun history() = PackageFilterSheetConfig(
            title = "Filtrar historial",
            subtitle = "Elegí criterios y tocá «Aplicar filtros».",
            showDateFilters = true,
            availableStatuses = historyFilterableStatuses
        )

        fun loadTruck() = PackageFilterSheetConfig(
            title = "Filtrar paquetes",
            subtitle = "Mostrá solo asignados o cargados (sin entregar).",
            showDateFilters = false,
            availableStatuses = loadTruckFilterableStatuses
        )
    }
}
