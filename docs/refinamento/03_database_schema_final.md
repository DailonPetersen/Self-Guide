# Schema Final do Banco de Dados — Tour Guiado Automatizado
> PostgreSQL + PostGIS via Supabase. Inclui todas as decisões tomadas no refinamento.

---

## Extensões Necessárias

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_trgm; -- Para fuzzy search futuro
```

---

## Tabela: `ponto_turistico`

```sql
CREATE TABLE ponto_turistico (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    geom GEOMETRY(POINT, 4326) NOT NULL,          -- Coordenadas WGS84
    geog GEOGRAPHY(POINT, 4326) NOT NULL,          -- Para validação precisa de raio de gatilho
    raio_gatilho_metros INTEGER DEFAULT 50,        -- Raio sugerido pela IA por categoria, override manual
    categoria_principal VARCHAR(50),               -- 'Museu', 'Parque', 'Praia', 'Centro Historico', etc.
    place_id VARCHAR(255) UNIQUE,                  -- Google Places ID (canônico, evita duplicatas)
    status VARCHAR(30) DEFAULT 'ativo',            -- 'ativo', 'pendente_moderacao', 'rejeitado'
    content_hash VARCHAR(64),                      -- SHA256 do conteúdo — invalida cache do cliente
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_status_valido
        CHECK (status IN ('ativo', 'pendente_moderacao', 'rejeitado')),
    CONSTRAINT chk_raio_positivo
        CHECK (raio_gatilho_metros > 0)
);

-- Índice espacial GIST para queries de proximidade (Web Mercator - performance)
CREATE INDEX idx_ponto_turistico_geom ON ponto_turistico USING GIST (geom);

-- Índice espacial para geography (precisão geodésica - validação de gatilho)
CREATE INDEX idx_ponto_turistico_geog ON ponto_turistico USING GIST (geog);

-- Índices de filtro
CREATE INDEX idx_ponto_turistico_ativo ON ponto_turistico (ativo);
CREATE INDEX idx_ponto_turistico_status ON ponto_turistico (status);
CREATE INDEX idx_ponto_turistico_place_id ON ponto_turistico (place_id);
CREATE INDEX idx_ponto_turistico_categoria ON ponto_turistico (categoria_principal);

-- Trigger de timestamp automático
CREATE TRIGGER trg_ponto_turistico_atualizacao
    BEFORE UPDATE ON ponto_turistico
    FOR EACH ROW EXECUTE FUNCTION trigger_atualizar_timestamp();
```

**Raios padrão por categoria (lógica do agente de IA):**

| Categoria | Raio Padrão |
|-----------|-------------|
| Museu | 50m |
| Monumento | 50m |
| Igreja / Catedral | 75m |
| Centro Histórico | 150m |
| Parque | 200m |
| Praia | 300m |
| Complexo / Campus | 200m |

---

## Tabela: `conteudo_midia`

```sql
CREATE TABLE conteudo_midia (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ponto_id UUID NOT NULL,
    idioma VARCHAR(10) DEFAULT 'pt-BR',           -- 'pt-BR', 'en-US', 'es-ES', etc.
    titulo_local VARCHAR(255) NOT NULL,
    roteiro_rapido TEXT NOT NULL,                 -- ~45s de áudio — tom objetivo
    roteiro_completo TEXT NOT NULL,               -- ~3min de áudio — tom narrativo/storytelling
    audio_url_rapido VARCHAR(512),                -- URL do MP3 versão rápida no Supabase Storage
    audio_url_completo VARCHAR(512),              -- URL do MP3 versão completa no Supabase Storage
    imagem_url VARCHAR(512),                      -- URL da imagem oficial (Google Places / externo)
    tts_provider VARCHAR(50) DEFAULT 'google',    -- Provider usado: 'google', 'polly', 'elevenlabs'
    tts_voice_id VARCHAR(100),                    -- ID da voz específica usada
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT fk_conteudo_midia_ponto
        FOREIGN KEY (ponto_id)
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_conteudo_ponto_idioma
        UNIQUE (ponto_id, idioma)                 -- Um registro por ponto por idioma
);

CREATE INDEX idx_conteudo_midia_ponto_idioma ON conteudo_midia (ponto_id, idioma);
CREATE INDEX idx_conteudo_midia_idioma ON conteudo_midia (idioma);

