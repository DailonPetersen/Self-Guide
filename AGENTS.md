# SelfGuide - Configuração do Projeto

## Supabase

- **Project ID:** `pyfqdfyujlufzuxnzhve`
- **Project Ref:** `pyfqdfyujlufzuxnzhve`
- **Região:** `sa-east-1` (South America - São Paulo)
- **URL:** https://pyfqdfyujlufzuxnzhve.supabase.co

## Extensões Habilitadas

| Extensão | Propósito |
|----------|-----------|
| postgis | Suporte a dados geográficos e queries de proximidade |
| uuid-ossp | Geração automática de identificadores UUIDv4 |
| pg_cron | Agendamento de tarefas do Agente de IA em background |
| pg_trgm | Suporte futuro a fuzzy search nos nomes de locais |

## Credenciais

As credenciais do Supabase já estão configuradas no `.env`:

- `SUPABASE_URL` - URL do projeto
- `SUPABASE_ANON_KEY` - Chave pública para uso no client-side
- `SUPABASE_SERVICE_ROLE_KEY` - Chave administrativa secreta (já configurada)

## Schema do Banco

Todas as tabelas criadas com sucesso:
- `ponto_turistico` (com geom/geog espaciais)
- `conteudo_midia`
- `contexto`
- `controle_erros`
- `reportes_usuario`
- `reputacao_usuario`
- `sugestoes_usuario`
- `votos_sugestao`
- `historico_usuario`
- `compras_usuario`
- `log_jobs`
- `vw_ponto_completo`
- `vw_usuario_acesso`

Índices espaciais GIST e GIN criados com sucesso.

### Segurança

- A `service_role_key` **NUNCA** deve ser exposta no app mobile ou em código client-side
- Utilize apenas `anon_key` nas integrações frontend
- Mantenha o `.env` no `.gitignore` (já configurado)

## Manipulação de Arquivos

### Ferramentas para Arquivos

- **Leitura:** Use `Read` tool (não bash) - aceita caminhos com ou sem `C:\Projects\self-guide\` prefix
- **Escrita/Edição:** Use `Write` ou `Edit` tools (não bash)
- **Busca:** Use `Glob` para arquivos, `Grep` para conteúdo
- **Diretório:** Use `Read` com caminho de pasta ou `Glob`

### Formatação de Caminhos

- **Sempre use caminhos absolutos completos:** `C:\Projects\self-guide\src\...`
- **Não misture formatos:** Evite `C:\Projects/self-guide/...` (mistura `\` e `/`)
- **Use aspas duplas** em comandos PowerShell com caminhos contendo espaços

### Exemplo Correto

```
Read: C:\Projects\self-guide\shared\src\androidMain\kotlin\auth\GoogleSignInHelper.kt
Glob: C:\Projects\self-guide\shared\**\*.kt
Grep: pattern, include="*.kt"
```