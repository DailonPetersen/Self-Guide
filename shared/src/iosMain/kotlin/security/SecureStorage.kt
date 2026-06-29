package com.selfguide.security

import com.selfguide.model.AuthSession
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Security.*

actual class SecureStorage actual constructor() {
    private val key = "auth_session"
    private val service = "SelfGuide"
    private val json = Json { ignoreUnknownKeys = true }

    actual suspend fun saveSession(session: AuthSession) {
        val sessionJson = Json.encodeToString(AuthSession.serializer(), session)
        saveToKeychain(sessionJson, key)
    }

    actual suspend fun loadSession(): AuthSession? {
        val sessionJson = loadFromKeychain(key) ?: return null
        return try {
            Json.decodeFromString(AuthSession.serializer(), sessionJson)
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun clearSession() {
        deleteFromKeychain(key)
    }

    private fun saveToKeychain(value: String, key: String) {
        val bytes = value.encodeToByteArray()
        val data = NSData.alloc().initWithBytes(bytes.toCValues(), bytes.size.toLong())
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key,
            kSecAttrService to service,
            kSecValueData to data
        )
        SecItemDelete(query)
        SecItemAdd(query, null)
    }

    private fun loadFromKeychain(key: String): String? {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key,
            kSecAttrService to service,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        )
        
        val resultPtr = SecItemCopyMatching(query, null) ?: return null
        val data = resultPtr as NSData
        return NSString.create(data, NSUTF8StringEncoding) as? String
    }

    private fun deleteFromKeychain(key: String) {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key,
            kSecAttrService to service
        )
        SecItemDelete(query)
    }
}