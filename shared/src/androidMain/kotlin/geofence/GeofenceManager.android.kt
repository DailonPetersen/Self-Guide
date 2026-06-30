package com.selfguide.geofence

import android.content.Context
import android.app.PendingIntent
import androidx.core.content.ContextCompat
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.GeofencingRequest
import com.selfguide.model.PontoTuristico
import com.selfguide.repository.PontosRepository
import com.selfguide.util.AppSettingsOpener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

actual class GeofenceOrchestratorImpl(
    private val context: Context,
    private val repository: PontosRepository
) : GeofenceOrchestrator {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _estadoAtual = MutableStateFlow(
        EstadoGeofences(
            pontosAtivos = emptyList(),
            pontosSubstitutos = emptyList(),
            pendentesSync = emptyList(),
            ultimaPosicao = Coordenadas(0.0, 0.0)
        )
    )
    private val dwellTimers = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val pendingGeofenceRequests = mutableMapOf<String, PendingIntent>()

    override suspend fun inicializar(
        posicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao {
        return try {
            val pontos = repository.buscarPontosProximos(usuarioId, posicao.latitude, posicao.longitude, idioma)
            val pontosAtivos = pontos.filter { it.isAtivo }
            val pontosSubstitutos = pontos.filter { !it.isAtivo }

            _estadoAtual.value = EstadoGeofences(
                pontosAtivos = pontosAtivos,
                pontosSubstitutos = pontosSubstitutos,
                pendentesSync = emptyList(),
                ultimaPosicao = posicao
            )

            registrarGeofencesNativas(pontosAtivos)
            registrarMacroCerca(posicao)

            ResultadoInicializacao(sucesso = true, pontosRegistrados = pontosAtivos.size)
        } catch (e: Exception) {
            ResultadoInicializacao(sucesso = false, pontosRegistrados = 0, erro = e.message)
        }
    }

    override suspend fun marcarConsumido(pontoId: String, usuarioId: String): ResultadoSubstituicao {
        removerGeofenceNativa(pontoId)

        val proximoSubstituto = _estadoAtual.value.pontosSubstitutos.firstOrNull()

        proximoSubstituto?.let {
            val novaLista = _estadoAtual.value.pontosAtivos.toMutableList()
                .apply { add(it.copy(isAtivo = true, rank = it.rank + 100)) }
            _estadoAtual.value = _estadoAtual.value.copy(pontosAtivos = novaLista)
        }

        return ResultadoSubstituicao(
            pontoRemovidoId = pontoId,
            pontoInseridoId = proximoSubstituto?.id,
            sincronizadoRemoto = false
        )
    }

    override suspend fun recalcularPorDeslocamento(
        novaPosicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao {
        return inicializar(novaPosicao, usuarioId, idioma)
    }

    override fun onEntradaGeofence(pontoId: String) {
        dwellTimers[pontoId] = scope.launch {
            delay(30_000)
            enviarNotificacaoLocal(pontoId)
        }
    }

    override fun onSaidaGeofence(pontoId: String) {
        dwellTimers[pontoId]?.cancel()
        dwellTimers.remove(pontoId)
    }

    override suspend fun sincronizarPendencias(usuarioId: String) {
    }

    override val estadoAtual: Flow<EstadoGeofences> = _estadoAtual

    private fun registrarGeofencesNativas(pontos: List<PontoTuristico>) {
        val geofenceList = pontos.map { ponto ->
            Geofence.Builder()
                .setRequestId(ponto.id)
                .setCircularRegion(
                    ponto.latitude,
                    ponto.longitude,
                    ponto.raioGatilhoMetros.toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
            .addGeofences(geofenceList)
            .build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                pontos.forEach { ponto ->
                    pendingGeofenceRequests[ponto.id] = pendingIntent
                }
            }
    }

    private fun registrarMacroCerca(posicao: Coordenadas) {
        val macroGeofence = Geofence.Builder()
            .setRequestId("MACRO_CERCA")
            .setCircularRegion(
                posicao.latitude,
                posicao.longitude,
                1000f
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_EXIT)
            .addGeofences(listOf(macroGeofence))
            .build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
    }

    private fun removerGeofenceNativa(pontoId: String) {
        geofencingClient.removeGeofences(pendingGeofenceRequests[pontoId])
        pendingGeofenceRequests.remove(pontoId)
    }

    private fun enviarNotificacaoLocal(pontoId: String) {
        val pontosAtivos = _estadoAtual.value.pontosAtivos
        val ponto = pontosAtivos.find { it.id == pontoId }
        ponto?.let {
            AppSettingsOpener.mostrarNotificacao(
                context,
                it.id,
                it.tituloLocal ?: it.nome,
                "Toque para ouvir a história deste local"
            )
        }
    }
}