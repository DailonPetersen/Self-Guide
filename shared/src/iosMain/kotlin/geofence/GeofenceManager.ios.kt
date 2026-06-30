package com.selfguide.geofence

import com.selfguide.model.PontoTuristico
import com.selfguide.repository.PontosRepository
import com.selfguide.util.AppSettingsOpener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLRegionStateInside
import platform.Foundation.NSObject
import platform.Foundation.NSTimer
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoop.mainRunLoop

actual class GeofenceOrchestratorImpl(
    private val repository: PontosRepository
) : GeofenceOrchestrator {

    private val locationManager = CLLocationManager()
    private val _estadoAtual = MutableStateFlow(
        EstadoGeofences(
            pontosAtivos = emptyList(),
            pontosSubstitutos = emptyList(),
            pendentesSync = emptyList(),
            ultimaPosicao = Coordenadas(0.0, 0.0)
        )
    )
    private val dwellTimers = mutableMapOf<String, NSTimer>()

    override suspend fun inicializar(
        posicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao {
        return try {
            val pontos = repository.buscarPontosProximos(usuarioId, posicao.latitude, posicao.longitude, idioma)
            // iOS limite: 19 slots + macro-cerca
            val pontosAtivos = pontos.filter { it.isAtivo }.take(19)
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
            ResultadoInicializacao(sucesso = false, pontosRegistrados = 0, erro = e.message?.toString())
        }
    }

    override suspend fun marcarConsumido(pontoId: String, usuarioId: String): ResultadoSubstituicao {
        removerGeofenceNativa(pontoId)

        val proximoSubstituto = _estadoAtual.value.pontosSubstitutos.firstOrNull()

        proximoSubstituto?.let {
            val novaLista = _estadoAtual.value.pontosAtivos.toMutableList()
                .apply { add(it.copy(isAtivo = true)) }
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
        dwellTimers[pontoId] = NSTimer.scheduledTimerWithTimeInterval(30.0, repeats = false) {
            enviarNotificacaoLocal(pontoId)
        }
    }

    override fun onSaidaGeofence(pontoId: String) {
        dwellTimers[pontoId]?.invalidate()
        dwellTimers.remove(pontoId)
    }

    override suspend fun sincronizarPendencias(usuarioId: String) {
    }

    override val estadoAtual: Flow<EstadoGeofences> = _estadoAtual

    private fun registrarGeofencesNativas(pontos: List<PontoTuristico>) {
        pontos.forEach { ponto ->
            val regiao = CLCircularRegion(
                center = CLLocationCoordinate2D(ponto.latitude, ponto.longitude),
                radius = ponto.raioGatilhoMetros.toDouble(),
                identifier = ponto.id
            )
            regiao.notifyOnEntry = true
            regiao.notifyOnExit = true
            locationManager.startMonitoringForRegion(regiao)
        }
    }

    private fun registrarMacroCerca(posicao: Coordenadas) {
        val macroCerca = CLCircularRegion(
            center = CLLocationCoordinate2D(posicao.latitude, posicao.longitude),
            radius = 1000.0,
            identifier = "MACRO_CERCA"
        )
        macroCerca.notifyOnExit = true
        locationManager.startMonitoringForRegion(macroCerca)
    }

    private fun removerGeofenceNativa(pontoId: String) {
        locationManager.monitoredRegions
            .filter { it.identifier == pontoId }
            .forEach { locationManager.stopMonitoringForRegion(it) }
    }

    private fun enviarNotificacaoLocal(pontoId: String) {
        val pontosAtivos = _estadoAtual.value.pontosAtivos
        val ponto = pontosAtivos.find { it.id == pontoId }
        ponto?.let {
            AppSettingsOpener.mostrarNotificacao(
                NSString("Título"),
                it.tituloLocal ?: it.nome,
                "Toque para ouvir a história deste local"
            )
        }
    }
}