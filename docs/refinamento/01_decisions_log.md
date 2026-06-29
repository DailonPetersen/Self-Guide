# Decisions Log — Tour Guiado Automatizado
> Registro completo das 21 decisões tomadas no processo de refinamento do sistema.

---

## Tópico 1 — Geofencing & Comportamento Mobile

**Decisão 1 — Comportamento ao cruzar geofence com tela bloqueada**
- Escolha: Notificação push rica (título + prévia do ponto)
- O usuário escolhe nas preferências entre Modo Econômico (download sob demanda ao abrir) e Modo Offline (pré-download antecipado)
- Essa preferência deve ser apresentada explicitamente no onboarding, não apenas nas configurações

**Decisão 2 — Proteção contra triggers acidentais (passagem de carro)**
- Escolha: Dwell time mínimo de 30 segundos dentro do raio antes de disparar a notificação
- Implementado via timer cancelável nas APIs nativas (CoreLocation / Geofencing API Android)

**Decisão 3 — Comportamento offline ao marcar ponto como consumido**
- Escolha: Pré-carregar os próximos 5 pontos além dos 20 ativos em cache local (só metadados + coordenadas, sem MP3)
- Ao marcar consumido offline: remove geofence do S.O., marca como consumido no SQLDelight local, insere próximo ponto do cache de 5
- Sincronização com servidor ocorre em background quando conexão voltar
- Toda essa lógica fica isolada no módulo `GeofenceOrchestrator` — nenhuma outra camada toca diretamente nas APIs nativas de geofencing

---

## Tópico 2 — Modelo de Paywall & Monetização

**Decisão 4 — Escopo do pagamento**
- Escolha: Híbrido — pagamento por cidade individualmente + upsell vitalício na segunda compra
- Primeira cidade: preço baixo para reduzir fricção de conversão
- Segunda cidade: apresentar passe vitalício como "mais vantajoso" com desconto

**Decisão 5 — Posicionamento dos anúncios na camada gratuita**
- Escolha: Banner fixo no rodapé + anúncio nativo integrado na lista de pontos
- Sem intersticiais — contexto turístico (usuário em movimento) exige experiência não-bloqueante

**Decisão 6 — Experiência de usuário que já pagou ao visitar nova cidade**
- Escolha: 10 pontos reais gratuitos, zero anúncios, apenas banner sutil no topo incentivando o upsell vitalício
- Respeitar o histórico de pagamento é prioridade — sem ads para quem já converteu

---

## Tópico 3 — Agente de IA & Pipeline de Conteúdo

**Decisão 7 — Tom e duração do áudio gerado**
- Escolha: Duas versões de áudio por ponto — Rápida (~45s) e Completa (~3min)
- Ambas geradas no Batch Ingestion (custo marginal baixo de TTS)
- Usuário configura versão padrão nas preferências do app (acessível a todos — gratuitos e premium)
- Dentro do card de um ponto, botão para solicitar a versão alternativa (download sob demanda)

**Decisão 8 — Critério de seleção de pontos por cidade**
- Fase 1 (MVP): Lista manual definida pelo administrador — o agente apenas enriquece com conteúdo
- Fase 2: Categorias configuráveis por cidade com filtro por preferência do usuário
- Fase 3: Usuários sugerem novos pontos via contribuição crowdsourced
- Campo `status` na tabela `ponto_turistico` deve existir desde o MVP: `['ativo', 'pendente_moderacao', 'rejeitado']`

**Decisão 9 — Fluxo de moderação de pontos sugeridos por usuários**
- Gatilho: 10 usuários distintos sugerem o mesmo ponto (deduplicado via `place_id` do Google Places)
- Moderação IA: valida existência no Google Places, checa duplicata no banco, confirma que é local real e turístico
- Se aprovado pela IA: entra no pipeline de Batch Ingestion automaticamente
- Se rejeitado pela IA: administrador recebe notificação com motivo para decisão final
- Tabela `sugestoes_usuario` com contador de votos e status de moderação deve existir desde o MVP

**Decisão 10 — Provider de TTS (Text-to-Speech)**
- Escolha: Abstração com provider configurável — interface genérica com 3 métodos: `generateAudio(text, voice, style)`, `getVoices()`, `estimateCost(text)`
- MVP: Google Cloud TTS ou Amazon Polly (melhor custo)
- Futuro: migração para ElevenLabs (melhor qualidade de storytelling) sem reescrita de código

