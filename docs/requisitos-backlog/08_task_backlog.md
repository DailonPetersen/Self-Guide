# Backlog de Tarefas — Tour Guiado Automatizado
> Organizado por épico. Prioridade: 🔴 Crítico (bloqueante) | 🟡 Alta | 🟢 Média | ⚪ Baixa (pós-MVP)
> Complexidade: P = Pequeno (< 4h) | M = Médio (4-16h) | G = Grande (> 16h)

---

## Como ler este backlog

Cada tarefa segue o formato:
```
[ID] Título
  RF: requisito(s) coberto(s)
  Depende de: tarefa(s) predecessoras
  Complexidade: P / M / G
  Prioridade: 🔴 / 🟡 / 🟢 / ⚪
  Notas: observações técnicas relevantes
```

---

## ÉPICO 1 — Fundação & Infraestrutura (Sem isso nada funciona)

**T-001** Criar projeto Supabase e configurar extensões
```r
RF: RNF-01, RNF-02
Depende de: —
Complexidade: P
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-001-criar-projeto-supabase-e-configurar-extensões)
Notas: Habilitar postgis, uuid-ossp, pg_cron, pg_trgm.
       Região: South America (São Paulo).
       Anotar Project URL, anon_key, service_role_key.
```

**T-002** Executar DDL completo no Supabase (todas as tabelas)
```r
RF: Schema completo de 03_database_schema_final.md
Depende de: T-001
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-002-executar-ddl-completo-no-supabase-todas-as-tabelas)
Notas: Executar na ordem correta (FKs exigem tabelas pai antes).
       Criar função trigger_atualizar_timestamp() antes das tabelas.
       Criar todos os índices GIST e compostos.
```

**T-003** Configurar Row Level Security (RLS)
```r
RF: RF-36, RNF-01
Depende de: T-002
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-003-configurar-row-level-security-rls)
Notas: RLS em: historico_usuario, compras_usuario, reputacao_usuario,
       reportes_usuario, votos_sugestao.
       Pontos turísticos e conteúdo: leitura pública para usuários autenticados.
```

**T-004** Configurar Supabase Storage (buckets audios/ e imagens/)
```r
RF: RF-54
Depende de: T-001
Complexidade: P
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-004-configurar-supabase-storage-buckets-audios-e-imagens)
Notas: Buckets públicos para leitura.
       Upload restrito a service_role (somente Agente de IA).
```

**T-005** Criar função SQL `buscar_pontos_proximos` (RPC)
```r
RF: RF-15, RF-22, RNF-04
Depende de: T-002
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-005-criar-funçao-sql-buscar_pontos_proximos-rpc)
Notas: Query com CTE — Web Mercator para ranking + geography para gatilho.
       Retorna 25 registros (20 ativos + 5 substitutos).
       Incluir campos: rank, is_premium, is_ativo, content_hash.
       Testar performance com EXPLAIN ANALYZE.
```

**T-006** Criar projeto KMP (Kotlin Multiplatform) com estrutura de módulos
```r
RF: —
Depende de: —
Complexidade: G
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-006-criar-projeto-kmp-kotlin-multiplatform-com-estrutura-de-módulos)
Notas: Módulos: :shared (lógica comum), :androidApp, :iosApp.
       Configurar: KMP Core, Compose Multiplatform, Koin, SQLDelight, Ktor.
       Setup inicial do SQLDelight schema local.
```

**T-007** Configurar variáveis de ambiente e secrets
```r
RF: RNF-02
Depende de: T-001, T-006
Complexidade: P
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-007-configurar-variáveis-de-ambiente-e-secrets)
Notas: .env para Edge Functions (nunca commitado).
       BuildConfig para o app (anon_key apenas).
       Nunca expor service_role_key no app mobile.
```

---

## ÉPICO 2 — Autenticação & Perfil

