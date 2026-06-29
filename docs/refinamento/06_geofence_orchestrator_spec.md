# Especificação do Módulo GeofenceOrchestrator
> Módulo isolado responsável pelo ciclo de vida completo das geofences. Nenhuma outra camada do app toca diretamente nas APIs nativas de geofencing.

---

## Responsabilidades

O `GeofenceOrchestrator` é o único módulo autorizado a:
- Registrar geofences no S.O. (CoreLocation no iOS, Geofencing API no Android)
- Remover geofences do S.O.
- Gerenciar o cache de substituição offline (5 pontos extras)
- Controlar a macro-cerca de recálculo (raio 1km)
- Sincronizar estado offline com o servidor

---

## Interface Pública (KMP Shared)

```kotlin
// shared/src/commonMain/kotlin/geofence/GeofenceOrchestrator.kt

interface GeofenceOrchestrator {

    // Inicializa os Top 20 geofences para uma nova posição
    suspend fun inicializar(
        posicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao

    // Chamado quando usuário marca ponto como consumido
    suspend fun marcarConsumido(
        pontoId: String,
        usuarioId: String
    ): ResultadoSubstituicao

    // Chamado pelo S.O. quando macro-cerca é cruzada (recálculo)
    suspend fun recalcularPorDeslocamento(
        novaPosicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao

    // Chamado quando geofence individual é disparada (entrada no raio)
    // Inicia timer de dwell time de 30s
    fun onEntradaGeofence(pontoId: String)

    // Chamado quando usuário sai do raio antes dos 30s
    // Cancela o timer — evita notificação espúria
    fun onSaidaGeofence(pontoId: String)

    // Sincroniza pendências offline quando conexão voltar
    suspend fun sincronizarPendencias(usuarioId: String)

    // Estado atual das geofences ativas
    val estadoAtual: Flow<EstadoGeofences>
}

data class Coordenadas(val latitude: Double, val longitude: Double)

data class EstadoGeofences(
    val pontosAtivos: List<PontoTuristico>,      // Top 20
    val pontosSubstitutos: List<PontoTuristico>, // Cache de 5 para offline
    val pendentesSync: List<String>,             // IDs consumidos offline aguardando sync
    val ultimaPosicao: Coordenadas
)

data class ResultadoInicializacao(
    val sucesso: Boolean,
    val pontosRegistrados: Int,
    val erro: String? = null
)

data class ResultadoSubstituicao(
    val pontoRemovidoId: String,
    val pontoInseridoId: String?,   // null se cache de substitutos vazio
    val sincronizadoRemoto: Boolean
)
```

---

## Implementação Android (expect/actual)

