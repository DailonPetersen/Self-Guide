package com.selfguide.security

import com.selfguide.model.AuthSession

expect class SecureStorage() {
    suspend fun saveSession(session: AuthSession)
    suspend fun loadSession(): AuthSession?
    suspend fun clearSession()
}