**T-008** Implementar cadastro e login com email/senha
```r
RF: RF-01, RF-04
Depende de: T-006, T-003
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-008-implementar-cadastro-e-login-com-emailsenha)
Notas: Usar supabase-kt Auth plugin.
       Persistir token no Keychain (iOS) / EncryptedSharedPreferences (Android).
```

**T-009** Implementar login social Google
```r
RF: RF-02, RF-04
Depende de: T-008
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-009-implementar-login-social-google)
Notas: Configurar OAuth no Google Cloud Console.
       Android: usar Google Sign-In SDK + supabase-kt.
       iOS: usar GoogleSignIn SDK + supabase-kt.
```

**T-010** Implementar login social Apple
```r
RF: RF-03, RF-04
Depende de: T-008
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-010-implementar-login-social-apple)
Notas: Obrigatório para publicação na App Store.
       Requer conta Apple Developer ativa.
       iOS only — Android não precisa.
```

**T-011** Criar tela de perfil do usuário
```r
RF: RF-06
Depende de: T-008
Complexidade: M
Prioridade: 🟢
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-011-criar-tela-de-perfil-do-usuário)
Notas: Exibir: nome, badges conquistados, total de pontos visitados,
       cidade(s) desbloqueada(s).
       Badges são derivados de reputacao_usuario.total_reportes_validos.
```

---

## ÉPICO 3 — Onboarding

**T-012** Criar as 3 telas de onboarding (conteúdo estático)
```r
RF: RF-07, RNF-03
Depende de: T-006
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-012-criar-as-3-telas-de-onboarding-conteúdo-estático)
Notas: Conteúdo embutido no app — sem requisição de rede.
       Tela 1: proposta de valor. Tela 2: como funciona. Tela 3: freemium.
       Componentes Compose Multiplatform compartilhados entre iOS e Android.
```

**T-013** Implementar tela de seleção de localização pós-onboarding
```r
RF: RF-08, RF-09
Depende de: T-012
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-013-implementar-tela-de-seleçao-de-localizaçao-pós-onboarding)
Notas: Duas opções: GPS automático ou busca de cidade manual.
       Exibir explicação contextual ANTES do prompt nativo de permissão.
       Campo de busca de cidade via Google Places Autocomplete API.
```

**T-014** Implementar Modo Degradado (localização negada)
```r
RF: RF-10, RF-11
Depende de: T-013
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-014-implementar-modo-degradado-localizaçao-negada)
Notas: Lista navegável de pontos por cidade digitada — sem geofences.
       Banner persistente no topo com botão "Ativar localização"
       que abre Intent/URL de configurações do dispositivo diretamente.
```

**T-015** Implementar tela de preferência de download de áudio no onboarding
```r
RF: RF-12, RF-13
Depende de: T-012
Complexidade: P
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-015-implementar-tela-de-preferência-de-download-de-áudio-no-onboarding)
Notas: Dois cards clicáveis: Modo Econômico vs Modo Offline.
       Salvar preferência no SQLDelight local.
       Flag onboarding_concluido salva localmente após conclusão.
```

---

## ÉPICO 4 — GeofenceOrchestrator

**T-016** Definir interface `GeofenceOrchestrator` no módulo shared
```r
RF: RF-14, RF-20
Depende de: T-006
Complexidade: P
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-016-definir-interface-geofenceorchestrator-no-módulo-shared)
Notas: Interface Kotlin com expect/actual.
       Métodos: inicializar(), marcarConsumido(), recalcularPorDeslocamento(),
       onEntradaGeofence(), onSaidaGeofence(), sincronizarPendencias().
       Flow<EstadoGeofences> para UI reativa.
```

**T-017** Implementar GeofenceOrchestrator no Android
```r
RF: RF-14 a RF-22
Depende de: T-016, T-005
Complexidade: G
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-017-implementar-geofenceorchestrator-no-android)
Notas: Usar Google Play Services Location (GeofencingClient).
       WorkManager para jobs de background (sincronização offline).
       Dwell time: timer cancelável de 30s por geofence.
       Macro-cerca: 1km, GEOFENCE_TRANSITION_EXIT.
```

