package com.trackit.feature.warehouse.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WarehouseHomeScreen(
    onLoadTruckClick: () -> Unit,
    onIntakeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Depósito",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Seleccioná la operación que querés realizar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLoadTruckClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            Icon(Icons.Default.LocalShipping, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Cargar camión", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onIntakeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            Icon(Icons.Default.Inventory2, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ingresar paquete", style = MaterialTheme.typography.titleMedium)
        }
    }
}
