package com.trackit.feature.warehouse.history

import com.trackit.core.ui.filters.PackageFilters
import com.trackit.data.model.PackageStatus
import com.trackit.testutil.samplePackage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PackageFiltersTest {

    @Test
    fun matches_emptyFilters_acceptsAll() {
        val filters = PackageFilters()
        val pkg = samplePackage()
        assertTrue(filters.matches(pkg))
    }

    @Test
    fun matches_multipleStatuses_acceptsAnySelected() {
        val filters = PackageFilters(
            statuses = setOf(PackageStatus.ASIGNADO, PackageStatus.CARGADO)
        )
        assertTrue(filters.matches(samplePackage(status = PackageStatus.ASIGNADO)))
        assertTrue(filters.matches(samplePackage(status = PackageStatus.CARGADO)))
        assertFalse(filters.matches(samplePackage(status = PackageStatus.EN_DEPOSITO)))
    }

    @Test
    fun matches_dateRange_filtersByScheduledDate() {
        val filters = PackageFilters(
            dateFrom = LocalDate.of(2026, 6, 10),
            dateTo = LocalDate.of(2026, 6, 20)
        )
        val inside = samplePackage().copy(scheduledDate = LocalDate.of(2026, 6, 15))
        val outside = samplePackage().copy(scheduledDate = LocalDate.of(2026, 6, 25))
        assertTrue(filters.matches(inside))
        assertFalse(filters.matches(outside))
    }
}
