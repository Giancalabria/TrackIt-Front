package com.trackit.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackit.data.model.Package
import com.trackit.data.model.isEditableByWarehouse

@Composable
fun HistoryPackageCard(
    packageItem: Package,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editable = packageItem.isEditableByWarehouse()
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (editable) {
                    Modifier.clickable(onClick = onEditClick)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
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
                if (packageItem.barcode.isNotBlank()) {
                    Text(
                        text = "Código: ${packageItem.barcode}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                PackageStatusChip(status = packageItem.status)
            }

            if (editable) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones del paquete"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
