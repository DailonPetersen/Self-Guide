package com.selfguide.geofence

import com.selfguide.model.PontoTuristico
import kotlinx.coroutines.flow.Flow

interface GeofenceOrchestrator {
    suspend fun inicializar(
        posicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao

    suspend fun marcarConsumido(
        pontoId: String,
        usuarioId: String
    ): ResultadoSubstituicao

    suspend fun recalcularPorDeslocamento(
        novaPosicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao

    fun onEntradaGeofence(pontoId: String)

    fun onSaidaGeofence(pontoId: String)

    suspend fun sincronizarPendencias(usuarioId: String)

    val estadoAtual: Flow<EstadoGeofences>
}

data class Coordenadas(val latitude: Double, val longitude: Double)

data class EstadoGeofences(
    val pontosAtivos: List<PontoTuristico>,
    val pontosSubstitutos: List<PontoTuristico>,
    val pendentesSync: List<String>,
    val ultimaPosicao: Coordenadas
)

data class ResultadoInicializacao(
    val sucesso: Boolean,
    val pontosRegistrados: Int,
    val erro: String? = null
)

data class ResultadoSubstituicao(
    val pontoRemovidoId: String,
    val pontoInseridoId: String?,
    val sincronizadoRemoto: Boolean
)