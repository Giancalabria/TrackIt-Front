package com.trackit.feature.profile

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackit.core.ui.theme.ThemeMode

@Composable
fun ThemeAppearanceSection(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Apariencia",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Elegí el tema de la app. «Sistema» sigue la configuración del dispositivo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeModeToggle(
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                icon = Icons.Default.Brightness6,
                label = "Sistema",
                contentDescription = "Tema del sistema"
            )
            ThemeModeToggle(
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                icon = Icons.Default.Brightness7,
                label = "Claro",
                contentDescription = "Tema claro"
            )
            ThemeModeToggle(
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) },
                icon = Icons.Default.Brightness4,
                label = "Oscuro",
                contentDescription = "Tema oscuro"
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Colores adaptativos",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Material You según tu wallpaper (Android 12+)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useDynamicColor,
                    onCheckedChange = onDynamicColorChange
                )
            }
        }
    }
}

@Composable
private fun ThemeModeToggle(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