---

## Tópico 4 — Banco de Dados & Queries Espaciais

**Decisão 11 — Deduplicação de sugestões de usuários com coordenadas divergentes**
- Escolha: Normalização via `place_id` do Google Places API
- `place_id` é o identificador canônico de um local — dois usuários apontando coordenadas diferentes do mesmo museu retornam o mesmo `place_id`
- Elimina necessidade de lógica fuzzy de distância ou similaridade de nome

**Decisão 12 — Definição do raio de gatilho por ponto**
- Escolha: Raio sugerido automaticamente pelo agente baseado na categoria do ponto, com override manual pelo administrador
- Raios padrão por categoria: Museu 50m, Parque 200m, Praia 300m, Centro Histórico 150m
- Administrador pode sobrescrever qualquer valor individualmente

**Decisão 13 — Projeção geográfica para cálculo de distância**
- Escolha: Híbrido
- Query dos Top 20: Web Mercator (SRID 3857) para aproveitar índice GIST e garantir performance
- Validação do raio de gatilho exato: `geography` (tipo esférico nativo do PostGIS) para precisão geodésica sem distorção

---

## Tópico 5 — Cache & Modo Offline

**Decisão 14 — Versões de áudio no Modo Offline**
- Escolha: Usuário configura uma única versão padrão (Rápida ou Completa)
- Modo Offline baixa apenas a versão padrão para os 20 pontos ativos
- Dentro do card de qualquer ponto, botão para baixar e ouvir a versão alternativa sob demanda
- Mesma lógica se aplica no Modo Econômico

**Decisão 15 — Invalidação de cache quando auditoria atualiza um ponto**
- Escolha: Hash de versão por ponto
- Campo `content_hash` adicionado à tabela `ponto_turistico` desde o MVP
- App compara hashes locais com os do servidor em background e baixa apenas os diffs necessários
- Não depende de conexão Realtime permanente (preserva bateria)

---

## Tópico 6 — Auditoria & Crowdsourcing de Erros

**Decisão 16 — Interface de reporte de dado incorreto**
- Escolha: Lista de categorias predefinidas + campo de texto opcional para detalhes
- Categorias definem severidade da auditoria:
  - "Local fechado permanentemente" → auditoria imediata com 1 único reporte
  - "Horário errado" / "Preço desatualizado" → threshold de 3 alertas
  - "Foto incorreta" → threshold de 3 alertas, menor prioridade

**Decisão 17 — Feedback ao usuário após auditoria**
- Escolha: Notificação push + sistema de reputação com badges por reportes válidos
- Notificação enviada apenas quando a auditoria confirma e aplica uma correção real
- Se o agente não encontrar divergência, o usuário não recebe nenhuma notificação (silêncio = sem ruído)

---

## Tópico 7 — Internacionalização & Multi-idioma

**Decisão 18 — Idiomas suportados no MVP**
- Escolha: pt-BR no MVP com arquitetura preparada para múltiplos idiomas sem reescrita
- Pipeline do agente gera conteúdo por idioma via configuração
- Adicionar novos idiomas = rodar batch para pontos existentes no novo idioma

**Decisão 19 — Tradução da interface do app**
- Escolha: Interface segue o idioma do dispositivo automaticamente
- Preparada para expansão de idiomas via arquivos de localização (i18n)
- Fallback para en-US se idioma do dispositivo não for suportado

---

## Tópico 8 — Onboarding & UX de Primeira Vez

**Decisão 20 — Fluxo de primeiro acesso**
- Escolha: 3 telas de onboarding comunicando o valor do produto (geofences automáticas, áudio histórico, guia de bolso)
- Após as telas: usuário escolhe entre usar GPS automaticamente ou digitar cidade manualmente
- Opção manual permite planejar a visita antes de chegar no destino

**Decisão 21 — Comportamento quando usuário nega permissão de localização**
- Escolha: Modo degradado — usuário navega pela lista de pontos digitando cidade/bairro manualmente, sem geofences automáticas
- Banner persistente suave incentivando ativar localização para experiência completa
- Não bloqueia o app — usuário pode testar o conteúdo e converter depois
