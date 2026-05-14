package com.trackit.feature.warehouse.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.PackageStatusChip
import com.trackit.data.model.Package

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val packages by viewModel.packages.collectAsStateWithLifecycle()

    if (packages.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Todavía no hay ingresos registrados.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(packages.reversed(), key = { it.id }) { packageItem ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = packageItem.clientName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = packageItem.address,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ID: ${packageItem.id}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    PackageStatusChip(status = packageItem.status)
                }
            }
        }
    }
}
