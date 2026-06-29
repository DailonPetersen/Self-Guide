# Especificação do Agente de IA — Tour Guiado Automatizado

Este documento descreve todos os pipelines, fluxos e interfaces do Agente de IA que opera no servidor de forma assíncrona.

---

## Visão Geral

O Agente de IA opera exclusivamente via tarefas agendadas (Cron Jobs) e gatilhos por eventos. Nunca processa requisições em tempo real durante a jornada do usuário — isso elimina latência na rua e mantém custo de tokens previsível.

**Pipelines existentes:**
1. Batch Ingestion — carga inicial de conteúdo por cidade
2. Auditoria Diária — verificação periódica de dados de contexto
3. Auditoria Prioritária — gatilhada por reportes de usuários
4. Moderação de Sugestões — avaliação de novos pontos sugeridos por crowdsourcing
5. Geração de Conteúdo Multi-idioma — expansão de idiomas existentes

---

## Interface TTS (Abstração de Provider)

Todos os pipelines de geração de áudio usam esta interface genérica. O provider é configurável por variável de ambiente sem alteração de código.

```typescript
interface TTSProvider {
  generateAudio(params: {
    text: string;
    voice: string;
    style: 'rapida' | 'completa';
    language: string;
  }): Promise<{ audioBuffer: Buffer; durationSeconds: number }>;

  getVoices(language: string): Promise<Voice[]>;

  estimateCost(text: string): Promise<{ characters: number; estimatedUSD: number }>;
}

// Providers implementados
class GoogleTTSProvider implements TTSProvider { ... }
class AmazonPollyProvider implements TTSProvider { ... }
class ElevenLabsProvider implements TTSProvider { ... }

// Seleção via env
const provider = TTSProviderFactory.create(process.env.TTS_PROVIDER); // 'google' | 'polly' | 'elevenlabs'
```

**Vozes recomendadas por idioma (MVP com Google TTS):**

| Idioma | Voz | Modelo |
|--------|-----|--------|
| pt-BR | pt-BR-Neural2-B | Neural2 |
| en-US | en-US-Neural2-D | Neural2 |
| es-ES | es-ES-Neural2-B | Neural2 |

---

## Pipeline 1: Batch Ingestion

**Gatilho:** Administrador insere lista de cidades/regiões no painel admin.

**Input esperado:**
```json
{
  "cidades": ["São Paulo, SP", "Rio de Janeiro, RJ"],
  "idiomas": ["pt-BR"],
  "categorias": {
    "Museu": { "limite": 10, "raio_padrao": 50 },
    "Parque": { "limite": 5, "raio_padrao": 200 },
    "Centro Historico": { "limite": 8, "raio_padrao": 150 },
    "Igreja": { "limite": 5, "raio_padrao": 75 },
    "Praia": { "limite": 5, "raio_padrao": 300 }
  }
}
```

**Fluxo detalhado:**

```
PARA CADA cidade:
  1. EXTRAÇÃO via Google Places API
     - Busca por categoria e cidade
     - Filtra: rating >= 4.0, reviews >= 100
     - Extrai: place_id, nome, coordenadas, horários, preço, imagem_url
     - Normaliza raio via tabela de categorias (com override se definido)

  2. DEDUPLICAÇÃO
     - Verifica se place_id já existe em ponto_turistico
     - Se existir: pula (não sobrescreve)
     - Se não existir: insere com status = 'ativo'

  3. GERAÇÃO DE ROTEIRO (via LLM — Claude claude-sonnet-4-6)
     PARA CADA ponto PARA CADA idioma:
       a. Prompt Versão Rápida (~45s):
          "Você é um guia turístico. Escreva um roteiro de áudio de 100-130 palavras
           sobre [nome do ponto], tom objetivo e direto, sem formatação, sem emojis,
           sem caracteres especiais. Idioma: [idioma]. Dados: [dados do Places API]"

       b. Prompt Versão Completa (~3min):
          "Você é um guia turístico experiente. Escreva um roteiro de áudio de 400-500
           palavras sobre [nome do ponto], tom narrativo de storytelling, como se fosse
           um documentário. Comece com um fato curioso ou histórico surpreendente.
           Sem formatação, sem emojis, sem caracteres especiais. Idioma: [idioma].
           Dados: [dados do Places API]"

  4. GERAÇÃO DE ÁUDIO (via TTSProvider)
     PARA CADA roteiro:
       - Envia texto ao provider configurado
       - Recebe buffer de áudio MP3
       - Upload para Supabase Storage: /audios/{ponto_id}/{idioma}/{versao}.mp3
       - Salva audio_url no banco

  5. ATUALIZAÇÃO DO BANCO
     - Insere/atualiza conteudo_midia (roteiro + audio_url por idioma)
     - Insere contexto (horários, preços, categorias, fonte_url)
     - Insere controle_erros com contador = 0
     - Calcula e salva content_hash em ponto_turistico

  6. LOG
     - Registra sucesso/falha por ponto em tabela de log de jobs
     - Pontos com falha entram em fila de retry automático (máx 3 tentativas)
```

