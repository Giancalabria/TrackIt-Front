package com.trackit.data.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val role: UserRole
)
