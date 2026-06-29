# Prompts de Desenvolvimento — Épico 5: Conteúdo & Player de Áudio

Este arquivo contém os prompts detalhados para cada tarefa do Épico 5. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-023: Criar Edge Function `buscar-pontos-proximos`

**Contexto do Projeto:**
Desejamos que o app móvel interaja com o banco de dados Supabase através de uma API HTTP limpa. A busca espacial complexa do PostGIS é encapsulada em uma Edge Function escrita em TypeScript (Deno runtime) servida pelo Supabase.

**Objetivo:**
Escrever a Edge Function `buscar-pontos-proximos` que valida o token JWT do usuário, recebe coordenadas geográficas e idioma e invoca a função RPC SQL criada anteriormente.

**Especificação Técnica de Referência:**
Consulte a seção "Edge Function: buscar-pontos-proximos" em [05_supabase_guide.md](file:///c:/Projects/self-guide/refinamento/05_supabase_guide.md#L245-L291) e o DDL em [03_database_schema_final.md](file:///c:/Projects/self-guide/refinamento/03_database_schema_final.md).

**Instruções de Implementação:**
1. Inicialize a estrutura de arquivos da Edge Function usando a CLI do Supabase: `supabase functions new buscar-pontos-proximos`.
2. No arquivo `index.ts` da função:
   - Importe o servidor HTTP do Deno (`serve` de `https://deno.land/std/http/server.ts`).
   - Importe o client oficial do Supabase JS (`createClient` de `https://esm.sh/@supabase/supabase-js`).
   - Leia os parâmetros do corpo da requisição POST: `usuario_id`, `latitude`, `longitude`, `idioma` (fallback 'pt-BR').
   - Obtenha as chaves de ambiente `SUPABASE_URL` e `SUPABASE_SERVICE_ROLE_KEY` (armazenadas em variáveis de ambiente da nuvem do Supabase).
   - Instancie o cliente do Supabase utilizando a chave de acesso administrativa `service_role` (necessário pois a query precisa ignorar RLS e consultar registros da tabela `historico_usuario`).
   - Faça a chamada RPC:
     ```typescript
     const { data, error } = await supabase.rpc('buscar_pontos_proximos', {
       p_usuario_id: usuario_id,
       p_latitude: latitude,
       p_longitude: longitude,
       p_idioma: idioma
     });
     ```
   - Trate erros de banco retornando código `500` com JSON descritivo.
   - Retorne o JSON com a lista de 25 pontos próximos com cabeçalho `Content-Type: application/json`.

**Critérios de Aceitação e Verificação:**
- A Edge Function foi deployada com sucesso usando a CLI: `supabase functions deploy buscar-pontos-proximos`.
- Um teste enviando uma requisição HTTP POST válida (com JWT no header `Authorization` e coordenadas em JSON no body) retorna com código HTTP `200` e o payload JSON com os pontos corretos em ordem de rank.

---

## Prompt para T-024: Criar repositório de pontos no módulo shared (KMP)

**Contexto do Projeto:**
A arquitetura do app mobile delega o acesso a dados para uma classe comum `PontosRepository` no módulo KMP shared. Este repositório realiza requisições HTTP via Ktor, armazena os dados frescos localmente no SQLDelight e expõe uma estratégia de "Cache-First" para inicialização rápida da tela.

**Objetivo:**
Implementar a classe `PontosRepository` em Kotlin no módulo shared comum para prover dados de pontos turísticos à aplicação.

**Especificação Técnica de Referência:**
Consulte a seção "Consumindo a API no App Mobile (Kotlin + Ktor)" em [05_supabase_guide.md](file:///c:/Projects/self-guide/refinamento/05_supabase_guide.md#L139-L208).

**Instruções de Implementação:**
1. Crie o arquivo `PontosRepository.kt` no diretório `shared/src/commonMain/kotlin/repository/`.
2. Configure o construtor da classe recebendo o cliente HTTP do Ktor (`HttpClient`) e o banco de dados do SQLDelight (`LocalDatabase`).
3. Implemente as funções assíncronas (`suspend`):
   - `buscarPontosProximos(usuarioId: String, lat: Double, lng: Double, idioma: String): List<PontoTuristico>`:
     - Realiza uma chamada HTTP POST via Ktor para o endpoint da Edge Function `buscar-pontos-proximos`.
     - Adiciona a autenticação JWT do usuário no cabeçalho.
     - Decodifica o JSON de retorno em objetos Kotlin (`PontoTuristico`) usando `kotlinx.serialization`.
     - Limpa os dados obsoletos e insere a lista fresca no banco SQLite local.
     - Em caso de falha de conexão (offline), consome e retorna os pontos salvos na tabela SQLite `PontoLocal` usando a query `pontosAtivos` e `substitutos`.
   - `marcarConsumidoRemoto(usuarioId: String, pontoId: String): Unit`:
     - Realiza um POST para o Supabase atualizando a tabela `historico_usuario` com `status_consumo = 'consumido'`.
   - `verificarContentHash(pontos: List<Pair<String, String>>): List<String>`:
     - Envia lista de IDs e hashes para a Edge Function de invalidação e retorna lista de IDs divergentes.

**Critérios de Aceitação e Verificação:**
- O repositório compila sem erros no gradle.
- Escreva um teste unitário mockando o `HttpClient` do Ktor e o banco SQLDelight. Certifique-se de que a chamada do repositório tente acessar a rede, salve o resultado no banco local com sucesso e retorne do banco local em caso de falha de rede.

---

## Prompt para T-025: Criar tela de lista de pontos (mapa + lista)

**Contexto do Projeto:**
A tela principal do SelfGuide deve apresentar ao usuário os pontos turísticos ao seu redor de forma intuitiva, dividida em uma visualização de Mapa e uma de Lista, lidando visualmente com o paywall (pontos bloqueados).

**Objetivo:**
Construir a interface principal do aplicativo contendo as visualizações de mapa georreferenciado e lista de cards de pontos usando Compose Multiplatform.

**Requisitos Relacionados:**
- RF-27 (Exibição de dados básicos dos pontos turísticos na lista)
- RF-29 (Exibição de pontos bloqueados na UI com cadeado para usuários gratuitos)
- UI/UX Guidelines: Premium Designs, Rich Aesthetics (cantos arredondados, imagens limpas, sombras, micro-animações, cards estilizados de anúncio AdMob "Patrocinado").

**Instruções de Implementação:**
1. Crie a tela principal `PontosListScreen.kt` em `shared/src/commonMain/kotlin/ui/pontos/`.
2. Implemente a barra de abas superior (Tabs) alternando entre:
   - **"Lista":** Uma lista vertical rolável de pontos baseada no `Flow<EstadoGeofences>` do orquestrador.
     - Exiba a foto, título do local, categoria principal e distância em metros.
     - Os primeiros 10 pontos (ranks 1 a 10) devem ter visualização normal e interativa.
     - Os pontos de 11 a 20 (se o usuário for gratuito) devem ser mostrados com efeito blur (desfocado), um ícone de cadeado e um banner discreto "Desbloquear Cidade".
     - Insira um card patrocinado formatado a cada N registros de pontos caso o usuário seja gratuito.
   - **"Mapa":** Integração com mapa nativo (Mapbox/Google Maps via expect/actual) renderizando pinos (markers) nas coordenadas de cada um dos 20 pontos ativos. Pinos premium devem ter cor diferenciada ou ícone de cadeado.
3. Adicione o comportamento de pull-to-refresh para recarregar manualmente a query.

**Critérios de Aceitação e Verificação:**
- A tela compila e renderiza de forma idêntica e fluida no Android e iOS.
- Para um usuário fictício com tier gratuito, os cards do rank 11 ao 20 aparecem bloqueados com cadeado e blur, sem permitir acesso à tela de detalhes.
- Para um usuário pagante, todos os 20 cards aparecem desbloqueados e acessíveis.

---

## Prompt para T-026: Criar tela de detalhe de ponto turístico (card)

**Contexto do Projeto:**
Ao clicar em um ponto desbloqueado ou ao tocar em uma notificação de geofence disparada, a tela de detalhe (card de ponto turístico) é aberta, apresentando texto, imagem e o player de áudio do guia.

**Objetivo:**
Desenvolver a tela de detalhes rica de um ponto turístico no Compose Multiplatform.

**Requisitos Relacionados:**
- RF-23 (Versões rápidas e completas de áudio)
- RF-24 (Configuração de preferência padrão)
- RF-25 (Botão para ouvir versão alternativa)
- RF-27 (Visualização de metadados: horários, preços, categorias)
- RF-44 (Botão de reporte de dado incorreto)

**Instruções de Implementação:**
1. Crie o arquivo `PontoDetalheScreen.kt` em `shared/src/commonMain/kotlin/ui/pontos/`.
2. A tela deve apresentar em layout premium de alta legibilidade:
   - Imagem de capa em destaque com gradiente overlay.
   - Título oficial do local e categoria.
   - Tags horizontais das categorias (ex: "Histórico", "Natureza").
   - Detalhes de contexto formatados: Horário de funcionamento (`horarios_funcionamento`), custo do ingresso (`custo_ingresso`).
   - Botão flutuante ou de destaque: "Já consumi este ponto" (chama o método de mesmo nome no orquestrador).
   - Botão discreto no canto superior ou rodapé: "Reportar dado incorreto".
3. Implemente a seção do Player de Áudio (play, pause, slider de progresso, tempo decorrido e total).
4. Adicione um seletor ou botão alternador de versão: "Ouvir Versão Completa (3min)" / "Ouvir Versão Rápida (45s)". Este botão deve acionar o download da respectiva trilha de áudio no repositório.

**Critérios de Aceitação e Verificação:**
- A tela de detalhes carrega instantaneamente as informações de texto e imagem que já estavam salvas no cache local.
- O clique em "Já consumi" executa a ação e fecha a tela, retornando para a lista atualizada.
- Clicar em "Reportar dado" abre o modal de reportes (implementado em Épico 9).

---

## Prompt para T-027: Implementar player de áudio (Modo Econômico)

**Contexto do Projeto:**
No Modo Econômico (padrão do app), o áudio MP3 de um ponto turístico não é baixado de forma antecipada para preservar os dados móveis do usuário. O download ocorre sob demanda no momento em que ele acessa a tela de detalhes.

**Objetivo:**
Escrever a lógica de download e o player nativo (`actual`) para reprodução assíncrona do MP3 sob demanda no app.

**Requisitos Relacionados:**
- RF-24 (Configuração de preferência do usuário: rápida vs completa)
- RF-26 (Download em background na abertura da notificação e início de reprodução automático)
- RNF-08 (Áudio MP3 de 128kbps)

**Instruções de Implementação:**
1. No módulo compartilhado, utilize Ktor para efetuar o download assíncrono do arquivo MP3 a partir da `audio_url_rapido` ou `audio_url_completo` (obtida do Supabase Storage).
2. Durante o download:
   - Exiba um indicador de progresso (ProgressBar circular) com percentual sobre o botão de play na UI.
   - Mantenha a interface responsiva (permitindo ao usuário ler o roteiro de texto e visualizar fotos).
3. Implemente a reprodução do áudio via mecanismo de expect/actual:
   - **Android (actual):** Encapsular o `MediaPlayer` nativo do Android.
   - **iOS (actual):** Encapsular a classe `AVAudioPlayer` do iOS.
4. Quando o buffer de áudio for carregado por completo, inicie automaticamente a reprodução do som se a tela foi aberta via notificação nativa (dwell time).
5. Gerencie a sessão de áudio do sistema para pausar caso o usuário receba uma ligação ou desconecte os fones.

**Critérios de Aceitação e Verificação:**
- Ao acessar a tela de detalhes, o download do MP3 inicia automaticamente e o indicador de carregamento é exibido na UI.
- Uma vez carregado, o player executa o som sem ruídos e os botões de pause/play, volume e slider de progresso funcionam corretamente sincronizados com o tempo do áudio.

---

## Prompt para T-028: Implementar pré-download de áudios (Modo Offline)

**Contexto do Projeto:**
Para turistas que preferem não consumir dados na rua ou vão visitar regiões sem sinal telefônico estável, o Modo Offline realiza o download antecipado das 20 trilhas de áudio da versão configurada assim que a lista ativa é calculada.

**Objetivo:**
Escrever o serviço de background para pré-download sequencial de áudios locais e gerenciamento do storage.

**Requisitos Relacionados:**
- RF-38 (Pré-download de MP3s da versão padrão para os 20 pontos ativos)
- RF-43 (Deleção de MP3s de pontos fora da lista de ativos para limpar storage)
- RNF-12 (Informar usuário sobre consumo de armazenamento nas preferências)

**Instruções de Implementação:**
1. No `PontosRepository` ou em um serviço dedicado (`AudioDownloadManager`):
   - Escute as mudanças no `estadoAtual` das geofences exposto pelo orquestrador.
   - Caso o `Modo Offline` esteja ativo nas configurações do app:
     - Identifique a lista de 20 pontos ativos.
     - Inicie uma fila de download sequencial (em thread de segundo plano) dos arquivos MP3 correspondentes à versão padrão escolhida pelo usuário.
     - Salve os arquivos MP3 localmente no diretório de arquivos privado do app (Internal Storage no Android / Documents Directory no iOS).
     - Atualize a coluna `audio_rapido_cached` ou `audio_completo_cached` do SQLDelight local para `1` para cada ponto concluído.
2. Limpeza de Cache: Quando a lista ativa de 20 pontos for recalculada por deslocamento (macro-cerca), identifique os pontos antigos que foram removidos. Exclua fisicamente os arquivos MP3 destes pontos do armazenamento local do celular para evitar acúmulo de arquivos obsoletos.
3. Exponha na UI de configurações um indicador de uso de disco: *"Dados do Guia ocupando X MB em disco"* com opção de *"Limpar cache"*.

**Critérios de Aceitação e Verificação:**
- Com o Modo Offline ativado, ao inicializar uma cidade, o download dos 20 áudios é disparado em background sequencialmente.
- Ao alternar para o Modo Avião (sem internet), os áudios dos 20 pontos salvos em disco tocam instantaneamente ao abrir os cards na UI.
- Ao forçar deslocamento do GPS (mudança de macro-cerca), os arquivos MP3 correspondentes aos locais antigos que saíram da lista são limpos do storage interno do aparelho.
