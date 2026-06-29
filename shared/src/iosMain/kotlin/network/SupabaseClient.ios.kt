package com.selfguide.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.kotlinxJson

actual fun createSupabaseClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            kotlinxJson()
        }
        expectSuccess = true
    }
}