**T-018** Implementar GeofenceOrchestrator no iOS
```r
RF: RF-14 a RF-22, RF-19
Depende de: T-016, T-005
Complexidade: G
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-018-implementar-geofenceorchestrator-no-ios)
Notas: Usar CoreLocation (CLLocationManager + CLCircularRegion).
       BackgroundTasks framework para jobs em background.
       Limite iOS: 19 pontos + 1 macro-cerca = 20 regiões total.
       Dwell time: NSTimer cancelável de 30s.
```

**T-019** Implementar schema SQLDelight local (PontoLocal)
```r
RF: RF-37, RF-42
Depende de: T-006
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-019-implementar-schema-sqldelight-local-pontolocal)
Notas: Tabela PontoLocal com todos os campos necessários.
       Queries geradas: pontosAtivos, substitutos, proximoSubstituto,
       pendentesSincronizacao, marcarConsumido, marcarSincronizado.
       Campo pendente_sync para controle de sincronização offline.
```

**T-020** Implementar lógica de cache de substitutos offline (5 pontos extras)
```r
RF: RF-15, RF-37
Depende de: T-019, T-017, T-018
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-020-implementar-lógica-de-cache-de-substitutos-offline-5-pontos-extras)
Notas: Ao inicializar: buscar 25 pontos, salvar todos no SQLDelight local.
       is_ativo = 1 para os primeiros 20, is_ativo = 0 para os 5 extras.
       Ao marcar consumido: mover próximo substituto para ativo.
```

**T-021** Implementar sincronização offline → online
```r
RF: RF-42
Depende de: T-019, T-017, T-018
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-021-implementar-sincronizaçao-offline-online)
Notas: Observar estado de conectividade via ConnectivityManager (Android)
       / NWPathMonitor (iOS).
       Ao reconectar: processar todos os registros com pendente_sync = true.
       Em caso de conflito (ponto já consumido remotamente): aceitar estado remoto.
```

**T-022** Implementar invalidação de cache por content_hash
```r
RF: RF-40, RF-41
Depende de: T-019, T-005
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-022-implementar-invalidaçao-de-cache-por-content_hash)
Notas: Ao abrir app em background ou após reconexão:
       Enviar lista {ponto_id, content_hash} para endpoint de verificação.
       Baixar metadados frescos apenas dos IDs com hash divergente.
       Não re-baixar MP3 se apenas metadados de contexto mudaram.
```

---

## ÉPICO 5 — Conteúdo & Player de Áudio

**T-023** Criar Edge Function `buscar-pontos-proximos`
```r
RF: RF-15
Depende de: T-005, T-001
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-023-criar-edge-function-buscar-pontos-proximos)
Notas: Deno/TypeScript. Chama a função RPC SQL T-005.
       Autenticação via JWT do usuário (não service_role).
       Retorna 25 pontos com todos os campos necessários para o app.
```

**T-024** Criar repositório de pontos no módulo shared (KMP)
```r
RF: RF-15, RF-28
Depende de: T-023, T-019
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-024-criar-repositório-de-pontos-no-módulo-shared-kmp)
Notas: PontosRepository: buscarPontosProximos(), verificarContentHash(),
       marcarConsumidoRemoto().
       Usar Ktor para chamadas HTTP.
       Cache-first: retorna dados locais enquanto busca remoto em background.
```

**T-025** Criar tela de lista de pontos (mapa + lista)
```r
RF: RF-27, RF-29
Depende de: T-024
Complexidade: G
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-025-criar-tela-de-lista-de-pontos-mapa-lista)
Notas: Duas tabs: Mapa e Lista.
       Pontos 1-10: card normal. Pontos 11-20: card com cadeado (se gratuito).
       Distância exibida em metros/km.
       Pull-to-refresh manual.
```

