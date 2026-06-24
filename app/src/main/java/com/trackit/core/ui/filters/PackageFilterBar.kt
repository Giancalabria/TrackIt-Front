package com.trackit.core.ui.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PackageFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    appliedFilters: PackageFilters,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    searchPlaceholder: String = "Buscar por cliente..."
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(searchPlaceholder) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar")
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onFilterClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Filtros")
                if (appliedFilters.isActive) {
                    Spacer(Modifier.width(8.dp))
                    Badge { Text("•") }
                }
            }
        }

        if (appliedFilters.isActive) {
            Text(
                text = "Filtros activos: ${appliedFilters.activeSummary()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
