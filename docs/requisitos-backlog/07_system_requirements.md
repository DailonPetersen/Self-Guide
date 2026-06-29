# Requisitos do Sistema — Tour Guiado Automatizado
> Baseado nas 21 decisões do refinamento. Formato: RF = Requisito Funcional, RNF = Requisito Não Funcional.

---

## Épicos

| ID | Épico |
|----|-------|
| E1 | Autenticação & Perfil de Usuário |
| E2 | Onboarding |
| E3 | Geofencing & Notificações |
| E4 | Conteúdo de Pontos Turísticos |
| E5 | Monetização & Paywall |
| E6 | Cache & Modo Offline |
| E7 | Crowdsourcing & Reputação |
| E8 | Agente de IA — Pipelines Server-Side |
| E9 | Internacionalização |
| E10 | Painel Administrativo |

---

## E1 — Autenticação & Perfil de Usuário

### Requisitos Funcionais

**RF-01** O sistema deve permitir cadastro com email e senha.

**RF-02** O sistema deve permitir login social via Google (Android e iOS).

**RF-03** O sistema deve permitir login social via Apple (obrigatório para iOS App Store).

**RF-04** O sistema deve manter a sessão do usuário ativa entre reinicializações do app via token JWT persistido localmente.

**RF-05** O sistema deve associar o histórico de consumo, compras e reputação ao `usuario_id` gerado pelo Supabase Auth.

**RF-06** O perfil do usuário deve exibir: nome, badges de reputação conquistados, total de pontos visitados e cidade(s) desbloqueada(s).

### Requisitos Não Funcionais

**RNF-01** Tokens de sessão devem ser armazenados no Keychain (iOS) e EncryptedSharedPreferences (Android) — nunca em armazenamento plano.

**RNF-02** A `service_role_key` do Supabase nunca deve ser exposta no app mobile — apenas a `anon_key`.

---

## E2 — Onboarding

### Requisitos Funcionais

**RF-07** Na primeira execução, o app deve exibir exatamente 3 telas de onboarding antes de qualquer outra tela:
- Tela 1: proposta de valor (geofences automáticas)
- Tela 2: como funciona (caminhar → notificação → áudio)
- Tela 3: modelo freemium (10 grátis, desbloqueie mais)

**RF-08** Após as telas de onboarding, o app deve apresentar duas opções de localização:
- "Usar minha localização atual" → solicita permissão de GPS
- "Escolher uma cidade" → exibe campo de busca de cidade

**RF-09** Ao solicitar permissão de localização, o app deve exibir uma explicação contextual antes do prompt nativo do S.O.: "Usamos sua localização para disparar áudios automáticos quando você se aproxima de pontos históricos, mesmo com o app fechado."

**RF-10** Se o usuário negar a permissão de localização, o app deve entrar em Modo Degradado: lista de pontos navegável por cidade digitada manualmente, sem geofences automáticas.

**RF-11** No Modo Degradado, deve existir um banner persistente suave no topo da tela principal com botão "Ativar localização" que abre as configurações do dispositivo diretamente.

**RF-12** O usuário deve poder configurar a preferência de download de áudio durante o onboarding:
- Modo Econômico: download sob demanda ao abrir a notificação
- Modo Offline: pré-download dos 20 pontos ativos

**RF-13** O onboarding não deve ser exibido em sessões subsequentes. Flag `onboarding_concluido` salva localmente.

### Requisitos Não Funcionais

**RNF-03** As 3 telas de onboarding devem carregar sem requisição de rede — conteúdo estático embutido no app.

---

## E3 — Geofencing & Notificações

### Requisitos Funcionais

**RF-14** O módulo `GeofenceOrchestrator` deve ser o único componente autorizado a criar, modificar e remover geofences do S.O. Nenhuma outra camada do app acessa diretamente as APIs nativas de geofencing.

**RF-15** Ao inicializar para uma posição, o sistema deve buscar os 25 pontos mais próximos não consumidos (20 ativos + 5 substitutos offline) e registrar 20 geofences no S.O.

**RF-16** O sistema deve criar uma macro-cerca adicional com raio de 1km ao redor da posição inicial do usuário. Ao cruzar esse perímetro, o app deve acordar em background, destruir as geofences anteriores e recalcular os Top 20 com base na nova posição.

**RF-17** Uma notificação push deve ser disparada somente após o usuário permanecer dentro do raio de um ponto por 30 segundos contínuos (dwell time). Se o usuário sair do raio antes dos 30s, o timer deve ser cancelado e nenhuma notificação disparada.

**RF-18** A notificação push deve conter: título do ponto turístico e subtítulo curto descritivo.

**RF-19** No iOS, o limite de 20 regiões monitoradas deve ser respeitado: 19 pontos turísticos + 1 macro-cerca.

