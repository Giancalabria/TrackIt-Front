package com.trackit.feature.warehouse.history

import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import java.time.LocalDate

data class HistoryFilters(
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

fun HistoryFilters.activeSummary(): String {
    if (!isActive) return ""
    val parts = mutableListOf<String>()
    if (dateFrom != null || dateTo != null) {
        parts += when {
            dateFrom != null && dateTo != null -> "${dateFrom} — ${dateTo}"
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

val warehouseFilterableStatuses: List<PackageStatus> = listOf(
    PackageStatus.EN_DEPOSITO,
    PackageStatus.ASIGNADO,
    PackageStatus.CARGADO,
    PackageStatus.EN_CAMINO,
    PackageStatus.ENTREGADO,
    PackageStatus.FALLIDO
)
