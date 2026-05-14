package com.trackit.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.trackit.core.ui.theme.DeepForestGreen
import com.trackit.core.ui.theme.TerracottaOrange
import com.trackit.data.model.PackageStatus

@Composable
fun PackageStatusChip(status: PackageStatus) {
    val (label, containerColor, labelColor) = when (status) {
        PackageStatus.PENDING -> Triple(
            "Pendiente",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )

        PackageStatus.IN_TRANSIT -> Triple(
            "En ruta",
            TerracottaOrange.copy(alpha = 0.18f),
            TerracottaOrange
        )

        PackageStatus.DELIVERED -> Triple(
            "Entregado",
            DeepForestGreen.copy(alpha = 0.18f),
            DeepForestGreen
        )
    }

    SuggestionChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            disabledContainerColor = containerColor,
            disabledLabelColor = labelColor
        )
    )
}
