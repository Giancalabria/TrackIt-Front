package com.trackit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val role: String
)

internal fun String.toUserRoleOrNull(): UserRole? = when (uppercase()) {
    "DRIVER" -> UserRole.DRIVER
    "WAREHOUSE" -> UserRole.WAREHOUSE
    "ADMIN" -> UserRole.ADMIN
    else -> null
}
