package com.trackit.feature.warehouse.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackit.data.model.PackageSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageEditSheet(
    form: PackageEditForm,
    onClientNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSizeSelected: (PackageSize) -> Unit,
    onSizeMenuExpandedChange: (Boolean) -> Unit,
    onFragileChange: (Boolean) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Editar paquete",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = form.clientName,
                onValueChange = onClientNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cliente") },
                singleLine = true,
                enabled = !form.isSaving
            )

            OutlinedTextField(
                value = form.address,
                onValueChange = onAddressChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dirección de destino") },
                enabled = !form.isSaving
            )

            ExposedDropdownMenuBox(
                expanded = form.isSizeMenuExpanded,
                onExpandedChange = onSizeMenuExpandedChange
            ) {
                OutlinedTextField(
                    value = form.size.toLabel(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Tamaño") },
                    enabled = !form.isSaving,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = form.isSizeMenuExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = form.isSizeMenuExpanded,
                    onDismissRequest = { onSizeMenuExpandedChange(false) }
                ) {
                    PackageSize.entries.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.toLabel()) },
                            onClick = { onSizeSelected(size) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Frágil")
                Switch(
                    checked = form.isFragile,
                    onCheckedChange = onFragileChange,
                    enabled = !form.isSaving
                )
            }

            OutlinedTextField(
                value = form.barcode,
                onValueChange = onBarcodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código de barras") },
                singleLine = true,
                enabled = !form.isSaving
            )

            form.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !form.isSaving
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !form.isSaving
                ) {
                    if (form.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

private fun PackageSize.toLabel(): String = when (this) {
    PackageSize.SMALL -> "Pequeño"
    PackageSize.MEDIUM -> "Mediano"
    PackageSize.LARGE -> "Grande"
}