**RF-20** Ao marcar um ponto como "Já consumi este ponto", o `GeofenceOrchestrator` deve:
1. Remover a geofence do ponto do S.O.
2. Marcar como consumido no banco local (SQLDelight) com `pendente_sync = true`
3. Inserir o próximo ponto do cache de substitutos no slot vago
4. Sincronizar com o Supabase em background quando houver conexão

**RF-21** O raio de gatilho de cada ponto deve ser definido pelo Agente de IA baseado na categoria, com possibilidade de override manual pelo administrador:
- Museu / Monumento: 50m
- Igreja / Catedral: 75m
- Centro Histórico: 150m
- Parque / Complexo: 200m
- Praia: 300m

**RF-22** A distância para exibição na lista de pontos deve usar projeção Web Mercator (SRID 3857) para performance. A validação do raio de gatilho exato deve usar o tipo `geography` do PostGIS para precisão geodésica.

### Requisitos Não Funcionais

**RNF-04** A query de busca dos Top 25 pontos próximos deve executar em menos de 50ms para bancos com até 100.000 pontos, utilizando índice GIST espacial.

**RNF-05** O módulo de geofencing deve funcionar com app em background e com tela bloqueada em ambas as plataformas.

**RNF-06** O consumo de bateria pelo monitoramento de geofences deve ser mínimo — usando fusão de sensores do S.O. (WiFi + celular + GPS), não polling contínuo de GPS.

---

## E4 — Conteúdo de Pontos Turísticos

### Requisitos Funcionais

**RF-23** Cada ponto turístico deve ter duas versões de áudio disponíveis:
- Versão Rápida: 100-130 palavras, ~45 segundos, tom objetivo
- Versão Completa: 400-500 palavras, ~3 minutos, tom narrativo de storytelling

**RF-24** O usuário deve poder configurar sua versão padrão de áudio nas preferências do app. O padrão inicial é Versão Rápida. A configuração é acessível a todos os usuários (gratuitos e premium).

**RF-25** Dentro do card de qualquer ponto, deve existir um botão "Ouvir versão [Completa/Rápida]" que baixa e reproduz a versão alternativa sob demanda.

**RF-26** Ao abrir o card de um ponto via notificação, o app deve:
1. Exibir imediatamente: título, imagem e texto (dados leves, já em cache)
2. Iniciar download do MP3 em background
3. Reproduzir o áudio assim que o download estiver completo

**RF-27** O card de um ponto deve exibir: título, imagem, horário de funcionamento, custo de ingresso, categorias e player de áudio.

**RF-28** O sistema deve suportar múltiplos idiomas por ponto (um registro em `conteudo_midia` por idioma). O app deve selecionar automaticamente o conteúdo no idioma configurado no dispositivo, com fallback para en-US.

### Requisitos Não Funcionais

**RNF-07** Imagens dos pontos devem ser servidas via CDN com URLs estáveis. Não devem ser buscadas em tempo real durante a navegação.

**RNF-08** Arquivos de áudio devem estar em formato MP3, bitrate 128kbps, para equilibrar qualidade e tamanho de arquivo.

---

## E5 — Monetização & Paywall

### Requisitos Funcionais

**RF-29** Usuário sem nenhuma compra (tier gratuito) deve ver:
- 10 primeiros pontos desbloqueados
- Pontos 11-20 bloqueados na UI (ícone de cadeado / borrado)
- Banner AdMob fixo no rodapé
- Cards nativos de anúncio na lista (identificados como "Patrocinado")
- Sem intersticiais

**RF-30** Ao tentar acessar um ponto bloqueado, deve aparecer um modal de paywall com opção de compra da cidade atual.

**RF-31** O sistema deve suportar pagamento por cidade individualmente (PIX + cartão). O valor deve ser configurável por cidade no painel admin.

**RF-32** Na segunda compra por cidade, o modal de paywall deve apresentar o passe vitalício como opção com desconto proporcional ao que o usuário já pagou.

**RF-33** Usuário com qualquer compra prévia (ao menos uma cidade paga) deve ver em cidades novas:
- 10 pontos gratuitos desbloqueados
- Zero anúncios
- Banner sutil no topo: "Desbloqueie [Cidade] ou acesse todas as cidades"
- Sem intersticiais

**RF-34** Usuário com passe vitalício deve ter:
- Acesso irrestrito a todos os pontos em todas as cidades
- Zero anúncios permanentemente
- Badge visual "Explorador" no perfil

**RF-35** O sistema deve registrar cada compra na tabela `compras_usuario` com: tipo, cidade (se aplicável), valor pago, método de pagamento e status.

**RF-36** A lógica de acesso deve ser verificada server-side via RLS do Supabase — nunca apenas client-side.

### Requisitos Não Funcionais

