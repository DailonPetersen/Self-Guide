package com.selfguide.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.selfguide.model.AuthSession
import kotlinx.serialization.json.Json

actual class SecureStorage {
    private val json = Json { ignoreUnknownKeys = true }

    actual suspend fun saveSession(session: AuthSession) {
        val prefs = AndroidPreferences.getPrefs()
        val sessionJson = json.encodeToString(AuthSession.serializer(), session)
        prefs.edit().putString("auth_session", sessionJson).apply()
    }

    actual suspend fun loadSession(): AuthSession? {
        val prefs = AndroidPreferences.getPrefs()
        val sessionJson = prefs.getString("auth_session", null) ?: return null
        return try {
            json.decodeFromString(AuthSession.serializer(), sessionJson)
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun clearSession() {
        val prefs = AndroidPreferences.getPrefs()
        prefs.edit().remove("auth_session").apply()
    }
}

object AndroidPreferences {
    private var appContext: Context? = null
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    fun getPrefs() = EncryptedSharedPreferences.create(
        "selfguide_secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        appContext!!,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}