**Estimativa de custo por ponto (MVP com Google TTS):**
- LLM (2 roteiros × 2 versões × N idiomas): ~$0.01-0.03 por ponto
- TTS (versão rápida ~600 chars + completa ~2500 chars por idioma): ~$0.02 por ponto/idioma
- Total estimado por ponto em pt-BR: ~$0.05

---

## Pipeline 2: Auditoria Diária

**Gatilho:** Cron Job às 02:00 UTC diariamente.

**Critério de seleção:**
```sql
SELECT ponto_id FROM contexto
WHERE data_ultima_verificacao < NOW() - INTERVAL '30 days'
ORDER BY data_ultima_verificacao ASC
LIMIT 100; -- Processa em lotes de 100
```

**Fluxo:**
```
PARA CADA ponto selecionado:
  1. Busca dados atuais via Google Places API (usando place_id)
  2. Compara com dados salvos no banco:
     - horarios_funcionamento
     - custo_ingresso
     - status operacional (aberto/fechado permanentemente)
  3. SE divergência encontrada:
     - Atualiza tabela contexto
     - Recalcula content_hash em ponto_turistico
     - Atualiza data_ultima_verificacao
  4. SE sem divergência:
     - Apenas atualiza data_ultima_verificacao
  5. Rate limiting: 1 requisição/segundo para não exceder quota da Places API
```

---

## Pipeline 3: Auditoria Prioritária

**Gatilho:** Evento no banco quando `controle_erros.contador_alertas` atinge threshold por categoria.

**Thresholds:**
```typescript
const THRESHOLDS = {
  'local_fechado': 1,      // Auditoria imediata
  'horario_errado': 3,
  'preco_desatualizado': 3,
  'foto_incorreta': 3,
  'outro': 3
};
```

**Fluxo:**
```
1. Recebe ponto_id + lista de reportes da categoria
2. Busca dados atuais via Google Places API
3. Compara com dados salvos

4. SE divergência confirmada:
   - Corrige dados no banco
   - Atualiza content_hash
   - Zera contador_alertas
   - Marca reportes como status = 'corrigido', correcao_aplicada = TRUE
   - Incrementa pontos_reputacao dos usuários que reportaram
   - Envia notificação push aos usuários com correcao_aplicada = TRUE:
     "Obrigado! As informações de [nome do ponto] foram atualizadas."

5. SE sem divergência:
   - Zera contador_alertas (falso positivo ou dado já corrigido)
   - Marca reportes como status = 'sem_divergencia'
   - Nenhuma notificação enviada aos usuários

6. Log de auditoria com resultado
```

---

## Pipeline 4: Moderação de Sugestões

**Gatilho:** Cron Job a cada 30 minutos verificando sugestões com `contador_votos >= 10`.

**Fluxo:**
```
1. Busca sugestões com contador_votos >= 10 e status = 'aguardando_votos'
2. PARA CADA sugestão:

   a. VALIDAÇÃO via Google Places API (usando place_id):
      - Verifica se local ainda existe e está operacional
      - Confirma coordenadas e nome oficial
      - Obtém rating e número de reviews

   b. VERIFICAÇÃO DE DUPLICATA no banco:
      - SELECT * FROM ponto_turistico WHERE place_id = :place_id
      - Se já existir: marca sugestão como 'rejeitado', motivo = 'duplicata'

   c. AVALIAÇÃO DE RELEVÂNCIA TURÍSTICA (via LLM):
      Prompt: "O seguinte local é turisticamente relevante para um guia de turismo?
      Responda apenas com JSON: { 'aprovado': boolean, 'motivo': string }
      Local: [dados do Places API]
      Critérios: rating >= 3.8, reviews >= 50, categorias turísticas relevantes"

   d. SE aprovado pela IA:
      - Insere em ponto_turistico com status = 'pendente_moderacao'
      - Atualiza sugestao.status = 'aprovado'
      - Entra na fila do Batch Ingestion para geração de conteúdo

   e. SE rejeitado pela IA:
      - Atualiza sugestao.status = 'rejeitado', salva motivo_rejeicao
      - Notifica administrador via webhook/email com dados da sugestão e motivo
      - Administrador pode sobrescrever a decisão manualmente
```

---

## Pipeline 5: Geração de Conteúdo Multi-idioma

**Gatilho:** Manual pelo administrador ao habilitar novo idioma.

