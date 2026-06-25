package com.trackit.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PackageStatus
import com.trackit.data.model.PhotonFeature
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.IMapRepository
import com.trackit.data.repository.MapRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseLocator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class MapUiState(
    val searchQuery: String = "",
    val searchResults: List<PhotonFeature> = emptyList(),
    val assignedPackages: List<Package> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingRoute: Boolean = false,
    val routeSegments: List<List<GeoPoint>> = emptyList(),
    val nextPackage: Package? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
class MapViewModel(
    private val mapRepository: IMapRepository = MapRepository.getInstance(),
    private val packageRepository: IPackageRepository = SupabaseLocator.packageRepository,
    private val authRepository: IAuthRepository = SupabaseLocator.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .filter { it.isNotBlank() && it.length > 2 }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }

        observeAssignedPackages()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLocation.value = lat to lon
    }

    /**
     * Draws a route from the driver's current location ONLY to the NEXT pending stop.
     * Also transitions all CARGADO packages to EN_CAMINO if it's the start of the trip.
     */
    fun startTrip(currentLat: Double, currentLon: Double) {
        val stops = _uiState.value.assignedPackages
            .filter { it.status != PackageStatus.ENTREGADO && it.status != PackageStatus.FALLIDO }
            .filter { it.destinationLat != null && it.destinationLon != null }
            .sortedWith(compareBy<Package, Int?>(nullsLast()) { it.routeOrder }.thenBy { it.eta })

        if (stops.isEmpty()) {
            _uiState.update { 
                it.copy(
                    errorMessage = "No tenés paradas pendientes para rutear.", 
                    routeSegments = emptyList(), 
                    nextPackage = null 
                ) 
            }
            return
        }

        val nextPkg = stops.first()

        _uiState.update { it.copy(isLoadingRoute = true, errorMessage = null, nextPackage = nextPkg) }
        viewModelScope.launch {
            try {
                // 1. Update status of CARGADO packages to EN_CAMINO
                val loadedPackages = stops.filter { it.status == PackageStatus.CARGADO }
                loadedPackages.forEach { pkg ->
                    packageRepository.updateStatus(pkg.id, PackageStatus.EN_CAMINO)
                }

                // 2. Build route ONLY to the next package
                val response = mapRepository.getRoute(
                    currentLon, currentLat,
                    nextPkg.destinationLon!!, nextPkg.destinationLat!!
                )
                val segmentPoints = response.features.firstOrNull()?.geometry?.asLineString()?.map {
                    GeoPoint(it[1], it[0])
                } ?: emptyList()

                _uiState.update { 
                    it.copy(
                        routeSegments = if (segmentPoints.isNotEmpty()) listOf(segmentPoints) else emptyList(), 
                        isLoadingRoute = false 
                    ) 
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        errorMessage = "No se pudo trazar la ruta al siguiente paquete. Reintentá."
                    )
                }
            }
        }
    }

    fun deliverPackage(packageId: String, scannedCode: String) {
        val pkg = _uiState.value.nextPackage ?: return
        if (pkg.id != packageId) return

        // Validate code using the same logic as RouteViewModel
        val input = scannedCode.trim()
        val expected = pkg.barcode.ifBlank { pkg.id }
        val matches = input.equals(expected, ignoreCase = true) || input.equals(pkg.id, ignoreCase = true)

        if (!matches) {
            _uiState.update {
                it.copy(errorMessage = "Código inválido para este paquete.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoute = true, errorMessage = null) }
            packageRepository.updateStatus(packageId, PackageStatus.ENTREGADO)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            successMessage = "¡Escaneo exitoso! Entrega registrada.",
                            errorMessage = null, 
                            isLoadingRoute = false 
                        ) 
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingRoute = false,
                            errorMessage = "Error al registrar la entrega."
                        )
                    }
                }
        }
    }

    private fun observeAssignedPackages() {
        combine(
            packageRepository.packages,
            authRepository.currentUser
        ) { allPackages, currentUser ->
            if (currentUser == null) return@combine emptyList()
            allPackages.filter { it.assignedDriverId == currentUser.id }
        }.onEach { pkgs ->
            _uiState.update { state ->
                val nextStops = pkgs
                    .filter { it.status != PackageStatus.ENTREGADO && it.status != PackageStatus.FALLIDO }
                    .filter { it.destinationLat != null && it.destinationLon != null }
                    .sortedWith(compareBy<Package, Int?>(nullsLast()) { it.routeOrder }.thenBy { it.eta })
                
                val nextPkg = nextStops.firstOrNull()
                
                state.copy(
                    assignedPackages = pkgs,
                    nextPackage = nextPkg,
                    // Clear segments if the next package changed to force recalculation
                    routeSegments = if (nextPkg?.id != state.nextPackage?.id) emptyList() else state.routeSegments
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, errorMessage = null) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, errorMessage = null) }
        try {
            val (lat, lon) = _userLocation.value ?: (null to null)
            val response = mapRepository.searchAddress(query = query, lat = lat, lon = lon)
            _uiState.update { it.copy(searchResults = response.features, isSearching = false) }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(
                    isSearching = false,
                    errorMessage = "No se pudo buscar la dirección. Verificá tu conexión."
                )
            }
        }
    }

    fun selectDestination(feature: PhotonFeature, currentLat: Double, currentLon: Double) {
        val coords = feature.geometry.asPoint() // [lon, lat]
        val destLon = coords[0]
        val destLat = coords[1]

        _uiState.update {
            it.copy(
                searchResults = emptyList(),
                searchQuery = feature.properties.getDisplayName(),
                isLoadingRoute = true,
                errorMessage = null,
                nextPackage = null // Clear next package when manually searching
            )
        }

        viewModelScope.launch {
            try {
                val response = mapRepository.getRoute(currentLon, currentLat, destLon, destLat)
                val points = response.features.firstOrNull()?.geometry?.asLineString()?.map {
                    GeoPoint(it[1], it[0]) // ORS [lon, lat] -> Osmdroid GeoPoint(lat, lon)
                } ?: emptyList()

                _uiState.update { it.copy(routeSegments = listOf(points), isLoadingRoute = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        errorMessage = "No se pudo calcular la ruta. Reintentá."
                    )
                }
            }
        }
    }
}
