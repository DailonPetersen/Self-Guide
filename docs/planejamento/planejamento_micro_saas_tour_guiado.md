# Blueprint de Arquitetura e Planejamento: Micro-SaaS Tour Guiado Automatizado

Este documento serve como a especificação técnica e funcional definitiva para o desenvolvimento do Micro-SaaS de turismo baseado em geolocalização e curadoria automatizada por Inteligência Artificial. O projeto foi desenhado sob a premissa de **baixa manutenção**, **alta escalabilidade** e **custo operacional previsível**.

---

## 1. Visão Geral do Modelo de Negócio

O sistema consiste em um aplicativo móvel nativo/híbrido que atua como um guia turístico de bolso baseado em áudio. À medida que o usuário se desloca por uma cidade, cercas virtuais geográficas (*geofences*) disparam notificações push contextuais contendo a história, curiosidades, horários e custos daquele ponto de interesse específico.

### Estratégia de Monetização Híbrida (Freemium + Paywall)
* **Camada Gratuita (Ad-Supported):** O usuário tem acesso irrestrito aos dados e áudios dos **10 pontos turísticos mais próximos** da sua localização inicial atual. Banners ou anúncios nativos (ex: Google AdMob) são exibidos durante a navegação.
* **Camada Premium (Paywall Automatizado):** Os pontos do 11º ao 20º da região aparecem na interface de forma bloqueada (borrados ou com ícone de cadeado). Para desbloquear o acesso total à cidade e remover permanentemente os anúncios, o usuário realiza um pagamento único via microtransação (PIX integrado).

---

## 2. Modelagem do Banco de Dados Relacional

A estrutura de dados foi projetada para otimizar buscas por coordenadas geográficas e suportar uma arquitetura desacoplada (onde o aplicativo consome apenas dados prontos e estáticos).

### Tabela: `ponto_turistico`
Armazena a entidade geográfica principal que o sistema operacional utilizará para o cercamento virtual.
* `id` (UUID, Primary Key)
* `latitude` (Decimal(10, 8), Not Null)
* `longitude` (Decimal(11, 8), Not Null)
* `raio_gatilho_metros` (Integer, Default 50) - Distância em metros para ativação da cerca virtual.

### Tabela: `conteudo_midia`
Contém as mídias textuais e os links dos arquivos gerados pelo Agente de IA.
* `ponto_id` (UUID, Foreign Key -> `ponto_turistico.id`)
* `idioma` (Varchar(5), Default 'pt-BR')
* `titulo_local` (Varchar(255), Not Null)
* `roteiro_texto` (Text, Not Null) - Texto limpo, refinado pelo agente para adequação à síntese de voz.
* `audio_url` (Varchar(512)) - Link de CDN/Bucket público (ex: AWS S3) contendo o MP3 gerado.
* `imagem_url` (Varchar(512)) - URL dinâmica extraída de plataformas geográficas parceiras.

### Tabela: `contexto`
Agrupa informações utilitárias que passam por auditoria diária do Agente de IA.
* `ponto_id` (UUID, Foreign Key -> `ponto_turistico.id`)
* `horarios_funcionamento` (Text) - Ex: "Ter-Dom: 09h às 17h"
* `custo_ingresso` (Text) - Ex: "R$ 30,00" ou "Gratuito"
* `categorias` (Array de Varchar ou Texto) - Tags para filtros (Ex: `["Histórico", "Natureza"]`)
* `fonte_url` (Varchar(512)) - Link de origem para rastreabilidade (Google Places / TripAdvisor).
* `data_ultima_verificacao` (Timestamp)

### Tabela: `controle_erros`
Inteligência coletiva (crowdsourcing) para mitigar manutenções manuais.
* `ponto_id` (UUID, Foreign Key -> `ponto_turistico.id`)
* `contador_alertas` (Integer, Default 0) - Incrementado quando um usuário reporta dados desatualizados.

### Tabela: `historico_usuario`
Evita redundância na experiência do turista.
* `usuario_id` (UUID)
* `ponto_id` (UUID, Foreign Key -> `ponto_turistico.id`)
* `status_consumo` (Varchar(20)) - Valores: `['nao_visto', 'consumido']`

---

## 3. Dinâmica do Aplicativo Móvel (Client-Side)

Para contornar a limitação rígida dos sistemas operacionais (iOS restringe o monitoramento a no máximo **20 geofences simultâneas** por app), o ciclo de vida do aplicativo adotará a seguinte lógica:

```
[Entrada na Cidade / Seleção de Local]
                │
                ▼
Buscar no Banco: Opção do usuário ou GPS inicial
                │
                ▼
Query Filtrada: Executa busca espacial (PostGIS / Spatial Index)
- Ordena por distância (Mais próximos)
- Filtra `status_consumo != 'consumido'`
- Limita o resultado a exatamente 20 registros
                │
                ▼
Registrar no S.O.: Cria 20 Cercas Virtuais nativas
- Pontos 1 a 10: Desbloqueados por padrão
- Pontos 11 a 20: Bloqueados na UI (Gatilho de Paywall)
```

### Comportamento em Deslocamento (Macro-Cerca)
O aplicativo cria uma **21ª cerca virtual móvel** ao redor do próprio usuário, com um raio maior (ex: 1 km). 
* Se o usuário caminhar para fora desse perímetro, o aplicativo acorda em segundo plano, destrói as cercas antigas e roda a query novamente, reconfigurando os "Top 20" pontos com base nas novas coordenadas.

### Gerenciamento Progressivo de Mídia (Cache Híbrido)
1.  **Configuração de Preferência:** O usuário escolhe se deseja realizar o download prévio (Offline Mode) dos áudios das 20 cercas ativas ou se prefere o consumo sob demanda.
2.  **Fluxo Sob Demanda:** Ao cruzar o gatilho de uma cerca sem o áudio baixado, a notificação push é disparada. Ao abrir a notificação, a interface carrega instantaneamente o texto e a imagem da API geográfica enquanto faz o download do arquivo `.mp3` em segundo plano para reprodução imediata.
3.  **Marcação de Consumo:** Ao clicar no botão "Já consumi este ponto", o aplicativo remove a cerca atual do S.O., atualiza a tabela `historico_usuario` local/remota e insere o próximo ponto turístico mais próximo da cauda longa no slot vago do limite de 20 geofences.

---

## 4. Arquitetura do Agente de IA (Server-Side)

O Agente de IA opera de forma totalmente assíncrona baseada em tarefas agendadas (*Cron Jobs/Cloud Functions*), eliminando interações diretas em tempo real na jornada de navegação do usuário (o que reduz custos com tokens de API e remove latências na rua).

### Fluxo 1: Carga em Lote (Batch Ingestion)
1.  **Input:** O administrador insere uma lista de capitais/regiões a serem mapeadas.
2.  **Extração Confiável:** A IA consulta APIs estruturadas de Plataformas Geográficas (Google Places API / TripAdvisor API). Ela extrai: Coordenadas exatas, Nome Oficial, Horários Operacionais, Custo Base de Entrada e as URLs oficiais das Imagens de exibição.
3.  **Processamento de Texto:** O agente reescreve a descrição histórica coletada, eliminando caracteres especiais, marcações de formatação web e termos de difícil pronúncia, focando em um tom de "Storytelling" ideal para guias de turismo.
4.  **Pipeline de Áudio (TTS):** O texto refinado é enviado a uma API de Text-to-Speech de alta fidelidade. O arquivo final de áudio é salvo em um Storage Público de nuvem (ex: Azure Blob Storage ou AWS S3), retornando a `audio_url` estável para persistência no banco de dados.

### Fluxo 2: Auditoria Automatizada e Inteligência Coletiva
* **Gatilho por Tempo:** Uma vez a cada 24 horas, o agente analisa os registros cuja `data_ultima_verificacao` exceda o limite parametrizado.
* **Gatilho por Alerta:** Se a tabela `controle_erros` indicar que o `contador_alertas` de um ponto turístico específico atingiu um limite (ex: `>= 3` reclamações de usuários sobre horários errados ou preços alterados), o Agente de IA é acionado imediatamente em prioridade alta para aquele ID.
* **Ação:** O agente revisita a API de origem do local, compara as informações salvas com as informações atuais, corrige as divergências no banco de dados de forma autônoma e zera o `contador_alertas`.

---

## 5. Recomendação de Tech Stack

### 5.1 Stack Mobile (Kotlin Multiplatform)

**Tecnologia Principal:** Kotlin Multiplatform Mobile (KMP) com Compose Multiplatform

