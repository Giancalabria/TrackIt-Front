package com.trackit.feature.warehouse.history

import com.trackit.data.model.PackageSize
import com.trackit.data.model.PackageStatus

data class PackageEditForm(
    val packageId: String,
    val clientName: String,
    val address: String,
    val destinationLat: Double?,
    val destinationLon: Double?,
    val size: PackageSize,
    val isFragile: Boolean,
    val barcode: String,
    val status: PackageStatus,
    val isSizeMenuExpanded: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)