**RNF-09** O processamento de pagamentos deve seguir as diretrizes de cada plataforma: In-App Purchase para iOS (Apple) e Google Play Billing para Android, além de PIX via gateway externo para pagamentos web.

**RNF-10** Em caso de falha no pagamento, o usuário deve receber mensagem clara e o acesso não deve ser concedido.

---

## E6 — Cache & Modo Offline

### Requisitos Funcionais

**RF-37** O app deve manter um banco local SQLDelight com os dados dos 25 pontos ativos (20 + 5 substitutos), incluindo metadados completos mas sem arquivos de áudio por padrão.

**RF-38** No Modo Offline, o app deve pré-baixar os arquivos MP3 da versão padrão configurada pelo usuário para os 20 pontos ativos.

**RF-39** O cache dos Top 20 deve ser invalidado por distância: quando o usuário percorre mais de 1km da posição do último cálculo (detectado pela macro-cerca).

**RF-40** O sistema deve implementar invalidação por hash de versão: o app envia a lista de `{ponto_id, content_hash}` dos pontos em cache; o servidor retorna apenas os IDs com hash divergente; o app baixa apenas os dados atualizados.

**RF-41** O campo `content_hash` em `ponto_turistico` deve ser recalculado pelo servidor sempre que uma auditoria alterar dados de contexto ou conteúdo de um ponto.

**RF-42** Operações realizadas offline (marcar consumido) devem ser sincronizadas automaticamente com o Supabase quando a conexão for restabelecida.

**RF-43** Ao recalcular os Top 20 por deslocamento, os arquivos MP3 dos pontos removidos da lista ativa devem ser deletados do storage local para liberar espaço.

### Requisitos Não Funcionais

**RNF-11** O app deve funcionar completamente offline para os 20 pontos ativos em cache, incluindo reprodução de áudio, visualização de texto e marcação de consumo.

**RNF-12** O tamanho máximo de cache esperado por sessão (20 pontos, MP3 versão rápida): aproximadamente 20 × 1MB = ~20MB. Versão completa: ~20 × 5MB = ~100MB. O app deve informar o usuário sobre o consumo de armazenamento no Modo Offline.

---

## E7 — Crowdsourcing & Reputação

### Requisitos Funcionais

**RF-44** O card de cada ponto deve conter um botão "Reportar dado incorreto".

**RF-45** O fluxo de reporte deve exibir uma lista de categorias predefinidas com texto opcional:
- Local fechado permanentemente (severidade: crítica, threshold: 1)
- Horário errado (severidade: alta, threshold: 3)
- Preço desatualizado (severidade: alta, threshold: 3)
- Foto incorreta (severidade: baixa, threshold: 3)
- Outro (severidade: média, threshold: 3)

**RF-46** Ao atingir o threshold de uma categoria, o sistema deve disparar automaticamente o pipeline de Auditoria Prioritária para aquele ponto.

**RF-47** O usuário que reportou deve receber uma notificação push apenas quando a auditoria confirmar e aplicar uma correção real. Se não houver divergência, nenhuma notificação é enviada.

**RF-48** Cada reporte válido (que resultou em correção) deve incrementar os pontos de reputação do usuário e verificar desbloqueio de badges:
- "Explorador Atento": 1 correção válida
- "Guardião da Cidade": 5 correções válidas
- "Curador Oficial": 20 correções válidas

**RF-49** O app deve exibir um botão "Sugerir lugar" no mapa para usuários autenticados.

**RF-50** Ao sugerir um lugar, o app deve normalizar as coordenadas via Google Places API e obter o `place_id` canônico antes de registrar na tabela `sugestoes_usuario`.

**RF-51** Se o `place_id` sugerido já existir em `ponto_turistico`, o usuário deve ser notificado que o ponto já está no app.

**RF-52** Ao atingir 10 votos distintos para a mesma sugestão, o sistema deve disparar automaticamente o pipeline de Moderação de Sugestões.

---

## E8 — Agente de IA (Server-Side)

### Requisitos Funcionais

**RF-53** O Agente de IA deve operar exclusivamente de forma assíncrona via Edge Functions e Cron Jobs. Não deve haver processamento de IA em tempo real durante a navegação do usuário.

**RF-54** O pipeline de Batch Ingestion deve:
1. Receber lista de cidades e categorias do administrador
2. Buscar pontos via Google Places API (filtro: rating ≥ 4.0, reviews ≥ 100)
3. Gerar roteiros via LLM (versão rápida + versão completa, por idioma)
4. Gerar arquivos MP3 via TTSProvider configurado
5. Fazer upload dos MP3s para Supabase Storage
6. Persistir todos os dados no banco

**RF-55** O pipeline de geração de áudio deve usar uma interface abstrata `TTSProvider` com os métodos `generateAudio()`, `getVoices()` e `estimateCost()`. O provider concreto (Google TTS, Amazon Polly ou ElevenLabs) deve ser configurável via variável de ambiente sem alteração de código.

