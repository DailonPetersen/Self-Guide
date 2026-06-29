package com.selfguide.repository

import com.selfguide.model.PontoTuristico

interface PontoTuristicoRepository {
    suspend fun buscarPontosProximos(
        usuarioId: String,
        latitude: Double,
        longitude: Double,
        idioma: String = "pt-BR"
    ): List<PontoTuristico>
    
    suspend fun buscarPontoPorId(id: String, idioma: String = "pt-BR"): PontoTuristico?
    
    suspend fun marcarComoConsumido(usuarioId: String, pontoId: String)
}