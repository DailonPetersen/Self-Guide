# Guia Supabase — Tour Guiado Automatizado
> Introdução prática ao Supabase aplicada diretamente ao projeto.

---

## O que é o Supabase?

Supabase é uma plataforma de backend open-source que fornece tudo que um projeto precisa sem gerenciar servidores. É construído sobre o PostgreSQL — o mesmo banco que nosso plano já usa com PostGIS.

A analogia mais direta: **Supabase é o Firebase do PostgreSQL.** Você foca no produto, não em infraestrutura.

---

## O que o Supabase entrega para este projeto

| Necessidade do Projeto | Solução Supabase |
|------------------------|-----------------|
| PostgreSQL + PostGIS para queries espaciais | Banco gerenciado com PostGIS habilitado nativamente |
| API REST para o app mobile consumir dados | Auto-gerada a partir das tabelas, zero código |
| Autenticação de usuários | Supabase Auth (email, Google, Apple) |
| Storage para MP3s e imagens | Supabase Storage (compatível com S3) |
| Lógica do Agente de IA (cron jobs, pipelines) | Edge Functions (Deno/TypeScript) |
| Cron jobs de auditoria diária | `pg_cron` integrado |
| Invalidação de cache via content_hash | RPC Functions customizadas |

---

## Estrutura do Projeto no Supabase

```
Supabase Project: tour-guiado
│
├── Database (PostgreSQL + PostGIS)
│   ├── Tabelas (schema definido em 03_database_schema_final.md)
│   ├── Views (vw_ponto_completo, vw_usuario_acesso)
│   ├── pg_cron (job_auditoria_diaria, job_moderar_sugestoes)
│   └── Row Level Security (RLS policies)
│
├── Auth
│   ├── Email + senha
│   ├── OAuth Google
│   └── OAuth Apple (necessário para iOS)
│
├── Storage
│   ├── Bucket: audios/ (público)
│   │   └── {ponto_id}/{idioma}/{versao}.mp3
│   └── Bucket: imagens/ (público)
│       └── {ponto_id}/thumb.jpg
│
└── Edge Functions (Deno/TypeScript)
    ├── buscar-pontos-proximos    → Query PostGIS dos Top 25
    ├── verificar-content-hash    → Invalidação de cache
    ├── audit-daily               → Auditoria diária (chamada pelo pg_cron)
    ├── audit-priority            → Auditoria por reporte urgente
    ├── moderate-suggestions      → Moderação de sugestões com IA
    └── batch-ingestion           → Carga inicial de cidades (admin only)
```

---

## Configuração Inicial (Passo a Passo)

### 1. Criar o Projeto

