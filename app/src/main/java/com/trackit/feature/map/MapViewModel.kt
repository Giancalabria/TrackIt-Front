package com.trackit.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.data.model.PhotonFeature
import com.trackit.data.repository.IMapRepository
import com.trackit.data.repository.MapRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class MapUiState(
    val searchQuery: String = "",
    val searchResults: List<PhotonFeature> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingRoute: Boolean = false,
    val routePoints: List<GeoPoint> = emptyList()
)

@OptIn(FlowPreview::class)
class MapViewModel(
    private val mapRepository: IMapRepository = MapRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

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
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        try {
            val response = mapRepository.searchAddress(query)
            _uiState.update { it.copy(searchResults = response.features, isSearching = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    fun selectDestination(feature: PhotonFeature, currentLat: Double, currentLon: Double) {
        val coords = feature.geometry.asPoint() // [lon, lat]
        val destLon = coords[0]
        val destLat = coords[1]

        _uiState.update { it.copy(searchResults = emptyList(), searchQuery = feature.properties.getDisplayName(), isLoadingRoute = true) }

        viewModelScope.launch {
            try {
                val response = mapRepository.getRoute(currentLon, currentLat, destLon, destLat)
                val points = response.features.firstOrNull()?.geometry?.asLineString()?.map {
                    GeoPoint(it[1], it[0]) // ORS [lon, lat] -> Osmdroid GeoPoint(lat, lon)
                } ?: emptyList()
                
                _uiState.update { it.copy(routePoints = points, isLoadingRoute = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingRoute = false) }
            }
        }
    }
}