**Fluxo:**
```
1. Recebe: idioma_novo (ex: 'fr-FR')
2. Busca todos os pontos em ponto_turistico com status = 'ativo'
   que NÃO possuem registro em conteudo_midia para esse idioma
3. PARA CADA ponto (em lotes de 50):
   - Gera roteiro_rapido no novo idioma
   - Gera roteiro_completo no novo idioma
   - Gera áudios via TTSProvider no novo idioma
   - Insere novo registro em conteudo_midia
4. Progresso salvo em tabela de jobs para retomada em caso de falha
```

---

## Prompts de Sistema do LLM

### Prompt Base — Geração de Roteiro Rápido

```
Você é um guia turístico profissional especializado em criar roteiros de áudio envolventes.

TAREFA: Escreva um roteiro de áudio curto sobre o ponto turístico fornecido.

REGRAS OBRIGATÓRIAS:
- Entre 100 e 130 palavras exatamente
- Tom: objetivo, informativo, levemente entusiasmado
- Sem formatação (sem asteriscos, hífens, colchetes, parênteses)
- Sem emojis
- Sem caracteres especiais ou símbolos
- Sem referências a preços específicos (podem mudar)
- Texto deve soar natural quando lido em voz alta
- Termine com uma frase de transição (ex: "Uma visita que vale cada passo.")
- Idioma de saída: [IDIOMA]

DADOS DO LOCAL:
Nome: [NOME]
Descrição: [DESCRICAO_PLACES]
Categoria: [CATEGORIA]
Localização: [CIDADE], [ESTADO]

Retorne APENAS o texto do roteiro, sem explicações adicionais.
```

### Prompt Base — Geração de Roteiro Completo

```
Você é um narrador de documentários turísticos com profundo conhecimento histórico.

TAREFA: Escreva um roteiro de áudio completo e imersivo sobre o ponto turístico fornecido.

REGRAS OBRIGATÓRIAS:
- Entre 400 e 500 palavras exatamente
- Tom: narrativo, evocativo, estilo storytelling de documentário
- Comece com um fato histórico surpreendente ou curiosidade pouco conhecida
- Inclua contexto histórico, cultural e arquitetônico relevante
- Sem formatação (sem asteriscos, hífens, colchetes, parênteses)
- Sem emojis
- Sem caracteres especiais ou símbolos
- Texto deve soar natural quando lido em voz alta por 2-3 minutos
- Termine com uma reflexão sobre o significado do local para a cidade
- Idioma de saída: [IDIOMA]

DADOS DO LOCAL:
Nome: [NOME]
Descrição: [DESCRICAO_PLACES]
Categoria: [CATEGORIA]
Localização: [CIDADE], [ESTADO]
Histórico adicional disponível: [HISTORICO_WIKIPEDIA_SE_DISPONIVEL]

Retorne APENAS o texto do roteiro, sem explicações adicionais.
```

### Prompt Base — Avaliação de Relevância Turística

```
Avalie se o seguinte local é turisticamente relevante para inclusão em um guia de turismo de áudio.

CRITÉRIOS DE APROVAÇÃO (todos devem ser verdadeiros):
- Rating no Google >= 3.8
- Número de reviews >= 50
- Categoria é turisticamente relevante (museu, monumento, parque, praia, centro histórico, igreja histórica, teatro, galeria de arte, mercado histórico, mirador/mirante)
- Não é um estabelecimento comercial comum (restaurante, loja, hotel, posto de gasolina)
- Tem interesse cultural, histórico, natural ou arquitetônico

DADOS DO LOCAL:
[DADOS_JSON_DO_PLACES_API]

Responda APENAS com JSON válido, sem explicações:
{
  "aprovado": true | false,
  "motivo": "explicação em 1 frase"
}
```

---

## Tratamento de Erros e Retry

```typescript
const RETRY_CONFIG = {
  maxAttempts: 3,
  backoffMs: [1000, 5000, 15000], // Exponencial
  retryableErrors: ['RATE_LIMIT', 'TIMEOUT', 'NETWORK_ERROR'],
  fatalErrors: ['INVALID_API_KEY', 'QUOTA_EXCEEDED', 'INVALID_PLACE_ID']
};

// Pontos com falha fatal são marcados em log_jobs com status = 'falha_fatal'
// e notificam o administrador via webhook
```

---

## Estimativa de Custos Mensais (MVP)

Assumindo 500 pontos turísticos, 3 idiomas, auditoria de 100 pontos/dia:

| Item | Quantidade | Custo Estimado/mês |
|------|-----------|-------------------|
| Batch Ingestion inicial (1x) | 500 pontos × 3 idiomas | ~$75 |
| Auditoria diária (Places API) | 3.000 requests/mês | ~$15 |
| Auditoria prioritária (LLM) | ~50 eventos/mês | ~$2 |
| Moderação de sugestões (LLM) | ~20 sugestões/mês | ~$1 |
| Supabase Edge Functions | ~10.000 invocações/mês | Free tier |
| **Total recorrente** | | **~$18/mês** |
