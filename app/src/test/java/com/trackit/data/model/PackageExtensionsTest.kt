package com.trackit.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageExtensionsTest {

    @Test
    fun isEditableByWarehouse_trueForDepotAndAssigned() {
        assertTrue(samplePackage(status = PackageStatus.EN_DEPOSITO).isEditableByWarehouse())
        assertTrue(samplePackage(status = PackageStatus.ASIGNADO).isEditableByWarehouse())
    }

    @Test
    fun isEditableByWarehouse_falseForInTransitOrDelivered() {
        assertFalse(samplePackage(status = PackageStatus.CARGADO).isEditableByWarehouse())
        assertFalse(samplePackage(status = PackageStatus.EN_CAMINO).isEditableByWarehouse())
        assertFalse(samplePackage(status = PackageStatus.ENTREGADO).isEditableByWarehouse())
    }

    private fun samplePackage(status: PackageStatus) = Package(
        id = "1",
        clientName = "Test",
        address = "Addr",
        eta = "",
        size = PackageSize.SMALL,
        isFragile = false,
        status = status
    )
}