CREATE TRIGGER trg_conteudo_midia_atualizacao
    BEFORE UPDATE ON conteudo_midia
    FOR EACH ROW EXECUTE FUNCTION trigger_atualizar_timestamp();
```

---

## Tabela: `contexto`

```sql
CREATE TABLE contexto (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ponto_id UUID NOT NULL,
    horarios_funcionamento TEXT,                  -- Ex: "Ter-Dom: 09h às 17h"
    custo_ingresso TEXT,                          -- Ex: "R$ 30,00" ou "Gratuito"
    categorias TEXT[],                            -- Tags: ["Histórico", "Natureza", "Arte"]
    fonte_url VARCHAR(512),                       -- URL de origem para rastreabilidade
    data_ultima_verificacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT fk_contexto_ponto
        FOREIGN KEY (ponto_id)
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_contexto_ultima_verificacao ON contexto (data_ultima_verificacao);
CREATE INDEX idx_contexto_categorias ON contexto USING GIN (categorias);
```

---

## Tabela: `controle_erros`

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

    CONSTRAINT chk_contador_nonnegative
        CHECK (contador_alertas >= 0)
);

CREATE INDEX idx_controle_erros_contador ON controle_erros (contador_alertas);
CREATE INDEX idx_controle_erros_ponto ON controle_erros (ponto_id, contador_alertas);
```

---

## Tabela: `reportes_usuario` *(nova)*

```sql
-- Registra cada reporte individual para rastreabilidade e sistema de reputação
CREATE TABLE reportes_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL,
    ponto_id UUID NOT NULL,
    categoria VARCHAR(50) NOT NULL,               -- 'local_fechado', 'horario_errado', 'preco_desatualizado', 'foto_incorreta', 'outro'
    descricao_livre TEXT,                         -- Texto opcional do usuário
    severidade VARCHAR(20) NOT NULL,              -- 'critica', 'alta', 'media', 'baixa'
    status VARCHAR(30) DEFAULT 'pendente',        -- 'pendente', 'corrigido', 'sem_divergencia'
    correcao_aplicada BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_resolucao TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_reporte_ponto
        FOREIGN KEY (ponto_id)
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_categoria_valida
        CHECK (categoria IN ('local_fechado', 'horario_errado', 'preco_desatualizado', 'foto_incorreta', 'outro')),

    CONSTRAINT chk_severidade_valida
        CHECK (severidade IN ('critica', 'alta', 'media', 'baixa')),

    CONSTRAINT chk_status_reporte_valido
        CHECK (status IN ('pendente', 'corrigido', 'sem_divergencia'))
);

CREATE INDEX idx_reportes_usuario_ponto ON reportes_usuario (ponto_id, status);
CREATE INDEX idx_reportes_usuario_usuario ON reportes_usuario (usuario_id, correcao_aplicada);
```

**Mapeamento de severidade por categoria:**

| Categoria | Severidade | Threshold |
|-----------|------------|-----------|
| local_fechado | critica | 1 reporte |
| horario_errado | alta | 3 reportes |
| preco_desatualizado | alta | 3 reportes |
| foto_incorreta | baixa | 3 reportes |
| outro | media | 3 reportes |

---

## Tabela: `reputacao_usuario` *(nova)*

```sql
CREATE TABLE reputacao_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL UNIQUE,
    pontos_reputacao INTEGER DEFAULT 0,
    total_reportes_validos INTEGER DEFAULT 0,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_pontos_nonnegative
        CHECK (pontos_reputacao >= 0)
);

CREATE INDEX idx_reputacao_usuario ON reputacao_usuario (usuario_id);

-- Badges são derivados de total_reportes_validos:
-- 'Explorador Atento': >= 1
-- 'Guardião da Cidade': >= 5
-- 'Curador Oficial': >= 20
```

---

## Tabela: `sugestoes_usuario` *(nova)*

```sql
CREATE TABLE sugestoes_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    place_id VARCHAR(255) NOT NULL,               -- Google Places ID canônico
    nome_sugerido VARCHAR(255),                   -- Nome digitado pelo primeiro usuário
    geom GEOMETRY(POINT, 4326),                   -- Coordenadas normalizadas via Places API
    contador_votos INTEGER DEFAULT 1,
    status VARCHAR(30) DEFAULT 'aguardando_votos', -- 'aguardando_votos', 'em_moderacao', 'aprovado', 'rejeitado'
    motivo_rejeicao TEXT,                         -- Preenchido pela IA ao rejeitar
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT uq_sugestao_place_id
        UNIQUE (place_id),

    CONSTRAINT chk_status_sugestao_valido
        CHECK (status IN ('aguardando_votos', 'em_moderacao', 'aprovado', 'rejeitado')),

    CONSTRAINT chk_votos_positivo
        CHECK (contador_votos > 0)
);

CREATE INDEX idx_sugestoes_status ON sugestoes_usuario (status, contador_votos DESC);
CREATE INDEX idx_sugestoes_place_id ON sugestoes_usuario (place_id);
```

---

## Tabela: `votos_sugestao` *(nova)*

```sql
-- Garante que cada usuário vote apenas uma vez por sugestão
CREATE TABLE votos_sugestao (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sugestao_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT fk_voto_sugestao
        FOREIGN KEY (sugestao_id)
        REFERENCES sugestoes_usuario(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_voto_usuario_sugestao
        UNIQUE (sugestao_id, usuario_id)          -- Um voto por usuário por sugestão
);

CREATE INDEX idx_votos_sugestao ON votos_sugestao (sugestao_id, usuario_id);
```

---

## Tabela: `historico_usuario`

```sql
CREATE TABLE historico_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL,
    ponto_id UUID NOT NULL,
    status_consumo VARCHAR(20) NOT NULL DEFAULT 'nao_visto',
    audio_versao_consumida VARCHAR(10),           -- 'rapida' ou 'completa'
    idioma_consumido VARCHAR(10),
    data_consumo TIMESTAMP WITH TIME ZONE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    pendente_sync BOOLEAN DEFAULT FALSE,          -- TRUE quando atualizado offline

    CONSTRAINT fk_historico_ponto
        FOREIGN KEY (ponto_id)
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_status_consumo_valido
        CHECK (status_consumo IN ('nao_visto', 'consumido')),

    CONSTRAINT uq_historico_usuario_ponto
        UNIQUE (usuario_id, ponto_id)
);

CREATE INDEX idx_historico_usuario_ponto_status ON historico_usuario (usuario_id, ponto_id, status_consumo);
CREATE INDEX idx_historico_usuario_sync ON historico_usuario (usuario_id, pendente_sync) WHERE pendente_sync = TRUE;
```

---

## Tabela: `compras_usuario` *(nova)*

```sql
CREATE TABLE compras_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL,
    tipo_compra VARCHAR(20) NOT NULL,             -- 'cidade', 'vitalicio'
    cidade_id VARCHAR(100),                       -- NULL se tipo = 'vitalicio'
    valor_pago DECIMAL(10,2) NOT NULL,
    metodo_pagamento VARCHAR(30),                 -- 'pix', 'cartao', 'apple_pay', 'google_pay'
    status_pagamento VARCHAR(20) DEFAULT 'confirmado',
    data_compra TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_tipo_compra_valido
        CHECK (tipo_compra IN ('cidade', 'vitalicio')),

    CONSTRAINT chk_status_pagamento_valido
        CHECK (status_pagamento IN ('pendente', 'confirmado', 'estornado'))
);

CREATE INDEX idx_compras_usuario ON compras_usuario (usuario_id, tipo_compra, status_pagamento);
CREATE INDEX idx_compras_cidade ON compras_usuario (usuario_id, cidade_id) WHERE tipo_compra = 'cidade';
```

---

## Views Auxiliares

### View: `vw_ponto_completo`

```sql
CREATE VIEW vw_ponto_completo AS
SELECT
    pt.id,
    pt.nome,
    pt.geom,
    pt.geog,
    pt.raio_gatilho_metros,
    pt.categoria_principal,
    pt.place_id,
    pt.content_hash,
    pt.status,
    cm.idioma,
    cm.titulo_local,
    cm.roteiro_rapido,
    cm.roteiro_completo,
    cm.audio_url_rapido,
    cm.audio_url_completo,
    cm.imagem_url,
    c.horarios_funcionamento,
    c.custo_ingresso,
    c.categorias,
    c.fonte_url,
    c.data_ultima_verificacao
FROM ponto_turistico pt
LEFT JOIN conteudo_midia cm ON pt.id = cm.ponto_id
LEFT JOIN contexto c ON pt.id = c.ponto_id
WHERE pt.ativo = TRUE AND pt.status = 'ativo';
```

### View: `vw_usuario_acesso`

```sql
-- Determina nível de acesso do usuário por cidade
CREATE VIEW vw_usuario_acesso AS
SELECT
    usuario_id,
    CASE
        WHEN EXISTS (
            SELECT 1 FROM compras_usuario
            WHERE compras_usuario.usuario_id = c.usuario_id
              AND tipo_compra = 'vitalicio'
              AND status_pagamento = 'confirmado'
        ) THEN 'vitalicio'
        ELSE 'cidade_especifica'
    END AS tipo_acesso
FROM compras_usuario c
GROUP BY usuario_id;
```

---

## Função Auxiliar: `trigger_atualizar_timestamp`

```sql
CREATE OR REPLACE FUNCTION trigger_atualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.data_atualizacao = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

## Query Principal: Top 20 Pontos por Proximidade

```sql
-- Parâmetros: $1=usuario_id, $2=latitude, $3=longitude, $4=idioma
WITH
pontos_consumidos AS (
    SELECT ponto_id
    FROM historico_usuario
    WHERE usuario_id = $1
      AND status_consumo = 'consumido'
),
pontos_ordenados AS (
    SELECT
        pt.id,
        pt.nome,
        pt.geom,
        pt.geog,
        pt.raio_gatilho_metros,
        pt.content_hash,
        cm.titulo_local,
        cm.roteiro_rapido,
        cm.roteiro_completo,
        cm.audio_url_rapido,
        cm.audio_url_completo,
        cm.imagem_url,
        c.horarios_funcionamento,
        c.custo_ingresso,
        c.categorias,
        -- Distância em metros via Web Mercator (performance)
        ROUND(
            ST_Distance(
                ST_Transform(pt.geom, 3857),
                ST_Transform(ST_SetSRID(ST_MakePoint($3, $2), 4326), 3857)
            )
        ) AS distancia_metros
    FROM ponto_turistico pt
    LEFT JOIN conteudo_midia cm ON pt.id = cm.ponto_id AND cm.idioma = $4
    LEFT JOIN contexto c ON pt.id = c.ponto_id
    WHERE pt.ativo = TRUE
      AND pt.status = 'ativo'
      AND pt.id NOT IN (SELECT ponto_id FROM pontos_consumidos)
    ORDER BY
        ST_Distance(
            ST_Transform(pt.geom, 3857),
            ST_Transform(ST_SetSRID(ST_MakePoint($3, $2), 4326), 3857)
        ) ASC
    LIMIT 25 -- 20 ativos + 5 de substituição offline
)
SELECT
    *,
    ROW_NUMBER() OVER (ORDER BY distancia_metros ASC) AS rank,
    CASE
        WHEN ROW_NUMBER() OVER (ORDER BY distancia_metros ASC) <= 10 THEN FALSE
        ELSE TRUE
    END AS is_premium,
    CASE
        WHEN ROW_NUMBER() OVER (ORDER BY distancia_metros ASC) <= 20 THEN TRUE
        ELSE FALSE
    END AS is_ativo  -- FALSE = cache de substituição offline
FROM pontos_ordenados;
```

---

## Cron Jobs (via `pg_cron`)

```sql
-- Auditoria diária de contexto (horários, preços)
SELECT cron.schedule(
    'job_auditoria_diaria',
    '0 2 * * *',  -- Todo dia às 02:00
    $$SELECT net.http_post(
        url := current_setting('app.edge_function_url') || '/audit-daily',
        headers := '{"Authorization": "Bearer ' || current_setting('app.service_key') || '"}'::jsonb
    )$$
);

-- Verificação de sugestões prontas para moderação
SELECT cron.schedule(
    'job_moderar_sugestoes',
    '*/30 * * * *',  -- A cada 30 minutos
    $$SELECT net.http_post(
        url := current_setting('app.edge_function_url') || '/moderate-suggestions',
        headers := '{"Authorization": "Bearer ' || current_setting('app.service_key') || '"}'::jsonb
    )$$
);
```
