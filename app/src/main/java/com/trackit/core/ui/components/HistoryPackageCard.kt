package com.trackit.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackit.data.model.Package

@Composable
fun HistoryPackageCard(
    packageItem: Package,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
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
                text = "ID: ${packageItem.id}",
                style = MaterialTheme.typography.labelLarge
            )
            PackageStatusChip(status = packageItem.status)
        }
    }
}
