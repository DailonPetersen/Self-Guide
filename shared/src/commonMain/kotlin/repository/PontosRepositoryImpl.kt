package com.selfguide.repository

import com.selfguide.model.PontoTuristico
import com.selfguide.network.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import com.selfguide.database.LocalDatabase

class PontosRepository(
    private val client: HttpClient,
    private val supabaseConfig: SupabaseConfig,
    private val localDb: LocalDatabase?
) : PontoTuristicoRepository {

    override suspend fun buscarPontosProximos(
        usuarioId: String,
        latitude: Double,
        longitude: Double,
        idioma: String
    ): List<PontoTuristico> {
        return try {
            val response = client.post("${supabaseConfig.supabaseUrl}/functions/v1/buscar-pontos-proximos") {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.Authorization, "Bearer ${supabaseConfig.supabaseAnonKey}")
                }
                setBody(mapOf(
                    "usuario_id" to usuarioId,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "idioma" to idioma
                ))
                contentType(ContentType.Application.Json)
            }
            response.body()
        } catch (e: Exception) {
            // Fallback to local cache
            localDb?.pontoLocalQueries?.pontosAtivos()?.map { it.toPontoTuristico() } ?: emptyList()
        }
    }

    override suspend fun buscarPontoPorId(id: String, idioma: String): PontoTuristico? {
        return null
    }

    override suspend fun marcarComoConsumido(usuarioId: String, pontoId: String) {
    }
}