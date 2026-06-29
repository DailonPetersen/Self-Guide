CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pg_net;

CREATE OR REPLACE FUNCTION trigger_atualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.data_atualizacao = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE ponto_turistico (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    geom GEOMETRY(POINT, 4326) NOT NULL,
    geog GEOGRAPHY(POINT, 4326) NOT NULL,
    raio_gatilho_metros INTEGER DEFAULT 50,
    categoria_principal VARCHAR(50),
    place_id VARCHAR(255) UNIQUE,
    status VARCHAR(30) DEFAULT 'ativo',
    content_hash VARCHAR(64),
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_status_valido
        CHECK (status IN ('ativo', 'pendente_moderacao', 'rejeitado')),
    CONSTRAINT chk_raio_positivo
        CHECK (raio_gatilho_metros > 0)
);

CREATE INDEX idx_ponto_turistico_geom ON ponto_turistico USING GIST (geom);
CREATE INDEX idx_ponto_turistico_geog ON ponto_turistico USING GIST (geog);
CREATE INDEX idx_ponto_turistico_ativo ON ponto_turistico (ativo);
CREATE INDEX idx_ponto_turistico_status ON ponto_turistico (status);
CREATE INDEX idx_ponto_turistico_place_id ON ponto_turistico (place_id);
CREATE INDEX idx_ponto_turistico_categoria ON ponto_turistico (categoria_principal);
CREATE INDEX idx_ponto_turistico_content_hash ON ponto_turistico (content_hash);

CREATE TRIGGER trg_ponto_turistico_atualizacao
    BEFORE UPDATE ON ponto_turistico
    FOR EACH ROW EXECUTE FUNCTION trigger_atualizar_timestamp();

CREATE TABLE conteudo_midia (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ponto_id UUID NOT NULL,
    idioma VARCHAR(10) DEFAULT 'pt-BR',
    titulo_local VARCHAR(255) NOT NULL,
    roteiro_rapido TEXT NOT NULL,
    roteiro_completo TEXT NOT NULL,
    audio_url_rapido VARCHAR(512),
    audio_url_completo VARCHAR(512),
    imagem_url VARCHAR(512),
    tts_provider VARCHAR(50) DEFAULT 'google',
    tts_voice_id VARCHAR(100),
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT fk_conteudo_midia_ponto
        FOREIGN KEY (ponto_id)
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_conteudo_ponto_idioma
        UNIQUE (ponto_id, idioma)
);

CREATE INDEX idx_conteudo_midia_ponto_idioma ON conteudo_midia (ponto_id, idioma);
CREATE INDEX idx_conteudo_midia_idioma ON conteudo_midia (idioma);

CREATE TRIGGER trg_conteudo_midia_atualizacao
    BEFORE UPDATE ON conteudo_midia
    FOR EACH ROW EXECUTE FUNCTION trigger_atualizar_timestamp();

CREATE TABLE contexto (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ponto_id UUID NOT NULL,
    horarios_funcionamento TEXT,
    custo_ingresso TEXT,
    categorias TEXT[],
    fonte_url VARCHAR(512),
    data_ultima_verificacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT fk_contexto_ponto
        FOREIGN KEY (ponto_id)
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_contexto_ultima_verificacao ON contexto (data_ultima_verificacao);
CREATE INDEX idx_contexto_categorias ON contexto USING GIN (categorias);

CREATE TRIGGER trg_contexto_atualizacao
    BEFORE UPDATE ON contexto
    FOR EACH ROW EXECUTE FUNCTION trigger_atualizar_timestamp();

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

CREATE TABLE reportes_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL,
    ponto_id UUID NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    descricao_livre TEXT,
    severidade VARCHAR(20) NOT NULL,
    status VARCHAR(30) DEFAULT 'pendente',
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

CREATE TRIGGER trg_reputacao_usuario_atualizacao
    BEFORE UPDATE ON reputacao_usuario
    FOR EACH ROW EXECUTE FUNCTION trigger_atualizar_timestamp();

CREATE TABLE sugestoes_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    place_id VARCHAR(255) NOT NULL,
    nome_sugerido VARCHAR(255),
    geom GEOMETRY(POINT, 4326),
    contador_votos INTEGER DEFAULT 1,
    status VARCHAR(30) DEFAULT 'aguardando_votos',
    motivo_rejeicao TEXT,
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

CREATE TRIGGER trg_sugestoes_usuario_atualizacao
    BEFORE UPDATE ON sugestoes_usuario
    FOR EACH ROW EXECUTE FUNCTION trigger_atualizar_timestamp();

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
        UNIQUE (sugestao_id, usuario_id)
);

CREATE INDEX idx_votos_sugestao ON votos_sugestao (sugestao_id, usuario_id);

CREATE TABLE historico_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL,
    ponto_id UUID NOT NULL,
    status_consumo VARCHAR(20) NOT NULL DEFAULT 'nao_visto',
    audio_versao_consumida VARCHAR(10),
    idioma_consumido VARCHAR(10),
    data_consumo TIMESTAMP WITH TIME ZONE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    pendente_sync BOOLEAN DEFAULT FALSE,

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

CREATE TABLE compras_usuario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL,
    tipo_compra VARCHAR(20) NOT NULL,
    cidade_id VARCHAR(100),
    valor_pago DECIMAL(10,2) NOT NULL,
    metodo_pagamento VARCHAR(30),
    status_pagamento VARCHAR(20) DEFAULT 'confirmado',
    data_compra TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_tipo_compra_valido
        CHECK (tipo_compra IN ('cidade', 'vitalicio')),

    CONSTRAINT chk_status_pagamento_valido
        CHECK (status_pagamento IN ('pendente', 'confirmado', 'estornado'))
);

CREATE INDEX idx_compras_usuario ON compras_usuario (usuario_id, tipo_compra, status_pagamento);
CREATE INDEX idx_compras_cidade ON compras_usuario (usuario_id, cidade_id) WHERE tipo_compra = 'cidade';

CREATE TABLE log_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_name TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'running',
    ponto_id UUID,
    mensagem TEXT,
    tentativas INTEGER NOT NULL DEFAULT 0,
    data_inicio TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    data_fim TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_log_jobs_ponto
        FOREIGN KEY (ponto_id)
        REFERENCES ponto_turistico(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_log_jobs_status_valido
        CHECK (status IN ('running', 'success', 'failure', 'retry', 'falha_fatal'))
);

CREATE INDEX idx_log_jobs_job_status ON log_jobs (job_name, status);
CREATE INDEX idx_log_jobs_data_inicio ON log_jobs (data_inicio DESC);

CREATE VIEW vw_ponto_completo WITH (security_invoker = true) AS
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

CREATE VIEW vw_usuario_acesso WITH (security_invoker = true) AS
SELECT
    c.usuario_id,
    CASE
        WHEN EXISTS (
            SELECT 1 FROM compras_usuario cu
            WHERE cu.usuario_id = c.usuario_id
              AND cu.tipo_compra = 'vitalicio'
              AND cu.status_pagamento = 'confirmado'
        ) THEN 'vitalicio'
        ELSE 'cidade_especifica'
    END AS tipo_acesso,
    EXISTS (
        SELECT 1 FROM compras_usuario cu
        WHERE cu.usuario_id = c.usuario_id
          AND cu.tipo_compra = 'cidade'
          AND cu.status_pagamento = 'confirmado'
    ) AS possui_compra_cidade
FROM compras_usuario c
GROUP BY c.usuario_id;
