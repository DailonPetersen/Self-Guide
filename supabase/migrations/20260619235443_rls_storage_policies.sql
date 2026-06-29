CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT auth.uid() IS NOT NULL
       AND auth.jwt() -> 'app_metadata' ->> 'role' = 'admin';
$$;

ALTER TABLE ponto_turistico ENABLE ROW LEVEL SECURITY;
ALTER TABLE conteudo_midia ENABLE ROW LEVEL SECURITY;
ALTER TABLE contexto ENABLE ROW LEVEL SECURITY;
ALTER TABLE controle_erros ENABLE ROW LEVEL SECURITY;
ALTER TABLE reportes_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE reputacao_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sugestoes_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE votos_sugestao ENABLE ROW LEVEL SECURITY;
ALTER TABLE historico_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE compras_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE log_jobs ENABLE ROW LEVEL SECURITY;

GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT INSERT, UPDATE ON historico_usuario, reportes_usuario, votos_sugestao, sugestoes_usuario TO authenticated;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

DROP POLICY IF EXISTS "admin_all_ponto_turistico" ON ponto_turistico;
CREATE POLICY "admin_all_ponto_turistico" ON ponto_turistico
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "pontos_publicos_leitura" ON ponto_turistico;
CREATE POLICY "pontos_publicos_leitura" ON ponto_turistico
    FOR SELECT TO anon, authenticated
    USING (ativo = TRUE AND status = 'ativo');

DROP POLICY IF EXISTS "admin_all_conteudo_midia" ON conteudo_midia;
CREATE POLICY "admin_all_conteudo_midia" ON conteudo_midia
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "conteudo_publico_leitura" ON conteudo_midia;
CREATE POLICY "conteudo_publico_leitura" ON conteudo_midia
    FOR SELECT TO anon, authenticated
    USING (TRUE);

DROP POLICY IF EXISTS "admin_all_contexto" ON contexto;
CREATE POLICY "admin_all_contexto" ON contexto
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "contexto_publico_leitura" ON contexto;
CREATE POLICY "contexto_publico_leitura" ON contexto
    FOR SELECT TO anon, authenticated
    USING (TRUE);

DROP POLICY IF EXISTS "admin_all_controle_erros" ON controle_erros;
CREATE POLICY "admin_all_controle_erros" ON controle_erros
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "usuarios_proprios_reportes" ON reportes_usuario;
CREATE POLICY "usuarios_proprios_reportes" ON reportes_usuario
    FOR ALL TO authenticated
    USING (usuario_id = auth.uid())
    WITH CHECK (usuario_id = auth.uid());

DROP POLICY IF EXISTS "admin_all_reportes_usuario" ON reportes_usuario;
CREATE POLICY "admin_all_reportes_usuario" ON reportes_usuario
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "usuarios_propria_reputacao" ON reputacao_usuario;
CREATE POLICY "usuarios_propria_reputacao" ON reputacao_usuario
    FOR SELECT TO authenticated
    USING (usuario_id = auth.uid());

DROP POLICY IF EXISTS "admin_all_reputacao_usuario" ON reputacao_usuario;
CREATE POLICY "admin_all_reputacao_usuario" ON reputacao_usuario
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "sugestoes_publicas_leitura" ON sugestoes_usuario;
CREATE POLICY "sugestoes_publicas_leitura" ON sugestoes_usuario
    FOR SELECT TO anon, authenticated
    USING (TRUE);

DROP POLICY IF EXISTS "usuarios_criam_sugestoes" ON sugestoes_usuario;
CREATE POLICY "usuarios_criam_sugestoes" ON sugestoes_usuario
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "admin_all_sugestoes_usuario" ON sugestoes_usuario;
CREATE POLICY "admin_all_sugestoes_usuario" ON sugestoes_usuario
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "usuarios_proprios_votos" ON votos_sugestao;
CREATE POLICY "usuarios_proprios_votos" ON votos_sugestao
    FOR ALL TO authenticated
    USING (usuario_id = auth.uid())
    WITH CHECK (usuario_id = auth.uid());

DROP POLICY IF EXISTS "admin_all_votos_sugestao" ON votos_sugestao;
CREATE POLICY "admin_all_votos_sugestao" ON votos_sugestao
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "usuarios_proprio_historico" ON historico_usuario;
CREATE POLICY "usuarios_proprio_historico" ON historico_usuario
    FOR ALL TO authenticated
    USING (usuario_id = auth.uid())
    WITH CHECK (usuario_id = auth.uid());

DROP POLICY IF EXISTS "admin_all_historico_usuario" ON historico_usuario;
CREATE POLICY "admin_all_historico_usuario" ON historico_usuario
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "usuarios_proprias_compras" ON compras_usuario;
CREATE POLICY "usuarios_proprias_compras" ON compras_usuario
    FOR SELECT TO authenticated
    USING (usuario_id = auth.uid());

DROP POLICY IF EXISTS "admin_all_compras_usuario" ON compras_usuario;
CREATE POLICY "admin_all_compras_usuario" ON compras_usuario
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "admin_all_log_jobs" ON log_jobs;
CREATE POLICY "admin_all_log_jobs" ON log_jobs
    FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "public_read_media_buckets" ON storage.objects;
CREATE POLICY "public_read_media_buckets" ON storage.objects
    FOR SELECT TO anon, authenticated
    USING (bucket_id IN ('audios', 'imagens'));

DROP POLICY IF EXISTS "service_role_write_media_buckets" ON storage.objects;
CREATE POLICY "service_role_write_media_buckets" ON storage.objects
    FOR ALL TO service_role
    USING (bucket_id IN ('audios', 'imagens'))
    WITH CHECK (bucket_id IN ('audios', 'imagens'));
