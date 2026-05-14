package com.trackit.feature.warehouse.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.EmptyState
import com.trackit.core.ui.components.HistoryPackageCard

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val packages by viewModel.packages.collectAsStateWithLifecycle()

    if (packages.isEmpty()) {
        EmptyState(message = "Todavía no hay ingresos registrados.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(packages.reversed(), key = { it.id }) { packageItem ->
            HistoryPackageCard(packageItem = packageItem)
        }
    }
}
