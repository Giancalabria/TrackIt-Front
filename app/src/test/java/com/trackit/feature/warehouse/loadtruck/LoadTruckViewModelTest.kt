package com.trackit.feature.warehouse.loadtruck

import com.trackit.data.model.PackageStatus
import com.trackit.data.model.Truck
import com.trackit.fakes.FakeFleetRepository
import com.trackit.fakes.FakePackageRepository
import com.trackit.testutil.MainDispatcherRule
import com.trackit.testutil.samplePackage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private val ARGENTINA_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

class LoadTruckViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now(ARGENTINA_ZONE)

    private val truckA = Truck(
        id = "truck-a",
        driverId = "driver-a",
        driverName = "Juan",
        plate = "ABC123"
    )

    private val truckB = Truck(
        id = "truck-b",
        driverId = "driver-b",
        driverName = "Pedro",
        plate = "XYZ789"
    )

    @Test
    fun selectTruck_showsOnlyAssignedPackagesForDriver() = runTest {
        val assigned = samplePackage(
            id = "p1",
            status = PackageStatus.ASIGNADO,
            barcode = "CODE1"
        ).copy(assignedDriverId = "driver-a", scheduledDate = today)
        val other = samplePackage(
            id = "p2",
            status = PackageStatus.ASIGNADO,
            clientName = "Otro"
        ).copy(assignedDriverId = "driver-b", scheduledDate = today)
        val depot = samplePackage(id = "p3", status = PackageStatus.EN_DEPOSITO)
            .copy(scheduledDate = today)

        val viewModel = LoadTruckViewModel(
            packageRepository = FakePackageRepository(listOf(assigned, other, depot)),
            fleetRepository = FakeFleetRepository(listOf(truckA, truckB))
        )

        viewModel.selectTruck(truckA)
        val state = viewModel.uiState.first { it.step == LoadTruckStep.LOADING }

        assertEquals(1, state.pendingPackages.size)
        assertEquals("p1", state.pendingPackages.first().id)
    }

    @Test
    fun onBarcodeScanned_validPackage_marksAsLoaded() = runTest {
        val assigned = samplePackage(
            id = "p1",
            status = PackageStatus.ASIGNADO,
            barcode = "CODE1"
        ).copy(assignedDriverId = "driver-a", scheduledDate = today)
        val repository = FakePackageRepository(listOf(assigned))
        val viewModel = LoadTruckViewModel(
            packageRepository = repository,
            fleetRepository = FakeFleetRepository(listOf(truckA))
        )

        viewModel.selectTruck(truckA)
        viewModel.uiState.first { it.step == LoadTruckStep.LOADING }
        viewModel.onBarcodeScanned("CODE1")

        assertEquals(PackageStatus.CARGADO, repository.lastUpdatedPackage?.status)
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun onBarcodeScanned_wrongTruck_showsError() = runTest {
        val assigned = samplePackage(
            id = "p1",
            status = PackageStatus.ASIGNADO,
            barcode = "CODE1"
        ).copy(assignedDriverId = "driver-b", scheduledDate = today)
        val viewModel = LoadTruckViewModel(
            packageRepository = FakePackageRepository(listOf(assigned)),
            fleetRepository = FakeFleetRepository(listOf(truckA, truckB))
        )

        viewModel.selectTruck(truckA)
        viewModel.uiState.first { it.step == LoadTruckStep.LOADING }
        viewModel.onBarcodeScanned("CODE1")

        assertTrue(viewModel.uiState.value.errorMessage?.contains("XYZ789") == true)
    }

    @Test
    fun onBarcodeScanned_unassignedPackage_showsError() = runTest {
        val depot = samplePackage(
            id = "p1",
            status = PackageStatus.EN_DEPOSITO,
            barcode = "CODE1"
        ).copy(scheduledDate = today)
        val viewModel = LoadTruckViewModel(
            packageRepository = FakePackageRepository(listOf(depot)),
            fleetRepository = FakeFleetRepository(listOf(truckA))
        )

        viewModel.selectTruck(truckA)
        viewModel.onBarcodeScanned("CODE1")

        assertTrue(viewModel.uiState.value.errorMessage?.contains("sin asignar") == true)
    }

    @Test
    fun onBarcodeScanned_alreadyLoaded_showsError() = runTest {
        val loaded = samplePackage(
            id = "p1",
            status = PackageStatus.CARGADO,
            barcode = "CODE1"
        ).copy(assignedDriverId = "driver-a", scheduledDate = today)
        val viewModel = LoadTruckViewModel(
            packageRepository = FakePackageRepository(listOf(loaded)),
            fleetRepository = FakeFleetRepository(listOf(truckA))
        )

        viewModel.selectTruck(truckA)
        viewModel.onBarcodeScanned("CODE1")

        assertTrue(viewModel.uiState.value.errorMessage?.contains("ya fue cargado") == true)
    }

    @Test
    fun searchQuery_filtersByClientName() = runTest {
        val pkg1 = samplePackage(
            id = "p1",
            clientName = "María",
            status = PackageStatus.ASIGNADO
        ).copy(assignedDriverId = "driver-a", scheduledDate = today)
        val pkg2 = samplePackage(
            id = "p2",
            clientName = "Carlos",
            status = PackageStatus.ASIGNADO
        ).copy(assignedDriverId = "driver-a", scheduledDate = today)

        val viewModel = LoadTruckViewModel(
            packageRepository = FakePackageRepository(listOf(pkg1, pkg2)),
            fleetRepository = FakeFleetRepository(listOf(truckA))
        )

        viewModel.selectTruck(truckA)
        viewModel.uiState.first { it.pendingPackages.size == 2 }
        viewModel.onSearchQueryChange("María")

        val state = viewModel.uiState.first { it.pendingPackages.size == 1 }
        assertEquals("María", state.pendingPackages.first().clientName)
    }
}