**T-026** Criar tela de detalhe de ponto turístico (card)
```r
RF: RF-23, RF-24, RF-25, RF-26, RF-27
Depende de: T-025
Complexidade: G
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-026-criar-tela-de-detalhe-de-ponto-turístico-card)
Notas: Campos: título, imagem, horário, custo, categorias, player de áudio.
       Player: play/pause, barra de progresso, duração.
       Botão "Ouvir versão [Completa/Rápida]" com download sob demanda.
       Botão "Já consumi este ponto".
       Botão "Reportar dado incorreto".
```

**T-027** Implementar player de áudio (Modo Econômico)
```r
RF: RF-24, RF-26
Depende de: T-026
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-027-implementar-player-de-áudio-modo-econômico)
Notas: Ao abrir card: exibe texto/imagem imediatamente (dados leves em cache).
       Inicia download do MP3 em background via Ktor.
       Exibe indicador de progresso de download.
       Reproduz assim que download completo usando MediaPlayer (Android)
       / AVAudioPlayer (iOS) via expect/actual.
```

**T-028** Implementar pré-download de áudios (Modo Offline)
```r
RF: RF-38, RF-43
Depende de: T-027, T-019
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-028-implementar-pré-download-de-áudios-modo-offline)
Notas: Se Modo Offline ativo: ao recalcular Top 20, iniciar download sequencial
       dos 20 MP3s da versão padrão em background.
       Ao remover ponto dos Top 20: deletar MP3 local correspondente.
       Mostrar progresso de download nas preferências.
```

---

## ÉPICO 6 — Notificações Push

**T-029** Configurar FCM (Firebase Cloud Messaging) para Android
```r
RF: RF-18
Depende de: T-006
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-029-configurar-fcm-firebase-cloud-messaging-para-android)
Notas: Integrar google-services.json.
       Registrar token FCM por usuário no Supabase.
       Atualizar token quando renovado pelo S.O.
```

**T-030** Configurar APNs (Apple Push Notification service) para iOS
```r
RF: RF-18
Depende de: T-006
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-030-configurar-apns-apple-push-notification-service-para-ios)
Notas: Configurar certificados APNs no Apple Developer.
       Registrar token APNs por usuário no Supabase.
       Atualizar token quando renovado pelo S.O.
```

**T-031** Implementar disparo de notificação local ao completar dwell time
```r
RF: RF-17, RF-18
Depende de: T-017, T-018, T-029, T-030
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-031-implementar-disparo-de-notificaçao-local-ao-completar-dwell-time)
Notas: Notificação local (não remota) disparada pelo próprio app após 30s.
       Título: nome do ponto. Subtítulo: frase curta descritiva.
       Deep link para abrir o card correto ao tocar na notificação.
       Cancelar notificação se usuário sair do raio antes dos 30s.
```

---

## ÉPICO 7 — Monetização

**T-032** Integrar AdMob (banner + nativo)
```r
RF: RF-29
Depende de: T-025
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-032-integrar-admob-banner-nativo)
Notas: Banner fixo no rodapé da tela de lista.
       Card nativo integrado na lista de pontos (a cada N cards).
       Sem intersticiais.
       Exibir somente para usuários sem nenhuma compra prévia.
       AdMob SDK via expect/actual.
```

**T-033** Implementar modal de paywall
```r
RF: RF-30, RF-32, RF-33
Depende de: T-025
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-033-implementar-modal-de-paywall)
Notas: Disparado ao tentar acessar ponto 11-20 (usuário gratuito).
       Exibe: preço da cidade + opção de passe vitalício.
       Na segunda compra: upsell vitalício com desconto proeminente.
       Usuário com compra prévia em outra cidade: modal diferenciado (sem ads, banner sutil).
```

