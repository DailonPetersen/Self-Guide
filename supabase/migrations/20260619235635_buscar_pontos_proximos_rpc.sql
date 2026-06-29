CREATE OR REPLACE FUNCTION buscar_pontos_proximos(
    p_usuario_id UUID,
    p_latitude DOUBLE PRECISION,
    p_longitude DOUBLE PRECISION,
    p_idioma VARCHAR DEFAULT 'pt-BR'
)
RETURNS TABLE (
    id UUID,
    nome VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    raio_gatilho_metros INTEGER,
    categoria_principal VARCHAR(50),
    place_id VARCHAR(255),
    content_hash VARCHAR(64),
    status VARCHAR(30),
    idioma VARCHAR(10),
    titulo_local VARCHAR(255),
    roteiro_rapido TEXT,
    roteiro_completo TEXT,
    audio_url_rapido VARCHAR(512),
    audio_url_completo VARCHAR(512),
    imagem_url VARCHAR(512),
    horarios_funcionamento TEXT,
    custo_ingresso TEXT,
    categorias TEXT[],
    fonte_url VARCHAR(512),
    distancia_metros BIGINT,
    rank BIGINT,
    is_premium BOOLEAN,
    is_ativo BOOLEAN
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public
AS $$
WITH ponto_usuario AS (
    SELECT
        ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326) AS geom,
        ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography AS geog
),
pontos_consumidos AS (
    SELECT ponto_id
    FROM historico_usuario
    WHERE usuario_id = p_usuario_id
      AND status_consumo = 'consumido'
),
pontos_ordenados AS (
    SELECT
        pt.id,
        pt.nome,
        ST_Y(pt.geom) AS latitude,
        ST_X(pt.geom) AS longitude,
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
        COALESCE(c.categorias, ARRAY[]::TEXT[]) AS categorias,
        c.fonte_url,
        ROUND(
            ST_Distance(
                ST_Transform(pt.geom, 3857),
                ST_Transform(pu.geom, 3857)
            )
        )::BIGINT AS distancia_metros
    FROM ponto_turistico pt
    CROSS JOIN ponto_usuario pu
    LEFT JOIN conteudo_midia cm
        ON cm.ponto_id = pt.id
       AND cm.idioma = p_idioma
    LEFT JOIN contexto c
        ON c.ponto_id = pt.id
    WHERE pt.ativo = TRUE
      AND pt.status = 'ativo'
      AND NOT EXISTS (
          SELECT 1
          FROM pontos_consumidos pc
          WHERE pc.ponto_id = pt.id
      )
    ORDER BY distancia_metros ASC
    LIMIT 25
),
pontos_ranqueados AS (
    SELECT
        *,
        ROW_NUMBER() OVER (ORDER BY distancia_metros ASC) AS rank
    FROM pontos_ordenados
)
SELECT
    id,
    nome,
    latitude,
    longitude,
    raio_gatilho_metros,
    categoria_principal,
    place_id,
    content_hash,
    status,
    idioma,
    titulo_local,
    roteiro_rapido,
    roteiro_completo,
    audio_url_rapido,
    audio_url_completo,
    imagem_url,
    horarios_funcionamento,
    custo_ingresso,
    categorias,
    fonte_url,
    distancia_metros,
    rank,
    rank > 10 AS is_premium,
    rank <= 20 AS is_ativo
FROM pontos_ranqueados;
$$;

REVOKE ALL ON FUNCTION buscar_pontos_proximos(UUID, DOUBLE PRECISION, DOUBLE PRECISION, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION buscar_pontos_proximos(UUID, DOUBLE PRECISION, DOUBLE PRECISION, VARCHAR) TO authenticated, service_role;
