package com.trackit.feature.warehouse.history

import com.trackit.data.model.PackageStatus
import com.trackit.fakes.FakePackageRepository
import com.trackit.testutil.MainDispatcherRule
import com.trackit.testutil.samplePackage
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startEditing_editablePackage_opensForm() = runTest {
        val pkg = samplePackage(status = PackageStatus.EN_DEPOSITO)
        val viewModel = HistoryViewModel(FakePackageRepository(listOf(pkg)))

        viewModel.startEditing(pkg)

        assertEquals(pkg.id, viewModel.editForm.value?.packageId)
        assertEquals(pkg.clientName, viewModel.editForm.value?.clientName)
    }

    @Test
    fun startEditing_deliveredPackage_showsFeedback() = runTest {
        val pkg = samplePackage(status = PackageStatus.ENTREGADO)
        val viewModel = HistoryViewModel(FakePackageRepository(listOf(pkg)))

        viewModel.startEditing(pkg)

        assertNull(viewModel.editForm.value)
        assertEquals(
            "Solo podés editar paquetes en depósito o asignados.",
            viewModel.feedbackMessage.value
        )
    }

    @Test
    fun saveEdit_withValidData_updatesRepository() = runTest {
        val pkg = samplePackage(status = PackageStatus.ASIGNADO)
        val repository = FakePackageRepository(listOf(pkg))
        val viewModel = HistoryViewModel(repository)
        viewModel.startEditing(pkg)
        viewModel.onEditClientNameChange("María López")
        viewModel.onEditBarcodeChange("XYZ999")

        viewModel.saveEdit()

        assertNull(viewModel.editForm.value)
        assertEquals("María López", repository.lastUpdatedPackage?.clientName)
        assertEquals("XYZ999", repository.lastUpdatedPackage?.barcode)
        assertEquals("Paquete actualizado.", viewModel.feedbackMessage.value)
    }

    @Test
    fun saveEdit_withEmptyFields_showsValidationError() = runTest {
        val pkg = samplePackage()
        val viewModel = HistoryViewModel(FakePackageRepository(listOf(pkg)))
        viewModel.startEditing(pkg)
        viewModel.onEditClientNameChange("")

        viewModel.saveEdit()

        assertNotNull(viewModel.editForm.value)
        assertEquals("Completá cliente y dirección.", viewModel.editForm.value?.errorMessage)
    }

    @Test
    fun confirmDelete_editablePackage_removesFromRepository() = runTest {
        val pkg = samplePackage()
        val repository = FakePackageRepository(listOf(pkg))
        val viewModel = HistoryViewModel(repository)

        viewModel.requestDelete(pkg)
        viewModel.confirmDelete()

        assertEquals(pkg.id, repository.lastDeletedId)
        assertEquals("Paquete eliminado.", viewModel.feedbackMessage.value)
    }

    @Test
    fun requestDelete_deliveredPackage_showsFeedback() = runTest {
        val pkg = samplePackage(status = PackageStatus.ENTREGADO)
        val viewModel = HistoryViewModel(FakePackageRepository(listOf(pkg)))

        viewModel.requestDelete(pkg)

        assertNull(viewModel.packageToDelete.value)
        assertEquals(
            "Solo podés eliminar paquetes en depósito o asignados.",
            viewModel.feedbackMessage.value
        )
    }

    @Test
    fun applyFilters_onlyShowsMatchingStatuses() = runTest {
        val assigned = samplePackage(id = "a", status = PackageStatus.ASIGNADO)
        val loaded = samplePackage(id = "b", status = PackageStatus.CARGADO, clientName = "Otro")
        val viewModel = HistoryViewModel(FakePackageRepository(listOf(assigned, loaded)))

        viewModel.toggleDraftStatus(PackageStatus.ASIGNADO)
        viewModel.toggleDraftStatus(PackageStatus.CARGADO)
        viewModel.applyFilters()
        advanceUntilIdle()

        val ids = viewModel.filteredPackages.value.map { it.id }.toSet()
        assertEquals(setOf("a", "b"), ids)
    }

    @Test
    fun applyFilters_doesNotFilterUntilApplied() = runTest {
        val assigned = samplePackage(id = "a", status = PackageStatus.ASIGNADO)
        val depot = samplePackage(id = "b", status = PackageStatus.EN_DEPOSITO)
        val viewModel = HistoryViewModel(FakePackageRepository(listOf(assigned, depot)))

        viewModel.toggleDraftStatus(PackageStatus.ASIGNADO)
        advanceUntilIdle()

        assertEquals(2, viewModel.filteredPackages.value.size)

        viewModel.applyFilters()
        advanceUntilIdle()

        assertEquals(1, viewModel.filteredPackages.value.size)
        assertEquals("a", viewModel.filteredPackages.value.first().id)
    }
}