**Justificativa para Desenvolvedor Solo:**
- **Código Compartilhado:** ~70-80% do código (lógica de negócio, repositórios, viewmodels) compartilhado entre iOS e Android
- **Geofencing Nativo:** Acesso direto às APIs nativas de geofencing através de expect/actual, garantindo máxima performance e confiabilidade em background
- **Compose Multiplatform:** UI declarativa moderna, hot reload, e componentes compartilhados entre plataformas
- **Ecoistema Kotlin:** Coroutines nativos para operações assíncronas, Flow para streams de dados reativos
- **Manutenção Reduzida:** Uma base de código para manter em vez de duas (Swift + Kotlin nativo)

**Dependências Chave:**
```kotlin
// Mobile
- KMP Core (kotlinx.coroutines, kotlinx.serialization)
- Compose Multiplatform (Material3)
- Koin (DI compartilhado)
- SQLDelight (Banco de dados local compartilhado)
- Ktor (Client HTTP compartilhado)
- AdMob SDK (via expect/actual para cada plataforma)

// Android-specific
- Google Play Services Location (Geofencing nativo)
- WorkManager (Jobs em background)

// iOS-specific
- CoreLocation (Geofencing nativo)
- BackgroundTasks (Jobs em background)
```

### 5.2 Stack Backend (Serverless/FaaS)

**Tecnologia Principal:** Supabase (PostgreSQL + PostGIS) + Cloud Functions

**Justificativa para Desenvolvedor Solo:**
- **Supabase:** PostgreSQL gerenciado com PostGIS habilitado nativamente, API REST automática, Realtime subscriptions, Auth integrado
- **Custo Operacional Previsível:** Free tier generoso para MVP, escala linear com uso
- **Zero DevOps:** Sem gerenciamento de servidores, backups automáticos, SSL automático
- **Edge Functions:** Deno runtime para lógica serverless quando necessário
- **Storage Integrado:** S3-compatible para áudios MP3 e imagens

**Arquitetura Serverless:**
```yaml
Serviços:
  - Supabase PostgreSQL (com extensão PostGIS)
  - Supabase Storage (áudios MP3, imagens)
  - Supabase Auth (autenticação de usuários)
  - Supabase Edge Functions (lógica de negócio leve)
  - GitHub Actions (CI/CD)
  - Vercel/Netlify (hosting de landing page opcional)

Cron Jobs (via Supabase pg_cron ou GitHub Actions):
  - job_atualizar_horarios_diario: Roda 1x/dia às 02:00
  - job_auditoria_prioritaria: Roda sob demanda quando contador_alertas >= 3
```

**Alternativa Serverless Pura (se preferir):**
- AWS Lambda + RDS PostgreSQL + S3 + CloudWatch Events
- Google Cloud Functions + Cloud SQL + Cloud Storage + Cloud Scheduler
- Azure Functions + Azure SQL + Blob Storage + Azure Logic Apps

**Recomendação:** Supabase para MVP (menor curva de aprendizado, tudo em um lugar). AWS/GCP para escala enterprise (mais granularidade de controle).

---

## 6. Modelagem DDL SQL (PostGIS)

### 6.1 Extensões e Configurações Iniciais

```sql
-- Habilitar extensões espaciais
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Configurar SRID padrão (WGS84 - GPS)
SELECT SetSRID('POINT(0 0)'::geometry, 4326);
```

### 6.2 Tabela: ponto_turistico

```sql
CREATE TABLE ponto_turistico (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    geom GEOMETRY(POINT, 4326) NOT NULL,
    raio_gatilho_metros INTEGER DEFAULT 50,
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índice espacial GIST para queries de proximidade (O(log n) performance)
CREATE INDEX idx_ponto_turistico_geom ON ponto_turistico USING GIST (geom);

-- Índice para filtro de pontos ativos
CREATE INDEX idx_ponto_turistico_ativo ON ponto_turistico (ativo);

-- Trigger para atualizar data_atualizacao automaticamente
CREATE OR REPLACE FUNCTION trigger_atualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.data_atualizacao = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ponto_turistico_atualizacao
    BEFORE UPDATE ON ponto_turistico
    FOR EACH ROW
    EXECUTE FUNCTION trigger_atualizar_timestamp();
```

### 6.3 Tabela: conteudo_midia

