package com.selfguide.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long? = null,
    val user: AuthUser? = null
)