package com.trackit.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.trackit.data.model.PackageStatus

@Composable
fun PackageStatusChip(status: PackageStatus, driverName: String? = null) {
    val (label, containerColor, labelColor) = when (status) {
        PackageStatus.EN_DEPOSITO -> Triple(
            "En depósito",
            Color.LightGray.copy(alpha = 0.2f),
            Color.Gray
        )

        PackageStatus.ASIGNADO -> Triple(
            if (driverName != null) "Asignado a $driverName" else "Asignado",
            Color(0xFFE3F2FD), // Light Blue
            Color(0xFF1976D2)
        )

        PackageStatus.CARGADO -> Triple(
            "Cargado",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        PackageStatus.EN_CAMINO -> Triple(
            "En camino",
            Color(0xFFD85C3A).copy(alpha = 0.18f),
            Color(0xFFD85C3A)
        )

        PackageStatus.ENTREGADO -> Triple(
            "Entregado",
            Color(0xFF2C5E3E).copy(alpha = 0.18f),
            Color(0xFF2C5E3E)
        )

        PackageStatus.FALLIDO -> Triple(
            "Fallido",
            Color(0xFFB3261E).copy(alpha = 0.18f),
            Color(0xFFB3261E)
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