```sql
CREATE TABLE conteudo_midia (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ponto_id UUID NOT NULL,
    idioma VARCHAR(5) DEFAULT 'pt-BR',
    titulo_local VARCHAR(255) NOT NULL,
    roteiro_texto TEXT NOT NULL,
    audio_url VARCHAR(512),
    imagem_url VARCHAR(512),
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT fk_conteudo_midia_ponto
        FOREIGN KEY (ponto_id) 
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE
);

-- Índice composto para busca rápida por ponto e idioma
CREATE INDEX idx_conteudo_midia_ponto_idioma 
    ON conteudo_midia (ponto_id, idioma);

-- Índice para busca por idioma (quando carregando múltiplos pontos)
CREATE INDEX idx_conteudo_midia_idioma ON conteudo_midia (idioma);

CREATE TRIGGER trg_conteudo_midia_atualizacao
    BEFORE UPDATE ON conteudo_midia
    FOR EACH ROW
    EXECUTE FUNCTION trigger_atualizar_timestamp();
```

### 6.4 Tabela: contexto

```sql
CREATE TABLE contexto (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ponto_id UUID NOT NULL,
    horarios_funcionamento TEXT,
    custo_ingresso TEXT,
    categorias TEXT[], -- Array de strings para tags
    fonte_url VARCHAR(512),
    data_ultima_verificacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT fk_contexto_ponto
        FOREIGN KEY (ponto_id) 
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE
);

-- Índice para auditoria diária (pontos não verificados recentemente)
CREATE INDEX idx_contexto_ultima_verificacao 
    ON contexto (data_ultima_verificacao);

-- Índice GIN para busca eficiente em arrays de categorias
CREATE INDEX idx_contexto_categorias 
    ON contexto USING GIN (categorias);

CREATE TRIGGER trg_contexto_atualizacao
    BEFORE UPDATE ON contexto
    FOR EACH ROW
    EXECUTE FUNCTION trigger_atualizar_timestamp();
```

### 6.5 Tabela: controle_erros

```sql
CREATE TABLE controle_erros (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ponto_id UUID NOT NULL,
    contador_alertas INTEGER DEFAULT 0,
    data_ultimo_alerta TIMESTAMP WITH TIME ZONE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT fk_controle_erros_ponto
        FOREIGN KEY (ponto_id) 
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE,
    
    CONSTRAINT chk_contador_alertas_nonnegative
        CHECK (contador_alertas >= 0)
);

-- Índice para auditoria prioritária (pontos com muitos alertas)
CREATE INDEX idx_controle_erros_contador 
    ON controle_erros (contador_alertas);

-- Índice composto para busca eficiente de pontos que precisam de auditoria
CREATE INDEX idx_controle_erros_ponto_contador 
    ON controle_erros (ponto_id, contador_alertas);
```

### 6.6 Tabela: historico_usuario

```sql
CREATE TABLE historico_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL,
    ponto_id UUID NOT NULL,
    status_consumo VARCHAR(20) NOT NULL DEFAULT 'nao_visto',
    data_consumo TIMESTAMP WITH TIME ZONE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT fk_historico_usuario_ponto
        FOREIGN KEY (ponto_id) 
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE,
    
    CONSTRAINT chk_status_consumo_valid
        CHECK (status_consumo IN ('nao_visto', 'consumido'))
);

-- Índice composto crítico para query dos 20 pontos mais próximos
-- Este índice permite filtrar pontos não consumidos por usuário em O(log n)
CREATE INDEX idx_historico_usuario_ponto_status 
    ON historico_usuario (usuario_id, ponto_id, status_consumo);

-- Índice para histórico temporal do usuário
CREATE INDEX idx_historico_usuario_data_consumo 
    ON historico_usuario (usuario_id, data_consumo DESC);
```

### 6.7 Views Auxiliares para Performance

```sql
-- View que combina ponto + conteúdo + contexto em uma única query
CREATE VIEW vw_ponto_completo AS
SELECT 
    pt.id,
    pt.nome,
    pt.geom,
    pt.raio_gatilho_metros,
    cm.idioma,
    cm.titulo_local,
    cm.roteiro_texto,
    cm.audio_url,
    cm.imagem_url,
    c.horarios_funcionamento,
    c.custo_ingresso,
    c.categorias,
    c.fonte_url,
    c.data_ultima_verificacao
FROM ponto_turistico pt
LEFT JOIN conteudo_midia cm ON pt.id = cm.ponto_id
LEFT JOIN contexto c ON pt.id = c.ponto_id
WHERE pt.ativo = TRUE;
```

---

