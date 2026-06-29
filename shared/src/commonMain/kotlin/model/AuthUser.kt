package com.selfguide.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String?
)
