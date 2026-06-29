package com.selfguide.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.kotlinxJson

actual fun createSupabaseClient(): HttpClient {
    return HttpClient(Android) {
        install(ContentNegotiation) {
            kotlinxJson()
        }
        expectSuccess = true
    }
}