```kotlin
// androidMain/kotlin/geofence/GeofenceOrchestratorImpl.kt

actual class GeofenceOrchestratorImpl(
    private val context: Context,
    private val repository: PontosRepository,
    private val localDb: LocalDatabase
) : GeofenceOrchestrator {

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val dwellTimers = mutableMapOf<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun inicializar(
        posicao: Coordenadas,
        usuarioId: String,
        idioma: String
    ): ResultadoInicializacao {
        // 1. Busca Top 25 da API (20 ativos + 5 substitutos)
        val pontos = repository.buscarPontosProximos(usuarioId, posicao.latitude, posicao.longitude, idioma)

        // 2. Salva tudo no banco local
        localDb.salvarPontos(pontos)

        // 3. Registra os 20 primeiros como geofences no S.O.
        val pontosAtivos = pontos.filter { it.isAtivo }
        registrarGeofencesNativas(pontosAtivos)

        // 4. Registra macro-cerca de 1km para recálculo
        registrarMacroCerca(posicao)

        return ResultadoInicializacao(sucesso = true, pontosRegistrados = pontosAtivos.size)
    }

    override suspend fun marcarConsumido(pontoId: String, usuarioId: String): ResultadoSubstituicao {
        // 1. Remove geofence do S.O.
        removerGeofenceNativa(pontoId)

        // 2. Marca consumido no banco local
        localDb.marcarConsumido(pontoId, pendentesSync = true)

        // 3. Tenta sincronizar remotamente
        val sincronizado = try {
            repository.marcarConsumidoRemoto(usuarioId, pontoId)
            localDb.marcarSincronizado(pontoId)
            true
        } catch (e: NetworkException) {
            false // Sincronizará depois via sincronizarPendencias()
        }

        // 4. Insere próximo substituto do cache local
        val substituto = localDb.proximoSubstituto()
        substituto?.let {
            registrarGeofenceNativa(it)
            localDb.moverSubstitutoParaAtivo(it.id)
        }

        return ResultadoSubstituicao(
            pontoRemovidoId = pontoId,
            pontoInseridoId = substituto?.id,
            sincronizadoRemoto = sincronizado
        )
    }

    override fun onEntradaGeofence(pontoId: String) {
        // Inicia timer de 30s de dwell time
        dwellTimers[pontoId] = coroutineScope.launch {
            delay(30_000)
            // Timer completou — usuário ficou 30s dentro do raio
            dispararNotificacao(pontoId)
            dwellTimers.remove(pontoId)
        }
    }

    override fun onSaidaGeofence(pontoId: String) {
        // Cancela timer se usuário saiu antes dos 30s
        dwellTimers[pontoId]?.cancel()
        dwellTimers.remove(pontoId)
    }

    private fun registrarGeofencesNativas(pontos: List<PontoTuristico>) {
        val geofences = pontos.map { ponto ->
            Geofence.Builder()
                .setRequestId(ponto.id)
                .setCircularRegion(ponto.latitude, ponto.longitude, ponto.raioGatilhoMetros.toFloat())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }
        // Adiciona via GeofencingClient...
    }

    private fun registrarMacroCerca(posicao: Coordenadas) {
        val macroCerca = Geofence.Builder()
            .setRequestId("MACRO_CERCA")
            .setCircularRegion(posicao.latitude, posicao.longitude, 1000f) // 1km
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
        // Adiciona via GeofencingClient...
    }
}
```

---

## Implementação iOS (expect/actual)

```kotlin
// iosMain/kotlin/geofence/GeofenceOrchestratorImpl.kt

actual class GeofenceOrchestratorImpl(
    private val repository: PontosRepository,
    private val localDb: LocalDatabase
) : GeofenceOrchestrator {

    private val locationManager = CLLocationManager()
    private val dwellTimers = mutableMapOf<String, NSTimer>()

    override suspend fun inicializar(...) {
        // Limite do iOS: máximo 20 regiões simultâneas
        // Nossa macro-cerca ocupa 1 slot → 19 slots para pontos
        // Ajuste: usar 19 pontos ativos + macro-cerca no iOS
        // Android não tem esse limite mas seguimos o mesmo padrão por consistência

        val pontos = repository.buscarPontosProximos(...)
        pontos.filter { it.isAtivo }.take(19).forEach { ponto ->
            val regiao = CLCircularRegion(
                center = CLLocationCoordinate2DMake(ponto.latitude, ponto.longitude),
                radius = ponto.raioGatilhoMetros.toDouble(),
                identifier = ponto.id
            )
            regiao.notifyOnEntry = true
            regiao.notifyOnExit = true
            locationManager.startMonitoring(regiao)
        }

        // Macro-cerca (20º slot no iOS)
        val macroCerca = CLCircularRegion(
            center = CLLocationCoordinate2DMake(posicao.latitude, posicao.longitude),
            radius = 1000.0,
            identifier = "MACRO_CERCA"
        )
        macroCerca.notifyOnExit = true
        locationManager.startMonitoring(macroCerca)
    }

    override fun onEntradaGeofence(pontoId: String) {
        // NSTimer para dwell time de 30s
        dwellTimers[pontoId] = NSTimer.scheduledTimerWithTimeInterval(
            30.0, repeats = false
        ) { _ ->
            dispararNotificacao(pontoId)
            dwellTimers.remove(pontoId)
        }
    }

    override fun onSaidaGeofence(pontoId: String) {
        dwellTimers[pontoId]?.invalidate()
        dwellTimers.remove(pontoId)
    }
}
```