## 7. Lógica do Algoritmo da Macro-Cerca

### 7.1 Visão Geral

O algoritmo deve resolver três problemas simultâneos:
1. **Busca Espacial Eficiente:** Encontrar os 20 pontos mais próximos da localização atual
2. **Filtro de Consumo:** Excluir pontos já marcados como 'consumido' pelo usuário
3. **Paginação Dinâmica:** Se houver menos de 20 pontos disponíveis após o filtro, buscar na "cauda longa" (pontos mais distantes)

### 7.2 Pseudocódigo Estruturado

```
FUNCAO buscar_20_pontos_proximos(usuario_id, latitude, longitude, idioma = 'pt-BR')
    RETORNA Lista<PontoTuristicoCompleto>

    // PASSO 1: Converter coordenadas para geometria PostGIS
    ponto_usuario = ST_MakePoint(longitude, latitude)
    ponto_usuario = ST_SetSRID(ponto_usuario, 4326)
    ponto_usuario = ST_Transform(ponto_usuario, 3857) // Projecção métrica para cálculo de distância

    // PASSO 2: Buscar IDs de pontos já consumidos pelo usuário
    ids_consumidos = QUERY(
        SELECT ponto_id 
        FROM historico_usuario 
        WHERE usuario_id = :usuario_id 
          AND status_consumo = 'consumido'
    )

    // PASSO 3: Query principal com CTE (Common Table Expression)
    // Esta query retorna os 20 pontos mais próximos NÃO consumidos
    pontos_proximos = QUERY("""
        WITH pontos_ordenados AS (
            SELECT 
                pt.id,
                pt.nome,
                pt.geom,
                pt.raio_gatilho_metros,
                cm.titulo_local,
                cm.roteiro_texto,
                cm.audio_url,
                cm.imagem_url,
                c.horarios_funcionamento,
                c.custo_ingresso,
                c.categorias,
                -- Distância em metros (usando projeção métrica)
                ST_Distance(
                    ST_Transform(pt.geom, 3857),
                    :ponto_usuario
                ) AS distancia_metros
            FROM ponto_turistico pt
            LEFT JOIN conteudo_midia cm ON pt.id = cm.ponto_id AND cm.idioma = :idioma
            LEFT JOIN contexto c ON pt.id = c.ponto_id
            WHERE pt.ativo = TRUE
              AND pt.id NOT IN (:ids_consumidos)
            ORDER BY 
                ST_Distance(
                    ST_Transform(pt.geom, 3857),
                    :ponto_usuario
                ) ASC
            LIMIT 100 -- Busca 100 para ter margem de substituição
        )
        SELECT * FROM pontos_ordenados
        LIMIT 20
    """, 
    parametros: {
        ponto_usuario: ponto_usuario,
        idioma: idioma,
        ids_consumidos: ids_consumidos
    })

    // PASSO 4: Se retornou menos de 20 pontos, buscar na cauda longa
    SE pontos_proximos.length < 20 ENTAO
        quantidade_faltando = 20 - pontos_proximos.length
        
        // Buscar próximos pontos ignorando a restrição de distância inicial
        pontos_cauda = QUERY("""
            SELECT 
                pt.id,
                pt.nome,
                pt.geom,
                pt.raio_gatilho_metros,
                cm.titulo_local,
                cm.roteiro_texto,
                cm.audio_url,
                cm.imagem_url,
                c.horarios_funcionamento,
                c.custo_ingresso,
                c.categorias,
                ST_Distance(
                    ST_Transform(pt.geom, 3857),
                    :ponto_usuario
                ) AS distancia_metros
            FROM ponto_turistico pt
            LEFT JOIN conteudo_midia cm ON pt.id = cm.ponto_id AND cm.idioma = :idioma
            LEFT JOIN contexto c ON pt.id = c.ponto_id
            WHERE pt.ativo = TRUE
              AND pt.id NOT IN (:ids_consumidos)
              AND pt.id NOT IN (:ids_ja_selecionados)
            ORDER BY 
                ST_Distance(
                    ST_Transform(pt.geom, 3857),
                    :ponto_usuario
                ) ASC
            LIMIT :quantidade_faltando
        """,
        parametros: {
            ponto_usuario: ponto_usuario,
            idioma: idioma,
            ids_consumidos: ids_consumidos,
            ids_ja_selecionados: [p.id for p in pontos_proximos],
            quantidade_faltando: quantidade_faltando
        })
        
        pontos_proximos = pontos_proximos.concat(pontos_cauda)
    FIM

    // PASSO 5: Adicionar metadados de rank para UI (1-20)
    PARA cada ponto EM pontos_proximos COM indice i:
        ponto.rank = i + 1
        ponto.is_premium = (i >= 10) // Pontos 11-20 são premium
    FIM

    RETORNA pontos_proximos
FIM
```

