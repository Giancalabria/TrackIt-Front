package com.trackit.feature.warehouse.intake

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackit.core.ui.components.AddressSearchField
import com.trackit.core.ui.components.BarcodeScannerSheet
import com.trackit.data.model.PackageSize
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    onBack: () -> Unit = {},
    viewModel: IntakeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingreso de paquetes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.clientName,
                onValueChange = viewModel::onClientNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cliente") },
                singleLine = true
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                AddressSearchField(
                    query = uiState.address,
                    onQueryChange = viewModel::onAddressChange,
                    onPlaceSelected = viewModel::onAddressSelected,
                    label = "Dirección de destino"
                )
            }

            ExposedDropdownMenuBox(
                expanded = uiState.isSizeMenuExpanded,
                onExpandedChange = viewModel::onSizeMenuExpandedChange
            ) {
                OutlinedTextField(
                    value = uiState.size.toLabel(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Tamaño") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isSizeMenuExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = uiState.isSizeMenuExpanded,
                    onDismissRequest = { viewModel.onSizeMenuExpandedChange(false) }
                ) {
                    PackageSize.entries.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.toLabel()) },
                            onClick = { viewModel.onSizeSelected(size) }
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
                    checked = uiState.isFragile,
                    onCheckedChange = viewModel::onFragileChange
                )
            }

            OutlinedTextField(
                value = uiState.scheduledDate.format(DATE_FORMATTER),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Fecha de entrega") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                    }
                },
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showDatePicker = true
                                }
                            }
                        }
                    }
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = viewModel::openScanner,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.barcode.isEmpty()) "Escanear Código de Barras" else "Código: ${uiState.barcode}")
            }
        }
    }

    if (uiState.isScannerOpen) {
        BarcodeScannerSheet(
            onCodeScanned = viewModel::onBarcodeScanned,
            onDismiss = viewModel::closeScanner,
            title = "Asociar Código al Ingreso"
        )
    }

    if (showDatePicker) {
        IntakeDatePickerDialog(
            initialDate = uiState.scheduledDate,
            onConfirm = {
                viewModel.onScheduledDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntakeDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    } ?: onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = state)
    }
}

private fun PackageSize.toLabel(): String = when (this) {
    PackageSize.SMALL -> "Pequeño"
    PackageSize.MEDIUM -> "Mediano"
    PackageSize.LARGE -> "Grande"
}
