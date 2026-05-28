package com.trackit.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.Package
import com.trackit.data.model.PhotonFeature
import com.trackit.data.repository.IAuthRepository
import com.trackit.data.repository.IMapRepository
import com.trackit.data.repository.MapRepository
import com.trackit.data.repository.IPackageRepository
import com.trackit.data.repository.SupabaseAuthRepository
import com.trackit.data.repository.SupabaseLocator
import com.trackit.data.repository.SupabasePackageRepository
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
    val routePoints: List<GeoPoint> = emptyList(),
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
class MapViewModel(
    private val mapRepository: IMapRepository = MapRepository.getInstance(),
    private val packageRepository: IPackageRepository = SupabasePackageRepository(SupabaseLocator.client),
    private val authRepository: IAuthRepository = SupabaseAuthRepository(SupabaseLocator.client)
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

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLocation.value = lat to lon
    }

    private fun observeAssignedPackages() {
        combine(
            packageRepository.packages,
            authRepository.currentUser
        ) { allPackages, currentUser ->
            if (currentUser == null) return@combine emptyList()
            allPackages.filter { it.assignedDriverId == currentUser.id }
        }.onEach { pkgs ->
            _uiState.update { it.copy(assignedPackages = pkgs) }
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
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val response = mapRepository.getRoute(currentLon, currentLat, destLon, destLat)
                val points = response.features.firstOrNull()?.geometry?.asLineString()?.map {
                    GeoPoint(it[1], it[0]) // ORS [lon, lat] -> Osmdroid GeoPoint(lat, lon)
                } ?: emptyList()

                _uiState.update { it.copy(routePoints = points, isLoadingRoute = false) }
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