**T-034** Implementar fluxo de compra por cidade (PIX + cartão)
```r
RF: RF-31, RF-35
Depende de: T-033
Complexidade: G
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-034-implementar-fluxo-de-compra-por-cidade-pix-cartao)
Notas: Integrar gateway de pagamento (ex: Stripe ou Mercado Pago para PIX).
       Registrar compra em compras_usuario após confirmação do gateway.
       Tratar falhas e timeouts de pagamento com mensagem clara.
       iOS: Apple IAP obrigatório para compras dentro do app.
       Android: Google Play Billing obrigatório.
```

**T-035** Implementar lógica de acesso por tier (free / cidade / vitalício)
```r
RF: RF-29, RF-33, RF-34, RF-36
Depende de: T-034, T-003
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-035-implementar-lógica-de-acesso-por-tier-free-cidade-vitalício)
Notas: Verificação server-side via RLS + consulta a compras_usuario.
       Usar view vw_usuario_acesso para determinar tier.
       Cache local do tier com TTL de 5 minutos (evitar query a cada interação).
```

---

## ÉPICO 8 — Agente de IA (Server-Side)

**T-036** Implementar interface abstrata `TTSProvider` e provider Google TTS
```r
RF: RF-55
Depende de: T-004
Complexidade: M
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-036-implementar-interface-abstrata-ttsprovider-e-provider-google-tts)
Notas: TypeScript/Deno. Interface com 3 métodos.
       Implementação inicial: Google Cloud TTS (Neural2).
       Vozes: pt-BR-Neural2-B, en-US-Neural2-D, es-ES-Neural2-B.
       Configurável via env var TTS_PROVIDER.
```

**T-037** Implementar pipeline de Batch Ingestion
```r
RF: RF-53, RF-54, RF-59, RF-60
Depende de: T-036, T-002, T-004
Complexidade: G
Prioridade: 🔴
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-037-implementar-pipeline-de-batch-ingestion)
Notas: Edge Function ou script Node.js executado manualmente pelo admin.
       Passos: busca Places API → deduplicação → geração LLM → TTS → upload Storage → persistência BD.
       Rate limiting: 1 req/s na Places API.
       Retry automático: 3 tentativas com backoff exponencial para cada passo.
       Log de progresso em tabela log_jobs.
       Calcular e salvar content_hash após inserção.
```

**T-038** Implementar pipeline de Auditoria Diária
```r
RF: RF-56
Depende de: T-037, T-001
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-038-implementar-pipeline-de-auditoria-diária)
Notas: Edge Function chamada pelo pg_cron às 02:00 UTC.
       Processar em lotes de 100 pontos.
       Comparar horarios_funcionamento e custo_ingresso com Places API.
       Atualizar content_hash se houver mudança.
```

**T-039** Implementar pipeline de Auditoria Prioritária
```r
RF: RF-57
Depende de: T-038
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-039-implementar-pipeline-de-auditoria-prioritária)
Notas: Edge Function disparada por evento (Supabase Database Webhooks
       quando contador_alertas atinge threshold por categoria).
       Corrigir dados → zerar contador → notificar usuários via FCM/APNs.
       Somente notificar se correção real foi aplicada.
```

**T-040** Implementar pipeline de Moderação de Sugestões
```r
RF: RF-58
Depende de: T-002, T-036
Complexidade: M
Prioridade: 🟢
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-040-implementar-pipeline-de-moderaçao-de-sugestões)
Notas: Edge Function chamada pelo pg_cron a cada 30 minutos.
       Sugestões com contador_votos >= 10 e status = 'aguardando_votos'.
       Validar via Places API + avaliar relevância turística via LLM.
       Aprovado: criar ponto com status 'pendente_moderacao', enfileirar no Batch.
       Rejeitado: salvar motivo, notificar admin via webhook/email.
```

