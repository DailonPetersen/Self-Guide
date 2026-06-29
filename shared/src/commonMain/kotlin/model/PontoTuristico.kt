package com.selfguide.model

import kotlinx.serialization.Serializable

@Serializable
data class PontoTuristico(
    val id: String,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raioGatilhoMetros: Int,
    val categoriaPrincipal: String?,
    val placeId: String?,
    val contentHash: String?,
    val status: String,
    val idioma: String?,
    val tituloLocal: String?,
    val roteiroRapido: String?,
    val roteiroCompleto: String?,
    val audioUrlRapido: String?,
    val audioUrlCompleto: String?,
    val imagemUrl: String?,
    val horariosFuncionamento: String?,
    val custoIngresso: String?,
    val categorias: List<String>,
    val fonteUrl: String?,
    val distanciaMetros: Long,
    val rank: Long,
    val isPremium: Boolean,
    val isAtivo: Boolean
)