### 7.3 Query SQL Final (Otimizada)

```sql
-- Esta query é a versão SQL do pseudocódigo acima
-- Deve ser executada via API REST do Supabase ou via Edge Function

WITH 
-- 1. Obter IDs de pontos já consumidos
pontos_consumidos AS (
    SELECT ponto_id
    FROM historico_usuario
    WHERE usuario_id = $1 -- :usuario_id
      AND status_consumo = 'consumido'
),

-- 2. Calcular distância e ordenar pontos não consumidos
pontos_ordenados AS (
    SELECT 
        pt.id,
        pt.nome,
        pt.geom,
        pt.raio_gatilho_metros,
        cm.titulo_local,
        cm.roteiro_texto,
        cm.audio_url,
        cm.imagem_url,
        c.horarios_funcionamento,
        c.custo_ingresso,
        c.categorias,
        -- Distância em metros (projecção Web Mercator)
        ROUND(
            ST_Distance(
                ST_Transform(pt.geom, 3857),
                ST_Transform(ST_SetSRID(ST_MakePoint($3, $2), 4326), 3857)
            )
        ) AS distancia_metros
    FROM ponto_turistico pt
    LEFT JOIN conteudo_midia cm ON pt.id = cm.ponto_id AND cm.idioma = $4 -- :idioma
    LEFT JOIN contexto c ON pt.id = c.ponto_id
    WHERE pt.ativo = TRUE
      AND pt.id NOT IN (SELECT ponto_id FROM pontos_consumidos)
    ORDER BY 
        ST_Distance(
            ST_Transform(pt.geom, 3857),
            ST_Transform(ST_SetSRID(ST_MakePoint($3, $2), 4326), 3857)
        ) ASC
    LIMIT 20
)

-- 3. Retornar resultado final com rank
SELECT 
    *,
    ROW_NUMBER() OVER (ORDER BY distancia_metros ASC) AS rank,
    CASE 
        WHEN ROW_NUMBER() OVER (ORDER BY distancia_metros ASC) <= 10 THEN FALSE
        ELSE TRUE
    END AS is_premium
FROM pontos_ordenados;
```

### 7.4 Parâmetros da Query

| Parâmetro | Tipo | Descrição |
|-----------|------|-----------|
| `$1` | UUID | `usuario_id` do usuário atual |
| `$2` | DECIMAL(10,8) | `latitude` atual do usuário |
| `$3` | DECIMAL(11,8) | `longitude` atual do usuário |
| `$4` | VARCHAR(5) | `idioma` desejado (default: 'pt-BR') |

### 7.5 Performance Esperada

Com os índices GIST espaciais e os índices compostos criados:
- **Tempo de execução:** < 50ms para bancos com até 100.000 pontos
- **Complexidade:** O(log n) para busca espacial + O(k) para filtro de consumidos
- **Escalabilidade:** Suporta milhões de pontos sem degradação significativa

### 7.6 Estratégia de Cache no Client-Side

Para reduzir chamadas à API:
```kotlin
// Cache local com SQLDelight
// Chave: "pontos_${usuario_id}_${lat}_${lng}_${idioma}"
// TTL: 5 minutos ou 1km de distância percorrida

data class PontoCacheKey(
    val usuarioId: String,
    val latitude: Double,
    val longitude: Double,
    val idioma: String
)

fun deveRecarregar(cacheKey: PontoCacheKey, ultimoCache: PontoCacheKey?): Boolean {
    if (ultimoCache == null) return true
    if (cacheKey.usuarioId != ultimoCache.usuarioId) return true
    if (cacheKey.idioma != ultimoCache.idioma) return true
    
    // Calcular distância entre coordenadas (Haversine formula)
    val distanciaKm = haversine(
        cacheKey.latitude, cacheKey.longitude,
        ultimoCache.latitude, ultimoCache.longitude
    )
    
    return distanciaKm > 1.0 // Recarregar se moveu mais de 1km
}
```

---