**T-041** Configurar pg_cron jobs no Supabase
```r
RF: RF-56, RF-58
Depende de: T-038, T-040
Complexidade: P
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-041-configurar-pg_cron-jobs-no-supabase)
Notas: job_auditoria_diaria: 0 2 * * * (02:00 UTC diariamente)
       job_moderar_sugestoes: */30 * * * * (a cada 30 minutos)
       Configurar via SQL no Supabase.
```

---

## ÉPICO 9 — Crowdsourcing & Reputação

**T-042** Implementar fluxo de reporte de dado incorreto
```r
RF: RF-44, RF-45, RF-46
Depende de: T-026
Complexidade: M
Prioridade: 🟢
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-042-implementar-fluxo-de-reporte-de-dado-incorreto)
Notas: Bottomsheet com lista de categorias + campo de texto opcional.
       Ao confirmar: POST para reportes_usuario + incrementar controle_erros.
       Se threshold atingido: disparar webhook para auditoria prioritária.
```

**T-043** Implementar notificação de correção ao usuário que reportou
```r
RF: RF-47
Depende de: T-042, T-039, T-029, T-030
Complexidade: M
Prioridade: 🟢
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-043-implementar-notificaçao-de-correçao-ao-usuário-que-reportou)
Notas: Pipeline de auditoria marca reportes como corrigido = TRUE.
       Notificação push enviada apenas para reportes com correcao_aplicada = TRUE.
       Mensagem: "As informações de [nome do ponto] foram atualizadas. Obrigado!"
```

**T-044** Implementar sistema de reputação e badges
```r
RF: RF-48
Depende de: T-043
Complexidade: M
Prioridade: 🟢
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-044-implementar-sistema-de-reputaçao-e-badges)
Notas: Ao marcar correcao_aplicada = TRUE: incrementar pontos_reputacao e
       total_reportes_validos em reputacao_usuario.
       Verificar desbloqueio de badges: 1 / 5 / 20 correções.
       Exibir badges no perfil (T-011).
```

**T-045** Implementar fluxo de sugestão de novos pontos
```r
RF: RF-49, RF-50, RF-51, RF-52
Depende de: T-025
Complexidade: M
Prioridade: ⚪
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-045-implementar-fluxo-de-sugestao-de-novos-pontos)
Notas: Botão "Sugerir lugar" no mapa (Fase 3).
       Normalizar via Places Autocomplete → obter place_id.
       Se place_id já existe em ponto_turistico: feedback "já está no app".
       Se place_id já tem sugestão: incrementar votos_sugestao.
       Se novo: inserir sugestoes_usuario com contador = 1.
```

---

## ÉPICO 10 — Internacionalização

**T-046** Configurar sistema i18n com strings para pt-BR
```r
RF: RF-61, RF-62
Depende de: T-006
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-046-configurar-sistema-i18n-com-strings-para-pt-br)
Notas: Usar Lyricist (KMP i18n) ou solução equivalente.
       Arquivo strings_pt_BR, strings_en_US como fallback.
       Todas as strings de UI em arquivo externo — zero texto hardcoded.
```

**T-047** Adicionar strings en-US ao sistema i18n
```r
RF: RF-61, RF-62, RF-64
Depende de: T-046
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-047-adicionar-strings-en-us-ao-sistema-i18n)
Notas: Traduzir todas as strings de strings_pt_BR para en-US.
       Configurar fallback: dispositivo em idioma não suportado → en-US.
```

**T-048** Adicionar suporte a novo idioma no pipeline do agente (configurável)
```r
RF: RF-63, RF-64
Depende de: T-037
Complexidade: M
Prioridade: 🟢
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-048-adicionar-suporte-a-novo-idioma-no-pipeline-do-agente-configurável)
Notas: Parâmetro `idioma` no Batch Ingestion deve gerar conteúdo no idioma solicitado.
       Verificar suporte do TTSProvider ao idioma antes de iniciar.
       Inserir novo registro em conteudo_midia (não sobrescreve existentes).
```

