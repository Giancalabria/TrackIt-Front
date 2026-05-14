package com.trackit.feature.admin.fleet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.data.model.Truck

@Composable
fun FleetScreen(
    viewModel: FleetViewModel = viewModel()
) {
    val trucks by viewModel.trucks.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(trucks, key = { it.id }) { truck ->
            TruckCard(truck = truck)
        }
    }
}

@Composable
private fun TruckCard(truck: Truck) {
    val progress = if (truck.totalCount == 0) {
        0f
    } else {
        truck.deliveredCount.toFloat() / truck.totalCount.toFloat()
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = truck.driverName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Patente: ${truck.plate}",
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${truck.deliveredCount}/${truck.totalCount} paquetes entregados",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
