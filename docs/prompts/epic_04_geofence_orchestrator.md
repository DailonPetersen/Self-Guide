# Prompts de Desenvolvimento — Épico 4: GeofenceOrchestrator

Este arquivo contém os prompts detalhados para cada tarefa do Épico 4. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-016: Definir interface `GeofenceOrchestrator` no módulo shared

**Contexto do Projeto:**
Para garantir um acoplamento fraco e permitir que outras camadas (como UI e sincronização) não acessem APIs de geolocalização específicas do Android ou iOS, criamos um componente orquestrador comum usando o mecanismo de expect/actual do Kotlin Multiplatform.

**Objetivo:**
Definir a assinatura e os modelos de dados da interface `GeofenceOrchestrator` no módulo `shared` comum.

**Especificação Técnica de Referência:**
Consulte a seção "Interface Pública (KMP Shared)" em [06_geofence_orchestrator_spec.md](file:///c:/Projects/self-guide/refinamento/06_geofence_orchestrator_spec.md#L17-L79).

**Instruções de Implementação:**
1. Crie o arquivo `GeofenceOrchestrator.kt` no diretório `shared/src/commonMain/kotlin/geofence/`.
2. Escreva os modelos de dados básicos:
   - `Coordenadas(latitude: Double, longitude: Double)`
   - `EstadoGeofences(pontosAtivos: List<PontoTuristico>, pontosSubstitutos: List<PontoTuristico>, pendentesSync: List<String>, ultimaPosicao: Coordenadas)`
   - `ResultadoInicializacao(sucesso: Boolean, pontosRegistrados: Int, erro: String? = null)`
   - `ResultadoSubstituicao(pontoRemovidoId: String, pontoInseridoId: String?, sincronizadoRemoto: Boolean)`
3. Defina a interface `GeofenceOrchestrator` com as seguintes funções declaradas:
   - `suspend fun inicializar(posicao: Coordenadas, usuarioId: String, idioma: String): ResultadoInicializacao`
   - `suspend fun marcarConsumido(pontoId: String, usuarioId: String): ResultadoSubstituicao`
   - `suspend fun recalcularPorDeslocamento(novaPosicao: Coordenadas, usuarioId: String, idioma: String): ResultadoInicializacao`
   - `fun onSaidaGeofence(pontoId: String)`
   - `fun onEntradaGeofence(pontoId: String)`
   - `suspend fun sincronizarPendencias(usuarioId: String)`
   - `val estadoAtual: Flow<EstadoGeofences>`
4. Declare a classe expect correspondente para instanciamento nas plataformas: `expect class GeofenceOrchestratorImpl`.

**Critérios de Aceitação e Verificação:**
- O arquivo Kotlin é compilado e indexado sem erros pelo Gradle.
- A interface expõe todos os tipos de retorno, assinaturas e fluxos reativos (`Flow`) descritos na especificação.

---

## Prompt para T-017: Implementar GeofenceOrchestrator no Android

**Contexto do Projeto:**
No Android, o gerenciamento de cercas virtuais em background é feito através do SDK do Google Play Services Location API. O app deve ser acordado por um BroadcastReceiver nativo quando o turista entra em um raio de gatilho ou sai da macro-cerca.

**Objetivo:**
Escrever a implementação nativa (`actual class`) do `GeofenceOrchestrator` no módulo Android, configurando as cercas ativas e a macro-cerca de 1km.

**Especificação Técnica de Referência:**
Consulte a seção "Implementação Android (expect/actual)" em [06_geofence_orchestrator_spec.md](file:///c:/Projects/self-guide/refinamento/06_geofence_orchestrator_spec.md#L83-L187).

**Instruções de Implementação:**
1. Crie o arquivo `GeofenceOrchestratorImpl.kt` no diretório `shared/src/androidMain/kotlin/geofence/`.
2. Utilize o `LocationServices.getGeofencingClient(context)` para fazer chamadas à API nativa do Google.
3. No método `inicializar`:
   - Busque a lista de 25 pontos através do `PontosRepository` (20 ativos + 5 substitutos) e salve no banco SQLDelight.
   - Crie uma lista de objetos `Geofence` nativos para os 20 pontos ativos. Use as coordenadas de cada ponto e o raio `raio_gatilho_metros` definido.
   - Registre essas geofences com as transições `ENTER` e `EXIT`.
   - Registre a macro-cerca móvel: uma `Geofence` extra chamada "MACRO_CERCA", centralizada nas coordenadas do usuário com raio fixo de 1km (`1000f`) e transição `EXIT`.
4. Dwell Time: No `onEntradaGeofence`, inicie uma coroutine com delay de 30 segundos (30.000 ms). Se o usuário permanecer no local ao final do tempo, dispare o fluxo de notificação. No `onSaidaGeofence`, cancele a coroutine (timer) correspondente se o ID do ponto for o mesmo.
5. Registre um `BroadcastReceiver` no arquivo `AndroidManifest.xml` do `:androidApp` para escutar e processar os eventos geográficos em segundo plano e com tela bloqueada.

**Critérios de Aceitação e Verificação:**
- O orquestrador compila com sucesso para Android.
- Escreva um teste de integração no emulador Android simulando coordenadas de GPS e verifique se:
  - 20 geofences nativas e a macro-cerca de 1km são cadastradas nas APIs do Google Play Services.
  - Ao cruzar o gatilho, a entrada e saída disparam os callbacks de Dwell Time.
  - O timer é cancelado caso ocorra saída antes dos 30s.

---

## Prompt para T-018: Implementar GeofenceOrchestrator no iOS

**Contexto do Projeto:**
O iOS possui restrições rígidas no ciclo de vida de aplicativos em background e impõe o limite máximo de 20 geofences simultâneas monitoradas via CoreLocation por app. Usamos 19 pontos ativos + 1 slot para a macro-cerca.

**Objetivo:**
Escrever a implementação nativa (`actual class`) do `GeofenceOrchestrator` no iOS utilizando a biblioteca nativa `CoreLocation` da Apple.

**Especificação Técnica de Referência:**
Consulte a seção "Implementação iOS (expect/actual)" em [06_geofence_orchestrator_spec.md](file:///c:/Projects/self-guide/refinamento/06_geofence_orchestrator_spec.md#L193-L247).

**Instruções de Implementação:**
1. Crie o arquivo `GeofenceOrchestratorImpl.kt` no diretório `shared/src/iosMain/kotlin/geofence/`.
2. Utilize as classes do CoreLocation: `CLLocationManager` e `CLCircularRegion`.
3. No método `inicializar`:
   - Limite a busca e inserção de geofences a no máximo 19 slots ativos (ranks 1 a 19) e salve os substitutos remanescentes no banco local.
   - Instancie objetos `CLCircularRegion` para os 19 locais com seus respectivos raios, identificados pelo UUID do ponto turistico.
   - Configure a macro-cerca (20º slot) com o identificador `"MACRO_CERCA"`, raio de 1km (`1000.0`) e centralizado na posição do turista.
   - Solicite monitoramento das regiões via `locationManager.startMonitoringForRegion()`.
4. Dwell Time: Implemente o timer no `onEntradaGeofence` utilizando `NSTimer` agendado para 30 segundos (`30.0` s) sem repetição. Se o usuário sair do raio antes do fim do tempo, invalide o timer (`NSTimer.invalidate()`) no método `onSaidaGeofence`.
5. Integre o monitoramento de background com o framework `BackgroundTasks` do iOS para sincronização posterior.

**Critérios de Aceitação e Verificação:**
- O código compila sem falhas gerando o framework do iOS.
- Teste simulando coordenadas de GPS no simulador do Xcode e verifique se:
  - 19 regiões e a macro-cerca de 1km aparecem na lista de monitoradas do `CLLocationManager`.
  - Ao cruzar o gatilho, as notificações locais são engatadas após 30 segundos contínuos e canceladas se houver saída prévia.

---

## Prompt para T-019: Implementar schema SQLDelight local (PontoLocal)

**Contexto do Projeto:**
Como o app opera offline de forma transparente para o turista, necessitamos de uma tabela relacional SQLite local que contenha os dados dos 20 pontos ativos e 5 substitutos pré-carregados, rastreando estados de consumo e sincronização.

**Objetivo:**
Criar o arquivo de especificação do banco de dados local com SQLDelight, configurando a tabela `PontoLocal` e as queries fundamentais geradas automaticamente.

**Especificação Técnica de Referência:**
Consulte a seção "Banco Local (SQLDelight)" em [06_geofence_orchestrator_spec.md](file:///c:/Projects/self-guide/refinamento/06_geofence_orchestrator_spec.md#L251-L300).

**Instruções de Implementação:**
1. Crie o arquivo `PontoLocal.sq` no caminho `shared/src/commonMain/sqldelight/database/`.
2. Escreva o DDL de criação da tabela `PontoLocal` com as colunas:
   - `id` (TEXT PRIMARY KEY NOT NULL)
   - `nome` (TEXT NOT NULL)
   - `latitude` (REAL NOT NULL)
   - `longitude` (REAL NOT NULL)
   - `raio_gatilho_metros` (INTEGER NOT NULL DEFAULT 50)
   - `titulo_local` (TEXT NOT NULL)
   - `roteiro_rapido` (TEXT NOT NULL)
   - `roteiro_completo` (TEXT NOT NULL)
   - `audio_url_rapido` (TEXT), `audio_url_completo` (TEXT)
   - `imagem_url` (TEXT), `horarios_funcionamento` (TEXT), `custo_ingresso` (TEXT)
   - `content_hash` (TEXT NOT NULL)
   - `rank` (INTEGER NOT NULL) - index de ordenação espacial (1 a 25)
   - `is_premium` (INTEGER NOT NULL) - 0 para free, 1 para premium
   - `is_ativo` (INTEGER NOT NULL) - 1 para ativo na cerca nativa, 0 para substituto offline
   - `status_consumo` (TEXT NOT NULL DEFAULT 'nao_visto') - 'nao_visto' ou 'consumido'
   - `pendente_sync` (INTEGER NOT NULL DEFAULT 0) - 1 se marcado offline e sem sincronizar com o Supabase
   - `audio_rapido_cached` (INTEGER NOT NULL DEFAULT 0), `audio_completo_cached` (INTEGER NOT NULL DEFAULT 0)
   - `data_cache` (TEXT NOT NULL) - ISO8601 string
3. Adicione as queries nomeadas abaixo da tabela:
   - `pontosAtivos` (retorna ordenados por rank onde `is_ativo = 1`)
   - `substitutos` (retorna os 5 adicionais ordenados por rank onde `is_ativo = 0`)
   - `proximoSubstituto` (retorna o substituto mais próximo na cauda: limit 1 onde `is_ativo = 0`)
   - `pendentesSincronizacao` (retorna registros onde `pendente_sync = 1`)
   - `marcarConsumido` (atualiza `status_consumo = 'consumido'` e `pendente_sync = 1`)
   - `marcarSincronizado` (atualiza `pendente_sync = 0`)
   - `deletarPontosInativos` (deleta pontos que saíram do cache após recálculo)

**Critérios de Aceitação e Verificação:**
- O gradle gera com sucesso as classes Java/Kotlin do SQLDelight (`PontoLocalQueries`, `PontoLocal`, etc.) executando `./gradlew generateSqlDelightInterface`.
- Escreva um teste unitário local rodando SQLite em memória e certifique-se de que as queries de busca, inserção e atualização de estado de sincronização e consumo funcionem exatamente conforme especificado.

---

## Prompt para T-020: Implementar lógica de cache de substitutos offline (5 pontos extras)

**Contexto do Projeto:**
Se um turista descer uma rua fora de cobertura de internet móvel e marcar o ponto atual como "Já consumi", o orquestrador não pode fazer uma requisição remota ao Supabase para preencher a cerca nativa vaga do S.O. Ele deve substituir a cerca nativa usando um dos 5 pontos extras mantidos no cache de substituição local.

**Objetivo:**
Codificar a lógica de gerenciamento e preenchimento de geofences usando os pontos substitutos do cache local caso o usuário consuma pontos offline.

**Requisitos Relacionados:**
- RF-15 (Buscar 25 pontos - 20 ativos + 5 substitutos)
- RF-20 (Marcação de consumo: remove atual, adiciona substituto do cache local)
- Decisões de Refinamento: Decisão 3 (Comportamento offline ao marcar consumido)

**Instruções de Implementação:**
1. No método `marcarConsumido` do `GeofenceOrchestratorImpl`:
   - Remova a geofence nativa do ponto consumido do sistema operacional.
   - Atualize o status no banco SQLDelight usando `marcarConsumido(pontoId)`.
   - Consulte o banco local para obter o próximo ponto inativo usando a query `proximoSubstituto`.
   - Se houver um substituto disponível:
     - Adicione este ponto nas geofences nativas do S.O.
     - Atualize o registro deste ponto no SQLDelight para `is_ativo = 1` usando um método de escrita (ex: `moverSubstitutoParaAtivo`).
     - Altere o `rank` do novo ponto ativo para assumir a posição liberada na UI.
2. Certifique-se de que a lógica ocorra de forma síncrona localmente para que a UI do app reaja exibindo a nova geofence inserida na lista.

**Critérios de Aceitação e Verificação:**
- Em teste com internet desligada: ao marcar um ponto ativo como consumido, ele some da lista ativa da UI.
- O primeiro ponto da cauda de substitutos (rank 21) é ativado nativamente no S.O. e passa a aparecer como ativo (com seu respectivo rank redefinido) na tela do app.
- O banco local SQLDelight atualiza o status de consumo do ponto antigo para `'consumido'` e altera a flag `is_ativo` do substituto de `0` para `1`.

---

## Prompt para T-021: Implementar sincronização offline → online

**Contexto do Projeto:**
As marcações de consumo efetuadas em modo offline (por exemplo, dentro de monumentos de pedra sem recepção) são registradas no banco SQLite com `pendente_sync = 1`. Precisamos de um mecanismo que monitore a conectividade do dispositivo e sincronize esses dados assim que a internet retornar.

**Objetivo:**
Escrever a lógica de monitoramento de internet e sincronização de dados pendentes do banco SQLite local com o Supabase.

**Requisitos Relacionados:**
- RF-20 (Sincronizar histórico de consumo em background)
- RF-42 (Operações realizadas offline sincronizadas automaticamente na reconexão)

**Instruções de Implementação:**
1. Crie um monitor de conectividade de rede usando as APIs nativas do dispositivo (ConnectivityManager no Android / NWPathMonitor no iOS) exposto no módulo shared como um `Flow<Boolean>`.
2. Implemente a função `sincronizarPendencias` em `GeofenceOrchestratorImpl`:
   - Leia todos os registros do SQLDelight onde `pendente_sync = 1` utilizando a query `pendentesSincronizacao`.
   - Para cada ponto com pendência, realize uma requisição HTTP POST para o Supabase para atualizar a tabela remota `historico_usuario` para o usuário logado com `status_consumo = 'consumido'`.
   - Em caso de sucesso da requisição, execute no SQLDelight `marcarSincronizado(pontoId)`.
   - Se ocorrer erro de rede, mantenha o estado local e aguarde a próxima janela de conexão.
3. Agende a execução desse fluxo de sync de forma resiliente:
   - **Android:** Via WorkManager (com constraints `NetworkType.CONNECTED`).
   - **iOS:** Via framework `BackgroundTasks` (schedules ao reconectar).

**Critérios de Aceitação e Verificação:**
- Com o app simulando rede instável: salve marcações com `pendente_sync = 1` no SQLDelight.
- Ao restabelecer a conexão com a internet, o monitor de rede deve disparar o `sincronizarPendencias`.
- As requisições HTTP correspondentes são enviadas ao Supabase.
- Verifique que os pontos no banco do Supabase foram atualizados e a coluna `pendente_sync` local mudou para `0`.

---

## Prompt para T-022: Implementar invalidação de cache por content_hash

**Contexto do Projeto:**
Os dados dos pontos turísticos (como preços e horários) podem sofrer alterações no servidor devido a auditorias da IA. Para manter o app sincronizado sem forçar o download diário de todos os metadados de áudio e texto pesados, o app compara um hash leve gerado pelo servidor.

**Objetivo:**
Escrever a funcionalidade de invalidação e atualização seletiva do cache de pontos com base em verificação de hash.

**Requisitos Relacionados:**
- RF-40 (Invalidação por hash de versão)
- RF-41 ( content_hash atualizado pelo servidor em modificações)
- Decisões de Refinamento: Decisão 15 (Hash de versão por ponto com sync em background)

**Instruções de Implementação:**
1. No `PontosRepository` do módulo shared, crie um método `verificarContentHash(pontos: List<Pair<String, String>>): List<String>` que realiza um POST HTTP para uma RPC/Edge Function do Supabase enviando uma lista de IDs de pontos e seus respectivos `content_hash` locais.
2. A Edge Function do Supabase deve retornar apenas os IDs dos pontos cujos hashes no servidor sejam diferentes dos enviados pelo cliente.
3. No app móvel:
   - Ao inicializar o orquestrador ou ao retornar do background, colete os IDs e hashes dos pontos salvos no SQLDelight local e chame o `verificarContentHash`.
   - Para cada ID retornado pelo servidor (hash divergente), realize uma requisição de busca seletiva para obter os dados frescos.
   - Atualize os dados do ponto no SQLDelight local com os novos metadados e o novo `content_hash`.
   - Não re-baixe o arquivo MP3 local a menos que a URL do áudio ou o próprio hash de áudio tenha mudado.

**Critérios de Aceitação e Verificação:**
- Execute a rotina de hash com dados locais desatualizados simulados.
- Verifique se a requisição de rede envia o payload de ID e Hash corretamente.
- Certifique-se de que apenas os pontos retornados pelo servidor com hash divergente sejam atualizados no SQLite local.
- Garanta que o áudio MP3 local seja preservado caso a modificação seja apenas textual (ex: preço de ingresso).
