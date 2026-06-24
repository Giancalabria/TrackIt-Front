package com.trackit.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackit.core.AppConfig
import com.trackit.data.model.PhotonFeature
import com.trackit.data.repository.IMapRepository
import com.trackit.data.repository.MapRepository
import kotlinx.coroutines.delay

@Composable
fun AddressSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (PhotonFeature) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Buscar dirección o lugar",
    enabled: Boolean = true,
    mapRepository: IMapRepository = remember { MapRepository.getInstance() }
) {
    var searchResults by remember { mutableStateOf<List<PhotonFeature>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.isBlank() || query.length <= 3) {
            searchResults = emptyList()
            searchError = null
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        searchError = null
        delay(500L)
        try {
            val response = mapRepository.searchAddress(
                query = query,
                lat = AppConfig.DEFAULT_GEOCODER_LAT,
                lon = AppConfig.DEFAULT_GEOCODER_LON
            )
            searchResults = response.features
        } catch (e: Exception) {
            e.printStackTrace()
            searchResults = emptyList()
            searchError = "No se pudo buscar la dirección. Verificá tu conexión."
        } finally {
            isSearching = false
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    )

    searchError?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    if (searchResults.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            searchResults.forEach { result ->
                ListItem(
                    headlineContent = { Text(result.properties.getDisplayName()) },
                    supportingContent = { Text(result.properties.city ?: "") },
                    modifier = Modifier.clickable(enabled = enabled) {
                        onPlaceSelected(result)
                        searchResults = emptyList()
                    }
                )
            }
        }
    }
}
