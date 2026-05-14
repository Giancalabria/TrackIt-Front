package com.trackit.feature.driver.route

import androidx.compose.foundation.clickable
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
fun RouteScreen(
    onPackageClick: (String) -> Unit,
    viewModel: RouteViewModel = viewModel()
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
                text = "No hay paquetes asignados.",
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
        items(packages, key = { it.id }) { packageItem ->
            PackageRouteCard(
                packageItem = packageItem,
                onClick = { onPackageClick(packageItem.id) }
            )
        }
    }
}

@Composable
private fun PackageRouteCard(
    packageItem: Package,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
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
                text = "ETA: ${packageItem.eta}",
                style = MaterialTheme.typography.labelLarge
            )
            PackageStatusChip(status = packageItem.status)
        }
    }
}