---

## Banco Local (SQLDelight)

```sql
-- PontoLocal.sq

CREATE TABLE PontoLocal (
    id TEXT NOT NULL PRIMARY KEY,
    nome TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    raio_gatilho_metros INTEGER NOT NULL DEFAULT 50,
    titulo_local TEXT NOT NULL,
    roteiro_rapido TEXT NOT NULL,
    roteiro_completo TEXT NOT NULL,
    audio_url_rapido TEXT,
    audio_url_completo TEXT,
    imagem_url TEXT,
    horarios_funcionamento TEXT,
    custo_ingresso TEXT,
    content_hash TEXT NOT NULL,
    rank INTEGER NOT NULL,           -- 1-20 (posição na lista)
    is_premium INTEGER NOT NULL,     -- 0 = free, 1 = premium
    is_ativo INTEGER NOT NULL,       -- 1 = geofence ativa, 0 = substituto offline
    status_consumo TEXT NOT NULL DEFAULT 'nao_visto',
    pendente_sync INTEGER NOT NULL DEFAULT 0,
    audio_rapido_cached INTEGER NOT NULL DEFAULT 0,   -- 1 se MP3 baixado localmente
    audio_completo_cached INTEGER NOT NULL DEFAULT 0,
    data_cache TEXT NOT NULL         -- ISO8601 timestamp
);

-- Queries geradas pelo SQLDelight
pontosAtivos:
SELECT * FROM PontoLocal WHERE is_ativo = 1 ORDER BY rank ASC;

substitutos:
SELECT * FROM PontoLocal WHERE is_ativo = 0 ORDER BY rank ASC LIMIT 5;

proximoSubstituto:
SELECT * FROM PontoLocal WHERE is_ativo = 0 ORDER BY rank ASC LIMIT 1;

pendentesSincronizacao:
SELECT * FROM PontoLocal WHERE pendente_sync = 1;

marcarConsumido:
UPDATE PontoLocal SET status_consumo = 'consumido', pendente_sync = 1 WHERE id = :id;

marcarSincronizado:
UPDATE PontoLocal SET pendente_sync = 0 WHERE id = :id;
```

---

## Fluxo de Sincronização Offline

```
App inicia com conexão disponível
    └─ inicializar() → busca Top 25 remotamente → salva local → registra 20 geofences

Usuário entra em área sem sinal
    └─ marcarConsumido() → opera 100% local → pendente_sync = TRUE

Conexão volta
    └─ sincronizarPendencias() é chamado:
        ├─ Para cada ponto com pendente_sync = TRUE:
        │   ├─ Tenta POST para Supabase: historico_usuario.status = 'consumido'
        │   └─ Se sucesso: pendente_sync = FALSE
        └─ Verifica se cache de substitutos ainda é válido (posição não mudou muito)
            └─ Se inválido: recalcularPorDeslocamento() com posição atual
```

---

## Observações de Implementação

**iOS — Restrição de 20 Geofences:**
O iOS limita 20 regiões monitoradas simultaneamente por app. Nossa arquitetura usa 19 para pontos turísticos + 1 para a macro-cerca. Em Android não há limite oficial mas seguimos o mesmo padrão para manter o comportamento consistente entre plataformas.

**Background Location:**
Tanto iOS quanto Android exigem permissão especial para monitoramento de geofences em background. No onboarding, a explicação deve deixar claro o motivo: "Para receber notificações automáticas quando você se aproximar de pontos históricos, mesmo com o app fechado."

**Bateria:**
Geofencing nativo tem impacto mínimo na bateria — os S.O. usam fusão de sensores (WiFi, celular, GPS) e só ativam o GPS de alta precisão quando o usuário está próximo de uma geofence. É substancialmente mais eficiente do que polling de GPS contínuo.
