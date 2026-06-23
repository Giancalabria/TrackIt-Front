package com.trackit.data.model

fun Package.isEditableByWarehouse(): Boolean =
    status == PackageStatus.EN_DEPOSITO || status == PackageStatus.ASIGNADO