1. Acesse [supabase.com](https://supabase.com) e crie uma conta
2. Clique em "New Project"
3. Escolha nome: `tour-guiado`
4. Região: **South America (São Paulo)** — menor latência para usuários brasileiros
5. Anote as credenciais: `Project URL`, `anon key`, `service_role key`

### 2. Habilitar Extensões

No SQL Editor do Supabase, execute:

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

### 3. Criar as Tabelas

Execute o DDL completo do arquivo `03_database_schema_final.md` no SQL Editor.

### 4. Configurar Row Level Security (RLS)

RLS garante que cada usuário só acessa seus próprios dados:

```sql
-- Habilitar RLS nas tabelas sensíveis
ALTER TABLE historico_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE compras_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE reputacao_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE reportes_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE votos_sugestao ENABLE ROW LEVEL SECURITY;

-- Política: usuário só vê e edita seus próprios registros
CREATE POLICY "usuarios_proprio_historico" ON historico_usuario
    FOR ALL USING (auth.uid() = usuario_id);

CREATE POLICY "usuarios_proprias_compras" ON compras_usuario
    FOR ALL USING (auth.uid() = usuario_id);

CREATE POLICY "usuarios_propria_reputacao" ON reputacao_usuario
    FOR ALL USING (auth.uid() = usuario_id);

-- Tabelas públicas: qualquer usuário autenticado pode ler
CREATE POLICY "pontos_publicos_leitura" ON ponto_turistico
    FOR SELECT USING (ativo = TRUE AND status = 'ativo');

CREATE POLICY "conteudo_publico_leitura" ON conteudo_midia
    FOR SELECT USING (TRUE);

CREATE POLICY "contexto_publico_leitura" ON contexto
    FOR SELECT USING (TRUE);
```

### 5. Configurar Storage

```sql
-- No painel: Storage > New bucket
-- Nome: audios, Public: true
-- Nome: imagens, Public: true

-- Política de upload (apenas service_role pode fazer upload)
CREATE POLICY "service_role_upload_audios" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'audios' AND
        auth.role() = 'service_role'
    );

CREATE POLICY "publico_download_audios" ON storage.objects
    FOR SELECT USING (bucket_id = 'audios');
```

---

## Consumindo a API no App Mobile (Kotlin + Ktor)

O Supabase gera automaticamente uma API REST para todas as tabelas. No app, usamos o cliente oficial:

```kotlin
// build.gradle.kts
implementation("io.github.jan-tennert.supabase:postgrest-kt:2.x.x")
implementation("io.github.jan-tennert.supabase:auth-kt:2.x.x")
implementation("io.github.jan-tennert.supabase:storage-kt:2.x.x")
implementation("io.ktor:ktor-client-android:2.x.x") // Android
implementation("io.ktor:ktor-client-darwin:2.x.x")  // iOS

// Inicialização (compartilhado KMP)
val supabase = createSupabaseClient(
    supabaseUrl = "https://[PROJECT_ID].supabase.co",
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Postgrest)
    install(Auth)
    install(Storage)
}
```

### Chamando a Query dos Top 25 Pontos

A query espacial complexa é encapsulada numa Edge Function (não exposta como tabela direta):

```kotlin
// Repositório de pontos (shared KMP)
class PontosRepository(private val supabase: SupabaseClient) {

    suspend fun buscarPontosProximos(
        usuarioId: String,
        latitude: Double,
        longitude: Double,
        idioma: String = "pt-BR"
    ): List<PontoTuristico> {
        val response = supabase.functions.invoke(
            function = "buscar-pontos-proximos",
            body = buildJsonObject {
                put("usuario_id", usuarioId)
                put("latitude", latitude)
                put("longitude", longitude)
                put("idioma", idioma)
            }
        )
        return response.decodeList<PontoTuristico>()
    }

    suspend fun verificarContentHash(
        pontos: List<Pair<String, String>> // (ponto_id, content_hash)
    ): List<String> { // Retorna IDs com hash divergente
        val response = supabase.functions.invoke(
            function = "verificar-content-hash",
            body = buildJsonObject {
                putJsonArray("pontos") {
                    pontos.forEach { (id, hash) ->
                        addJsonObject {
                            put("ponto_id", id)
                            put("content_hash", hash)
                        }
                    }
                }
            }
        )
        return response.decodeList<String>()
    }
}
```

### Autenticação

```kotlin
// Login com email/senha
supabase.auth.signInWith(Email) {
    email = "usuario@email.com"
    password = "senha123"
}

// Login com Google (Android)
supabase.auth.signInWith(Google)

// Obter usuário atual
val usuario = supabase.auth.currentUserOrNull()
val usuarioId = usuario?.id
```

### Upload de Áudio (Agente de IA — Node.js/Deno)

```typescript
// Edge Function: batch-ingestion
const { data, error } = await supabase.storage
  .from('audios')
  .upload(
    `${pontoId}/pt-BR/rapido.mp3`,
    audioBuffer,
    { contentType: 'audio/mpeg', upsert: true }
  );

const audioUrl = supabase.storage
  .from('audios')
  .getPublicUrl(`${pontoId}/pt-BR/rapido.mp3`).data.publicUrl;
```

---

## Edge Function: `buscar-pontos-proximos`

```typescript
// supabase/functions/buscar-pontos-proximos/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const { usuario_id, latitude, longitude, idioma } = await req.json()

  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
  )

  const { data, error } = await supabase.rpc('buscar_pontos_proximos', {
    p_usuario_id: usuario_id,
    p_latitude: latitude,
    p_longitude: longitude,
    p_idioma: idioma ?? 'pt-BR'
  })

  if (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  }

  return new Response(JSON.stringify(data), {
    headers: { 'Content-Type': 'application/json' }
  })
})
```

```sql
-- Função SQL correspondente (executada no banco)
CREATE OR REPLACE FUNCTION buscar_pontos_proximos(
  p_usuario_id UUID,
  p_latitude DECIMAL,
  p_longitude DECIMAL,
  p_idioma VARCHAR DEFAULT 'pt-BR'
)
RETURNS TABLE (...) -- schema completo no arquivo 03_database_schema_final.md
LANGUAGE SQL
STABLE
AS $$
  -- Query completa do arquivo 03_database_schema_final.md
$$;
```

---

## Variáveis de Ambiente Necessárias

```bash
# .env (nunca commitado)
SUPABASE_URL=https://[PROJECT_ID].supabase.co
SUPABASE_ANON_KEY=eyJ...          # Segura para expor no app mobile
SUPABASE_SERVICE_ROLE_KEY=eyJ...  # NUNCA exposta no app — só Edge Functions e admin

# Agente de IA
TTS_PROVIDER=google                # 'google' | 'polly' | 'elevenlabs'
GOOGLE_TTS_API_KEY=AIza...
GOOGLE_PLACES_API_KEY=AIza...
ANTHROPIC_API_KEY=sk-ant-...       # Para os prompts de geração de roteiro

# Pagamentos
PIX_GATEWAY_KEY=...
APPLE_IAP_SECRET=...
GOOGLE_PLAY_KEY=...
```

---

## Monitoramento e Observabilidade

O Supabase fornece gratuitamente no dashboard:

- **Logs de API:** todas as requisições com latência e erros
- **Logs de Edge Functions:** stdout/stderr de cada invocação
- **Query Performance:** queries lentas e planos de execução
- **Storage Usage:** consumo de banda e armazenamento
- **Auth Logs:** logins, registros, tokens expirados

Para produção, considere adicionar:
- **Sentry** para erros no app mobile e Edge Functions
- **Uptime Robot** para monitorar disponibilidade da API

---

## Limites do Free Tier (MVP)

| Recurso | Free Tier | Quando Fazer Upgrade |
|---------|-----------|---------------------|
| Banco de dados | 500 MB | ~100.000 pontos turísticos |
| Storage | 1 GB | ~500 áudios de 2MB cada |
| Edge Functions | 500K invocações/mês | ~16K usuários ativos/dia |
| Auth | Ilimitado | Nunca |
| Bandwidth | 5 GB/mês | ~2.500 downloads de áudio/mês |

**Conclusão:** Free tier suporta confortavelmente o MVP até ~1.000 usuários ativos mensais. O plano Pro ($25/mês) remove todos esses limites.