**RF-56** O pipeline de Auditoria Diária deve executar via `pg_cron` às 02:00 UTC, processar em lotes de 100 os pontos com `data_ultima_verificacao` há mais de 30 dias, comparar dados com a Google Places API e corrigir divergências automaticamente.

**RF-57** O pipeline de Auditoria Prioritária deve ser disparado por evento quando o threshold de reportes for atingido. Deve re-verificar os dados, corrigir se necessário, zerar o `contador_alertas` e notificar os usuários que reportaram (quando houver correção real).

**RF-58** O pipeline de Moderação de Sugestões deve executar a cada 30 minutos via `pg_cron`, processar sugestões com 10+ votos e avaliar via LLM se o ponto tem relevância turística, usando os critérios: rating ≥ 3.8, reviews ≥ 50, categoria turística relevante, não é estabelecimento comercial comum.

**RF-59** O raio de gatilho sugerido pelo agente deve ser definido automaticamente pela categoria do ponto, conforme tabela de raios padrão (RF-21), e salvo no campo `raio_gatilho_metros`.

**RF-60** Pontos com falha no pipeline de Batch Ingestion devem ser re-tentados automaticamente até 3 vezes com backoff exponencial (1s, 5s, 15s). Falhas fatais devem notificar o administrador.

### Requisitos Não Funcionais

**RNF-13** O custo operacional recorrente do Agente de IA (auditoria diária + moderação) não deve exceder $30/mês para uma base de até 1.000 pontos turísticos.

**RNF-14** Os prompts LLM devem garantir saída sem formatação (sem asteriscos, hífens, colchetes), sem emojis e com texto otimizado para síntese de voz.

**RNF-15** O pipeline de Batch Ingestion deve respeitar rate limiting das APIs externas: máximo 1 requisição/segundo para Google Places API.

---

## E9 — Internacionalização

### Requisitos Funcionais

**RF-61** A interface do app (menus, botões, labels, mensagens de erro) deve ser traduzida via sistema i18n. A língua ativa deve seguir o idioma do dispositivo automaticamente.

**RF-62** O fallback de idioma da interface deve ser en-US para qualquer idioma não suportado.

**RF-63** O conteúdo dos pontos turísticos (roteiros e áudios) deve ser gerado e armazenado por idioma independentemente, sem acoplamento entre idiomas.

**RF-64** Adicionar suporte a um novo idioma deve requerer apenas: rodar o pipeline de Batch Ingestion com o novo parâmetro de idioma + adicionar arquivo de strings de UI — sem alteração de schema ou código de lógica.

### Requisitos Não Funcionais

**RNF-16** Datas, moedas e formatos numéricos devem seguir o locale do dispositivo.

---

## E10 — Painel Administrativo

### Requisitos Funcionais

**RF-65** O painel admin deve permitir ao administrador:
- Criar/editar/desativar pontos turísticos manualmente
- Definir ou sobrescrever o `raio_gatilho_metros` de cada ponto
- Iniciar o pipeline de Batch Ingestion para uma nova cidade
- Visualizar e resolver sugestões rejeitadas pela IA (override manual)
- Visualizar logs de jobs com status (sucesso / falha / retry)
- Configurar o provider de TTS ativo

**RF-66** O painel admin deve ser protegido por autenticação separada (role `admin` no Supabase Auth) — não acessível por usuários comuns.

**RF-67** Operações críticas no painel (deletar ponto, rejeitar sugestão) devem exigir confirmação explícita.

### Requisitos Não Funcionais

**RNF-17** O painel admin pode ser uma interface web simples (Supabase Dashboard + scripts SQL) no MVP, evoluindo para painel dedicado na Fase 2.

---

## Resumo de Cobertura

| Épico | RFs | RNFs |
|-------|-----|------|
| E1 — Auth & Perfil | RF-01 a RF-06 | RNF-01, RNF-02 |
| E2 — Onboarding | RF-07 a RF-13 | RNF-03 |
| E3 — Geofencing | RF-14 a RF-22 | RNF-04 a RNF-06 |
| E4 — Conteúdo | RF-23 a RF-28 | RNF-07, RNF-08 |
| E5 — Monetização | RF-29 a RF-36 | RNF-09, RNF-10 |
| E6 — Cache & Offline | RF-37 a RF-43 | RNF-11, RNF-12 |
| E7 — Crowdsourcing | RF-44 a RF-52 | — |
| E8 — Agente de IA | RF-53 a RF-60 | RNF-13 a RNF-15 |
| E9 — i18n | RF-61 a RF-64 | RNF-16 |
| E10 — Admin | RF-65 a RF-67 | RNF-17 |
| **Total** | **67 RFs** | **17 RNFs** |