---

## ÉPICO 11 — Painel Administrativo (MVP simplificado)

**T-049** Criar Edge Function de Batch Ingestion com autenticação admin
```r
RF: RF-65, RF-66
Depende de: T-037
Complexidade: M
Prioridade: 🟡
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-049-criar-edge-function-de-batch-ingestion-com-autenticaçao-admin)
Notas: Endpoint protegido: verificar role = 'admin' no JWT.
       Aceita payload JSON com lista de cidades e configurações.
       Retorna job_id para acompanhamento de progresso.
```

**T-050** Criar interface web simples de painel admin (MVP)
```r
RF: RF-65, RF-66, RF-67
Depende de: T-049
Complexidade: G
Prioridade: 🟢
  Prompt de Referencia: epic_01_fundacao.md(file:///c:/Projects/self-guide/prompts/epic_01_fundacao.md#prompt-para-t-050-criar-interface-web-simples-de-painel-admin-mvp)
Notas: HTML/JS simples ou Next.js mínimo.
       Funcionalidades MVP: iniciar batch ingestion, ver logs de jobs,
       listar sugestões rejeitadas pela IA para override manual,
       editar raio_gatilho_metros de pontos individuais.
       Autenticação via Supabase Auth (role admin).
```

---

## Sequência Sugerida de Desenvolvimento (MVP)

### Fase 0 — Fundação (1-2 semanas)
```
T-001 → T-002 → T-003 → T-004 → T-005 → T-006 → T-007
```

### Fase 1 — Auth + Conteúdo básico (1-2 semanas)
```
T-008 → T-019 → T-024 → T-023 → T-036 → T-037
```
*Ao final: admin consegue rodar Batch Ingestion e popular o banco.*

### Fase 2 — Geofencing + Notificações (2-3 semanas)
```
T-016 → T-017 + T-018 (paralelo) → T-020 → T-029 + T-030 (paralelo) → T-031
```
*Ao final: geofences funcionando com dwell time e notificações push.*

### Fase 3 — UI completa + Player (1-2 semanas)
```
T-025 → T-026 → T-027 → T-012 → T-013 → T-014 → T-015
```
*Ao final: app navegável com player de áudio funcional e onboarding completo.*

### Fase 4 — Monetização (1-2 semanas)
```
T-046 → T-047 → T-032 → T-033 → T-034 → T-035
```
*Ao final: app monetizável com AdMob + paywall + compras.*

### Fase 5 — Qualidade + Offline (1 semana)
```
T-021 → T-022 → T-028 → T-038 → T-039 → T-041
```
*Ao final: modo offline funcionando + auditorias automáticas ativas.*

### Fase 6 — Crowdsourcing + Admin (pós-MVP)
```
T-042 → T-043 → T-044 → T-045 → T-049 → T-050 → T-009 → T-010 → T-011 → T-040 → T-048
```

---

## Resumo do Backlog

| Épico | Tarefas | 🔴 Crítico | 🟡 Alta | 🟢 Média | ⚪ Pós-MVP |
|-------|---------|-----------|--------|---------|-----------|
| Fundação & Infra | 7 | 7 | — | — | — |
| Auth & Perfil | 4 | 1 | 2 | 1 | — |
| Onboarding | 4 | — | 4 | — | — |
| GeofenceOrchestrator | 7 | 5 | 2 | — | — |
| Conteúdo & Player | 6 | 4 | 1 | — | — |
| Notificações Push | 3 | 3 | — | — | — |
| Monetização | 4 | — | 4 | — | — |
| Agente de IA | 6 | 2 | 3 | 1 | — |
| Crowdsourcing | 4 | — | — | 3 | 1 |
| Internacionalização | 3 | — | 2 | 1 | — |
| Painel Admin | 2 | — | 1 | 1 | — |
| **Total** | **50** | **22** | **19** | **7** | **2** |
