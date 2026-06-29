package com.selfguide.auth

import com.selfguide.model.AuthUser

interface AuthRepository {

    suspend fun signUp(email: String, password: String): Result<Unit>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    suspend fun signInWithApple(idToken: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    suspend fun getCurrentUser(): AuthUser?

    suspend fun restoreSession(): Boolean
}