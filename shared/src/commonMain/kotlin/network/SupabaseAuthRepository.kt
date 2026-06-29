package com.selfguide.network

import com.selfguide.model.AuthUser
import com.selfguide.model.AuthSession
import com.selfguide.security.SecureStorage
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.Provider
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.plugins.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SupabaseProvider(
    private val config: SupabaseConfig,
    private val secureStorage: SecureStorage
) {
    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() = _client ?: createClient()

    private fun createClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabaseAnonKey
        ) {
            install(Auth)
            install(Postgrest)

            auth.onSessionExpired += {
                secureStorage.clearSession()
            }
        }.also { _client = it }
    }

    fun initializeSession() {
        val session = secureStorage.loadSession() ?: return
        client.auth.importSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken
        )
    }

    suspend fun restoreSessionIfNeeded(): Boolean {
        return try {
            if (client.auth.getCurrentSessionOrNull() == null) {
                val savedSession = secureStorage.loadSession() ?: return false
                client.auth.importSession(
                    accessToken = savedSession.accessToken,
                    refreshToken = savedSession.refreshToken
                )
            }
            true
        } catch (e: Exception) {
            secureStorage.clearSession()
            false
        }
    }
}

class SupabaseAuthRepository(
    private val provider: SupabaseProvider
) : AuthRepository {

    private val supabase = provider.client
    private val auth = supabase.auth
    private val secureStorage = provider.secureStorage

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        setupSessionListener()
    }

    private fun setupSessionListener() {
        scope.launch {
            auth.sessionStatus.collectLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val session = auth.getCurrentSessionOrNull()
                        session?.let {
                            secureStorage.saveSession(
                                AuthSession(
                                    accessToken = it.accessToken,
                                    refreshToken = it.refreshToken,
                                    expiresAt = it.expiresAt
                                )
                            )
                        }
                    }
                    is SessionStatus.SignedOut -> {
                        secureStorage.clearSession()
                    }
                    else -> Unit
                }
            }
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.loginWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        auth.signInWith(Provider.Google) {
            this.idToken = idToken
        }
        Unit
    }

    override suspend fun signInWithApple(idToken: String): Result<Unit> = runCatching {
        auth.signInWith(Provider.Apple) {
            this.idToken = idToken
        }
        Unit
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    override suspend fun getCurrentUser(): AuthUser? {
        val session = auth.getCurrentSessionOrNull() ?: return null
        return AuthUser(
            id = session.user?.id ?: return null,
            email = session.user?.email
        )
    }

    override suspend fun restoreSession(): Boolean = provider.restoreSessionIfNeeded()
}