# Prompts de Desenvolvimento — Épico 8: Agente de IA (Server-Side)

Este arquivo contém os prompts detalhados para cada tarefa do Épico 8. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-036: Implementar interface abstrata `TTSProvider` e provider Google TTS

**Contexto do Projeto:**
Os roteiros reescritos pela IA são convertidos em áudio MP3. Queremos desacoplar o provedor de síntese de voz (Text-to-Speech) para poder migrar do Google TTS (melhor custo) paraElevenLabs (melhor qualidade) futuramente, sem reescrever o código do pipeline.

**Objetivo:**
Criar a interface genérica `TTSProvider` no backend (TypeScript/Deno) e implementar o adaptador concreto utilizando a API do Google Cloud TTS.

**Especificação Técnica de Referência:**
Consulte a seção "Interface TTS (Abstração de Provider)" em [04_ai_agent_spec.md](file:///c:/Projects/self-guide/refinamento/04_ai_agent_spec.md#L20-L55).

**Instruções de Implementação:**
1. Crie o arquivo `tts-provider.ts` no diretório das Edge Functions do Supabase (`supabase/functions/shared/`).
2. Defina a interface `TTSProvider` em TypeScript contendo as funções:
   - `generateAudio(params: { text: string; voice: string; style: 'rapida' | 'completa'; language: string }): Promise<{ audioBuffer: Buffer; durationSeconds: number }>`
   - `getVoices(language: string): Promise<Voice[]>`
   - `estimateCost(text: string): Promise<{ characters: number; estimatedUSD: number }>`
3. Crie a classe concreta `GoogleTTSProvider` estendendo a interface:
   - Realize requisições HTTP POST para a API do Google Cloud Text-to-Speech utilizando o endpoint `https://texttospeech.googleapis.com/v1/text:synthesize?key=${GOOGLE_TTS_API_KEY}`.
   - Configure o áudio de saída como `MP3`.
   - Adote as vozes oficiais sugeridas no MVP:
     - `pt-BR` → `pt-BR-Neural2-B` (Modelo Neural2)
     - `en-US` → `en-US-Neural2-D` (Modelo Neural2)
     - `es-ES` → `es-ES-Neural2-B` (Modelo Neural2)
4. Escreva uma classe Factory (`TTSProviderFactory`) que instancie o provider correto com base na variável de ambiente `TTS_PROVIDER`.

**Critérios de Aceitação e Verificação:**
- O arquivo TypeScript compila sem erros no Deno.
- Execute um script de teste simulando a chamada da factory com o texto *"Bem-vindo ao centro histórico"* no idioma `pt-BR`.
- Verifique se a API retorna com sucesso um buffer contendo um arquivo MP3 válido de 128kbps e a duração exata do áudio.

---

## Prompt para T-037: Implementar pipeline de Batch Ingestion

**Contexto do Projeto:**
O pipeline de Ingestão em Lote (Batch Ingestion) popula o banco do Supabase com novos pontos turísticos e gera todo o conteúdo de mídia (roteiros de texto e áudio por idioma) de forma assíncrona.

**Objetivo:**
Implementar a Edge Function ou Script TypeScript `batch-ingestion` no Supabase que recebe uma cidade de entrada, realiza mineração geográfica, gera roteiros via LLM, gera áudio via TTS e persiste no banco.

**Especificação Técnica de Referência:**
Consulte "Pipeline 1: Batch Ingestion" e "Prompts de Sistema do LLM" em [04_ai_agent_spec.md](file:///c:/Projects/self-guide/refinamento/04_ai_agent_spec.md#L57-L128) e [04_ai_agent_spec.md#L257-L314].

**Instruções de Implementação:**
1. Crie a Edge Function `batch-ingestion` utilizando a CLI do Supabase.
2. O payload JSON recebido pela requisição deve especificar a lista de cidades, categorias limitadas de locais e os idiomas.
3. No fluxo da função:
   - **Busca Geográfica:** Consulte o Google Places API (Text Search) filtrando locais da categoria informada naquela cidade com `rating >= 4.0` e `reviews >= 100`.
   - **Deduplicação:** Verifique se o `place_id` canônico retornado já existe em `ponto_turistico`. Se já existir, ignore o ponto e prossiga para evitar duplicatas.
   - **Geração de Roteiros:** Chame a API da Anthropic (Claude Sonnet 3.5) passando os dados do local (nome, descrição da API, etc.) para criar duas versões do roteiro no idioma correspondente:
     - *Versão Rápida:* ~45 segundos (100-130 palavras), tom objetivo.
     - *Versão Completa:* ~3 minutos (400-500 palavras), tom narrativo de storytelling. Use exatamente as regras obrigatórias descritas nos Prompts Base.
   - **Geração de Áudio:** Sintetize ambos os roteiros usando o `TTSProvider`.
   - **Armazenamento:** Salve os arquivos MP3 gerados nos caminhos correspondentes do Supabase Storage bucket `audios` e obtenha a URL pública.
   - **Persistência SQL:** Insira na tabela `ponto_turistico` com status `'ativo'`, registre em `conteudo_midia` e inicialize `contexto` e `controle_erros`. Gere o `content_hash` utilizando SHA-256 a partir dos textos de roteiro.
4. Adicione tratamento de erros com até 3 retries e backoff exponencial. Salve os logs na tabela de logs de jobs.

**Critérios de Aceitação e Verificação:**
- Um disparo da Edge Function para a cidade "Santos, SP" com limite de 1 museu cria com sucesso:
  - O registro em `ponto_turistico` com as coordenadas corretas e o Google `place_id` cadastrado.
  - O registro em `conteudo_midia` com as duas descrições geradas pelo Claude e as duas URLs de áudio funcionais.
  - Os dois arquivos MP3 persistidos no bucket `audios` do storage.
  - O `content_hash` gerado e preenchido.

---

## Prompt para T-038: Implementar pipeline de Auditoria Diária

**Contexto do Projeto:**
As informações úteis dos pontos turísticos (como preços de ingressos e horários) sofrem alterações. O Agente de IA realiza uma auditoria em background periodicamente a cada 24 horas nos pontos que não são verificados há mais de 30 dias.

**Objetivo:**
Codificar a Edge Function de Auditoria Diária (`audit-daily`) que executa buscas na Google Places API e atualiza dados divergentes.

**Especificação Técnica de Referência:**
Consulte a seção "Pipeline 2: Auditoria Diária" em [04_ai_agent_spec.md](file:///c:/Projects/self-guide/refinamento/04_ai_agent_spec.md#L130-L159).

**Instruções de Implementação:**
1. Crie a Edge Function `audit-daily` no Supabase.
2. Na lógica da função:
   - Selecione do banco de dados do Supabase até 100 registros da tabela `contexto` cuja `data_ultima_verificacao` tenha ocorrido há mais de 30 dias, ordenados pela verificação mais antiga.
   - Para cada ponto:
     - Realize uma consulta na Google Places API utilizando o `place_id` registrado no ponto.
     - Compare os campos `horarios_funcionamento`, `custo_ingresso` e o status operacional (aberto/fechado permanentemente) com os dados atualmente salvos.
     - Se houver divergências: atualize os registros correspondentes em `contexto` e recalcule o `content_hash` do `ponto_turistico` para alertar o cache do app.
     - Caso não haja divergências: apenas atualize o timestamp `data_ultima_verificacao` para a data/hora atual.
   - Implemente controle de taxa de requisições (rate limit de 1 requisição por segundo) para respeitar as quotas do Google Places.

**Critérios de Aceitação e Verificação:**
- A função compila e executa sem estourar limites de memória.
- Testando com registros simulados contendo preços desatualizados: ao rodar a Edge Function, as tabelas `contexto` são atualizadas com os dados atuais do Google Maps e o `content_hash` do ponto é alterado.

---

## Prompt para T-039: Implementar pipeline de Auditoria Prioritária

**Contexto do Projeto:**
Caso vários usuários reportem que um local fechou ou mudou de preço, o app incrementa os alertas de erro. Quando o threshold por categoria é atingido, o Agente de IA é acionado imediatamente em prioridade alta para aquele ID, mitigando manutenções manuais.

**Objetivo:**
Escrever a Edge Function `audit-priority` responsável por re-verificar pontos denunciados e disparar notificações de agradecimento aos usuários colaboradores em caso de correções confirmadas.

**Especificação Técnica de Referência:**
Consulte a seção "Pipeline 3: Auditoria Prioritária" em [04_ai_agent_spec.md](file:///c:/Projects/self-guide/refinamento/04_ai_agent_spec.md#L161-L199).

**Instruções de Implementação:**
1. Crie a Edge Function `audit-priority` no Supabase.
2. O gatilho de execução é um evento disparado pelo Supabase Webhook sempre que `controle_erros.contador_alertas` atinge o threshold configurado da categoria correspondente (ex: >= 1 para local fechado, >= 3 para preço ou horário desatualizado).
3. Na execução da função:
   - Busque os dados reais atuais do local denunciado na Google Places API.
   - **Caso a divergência seja confirmada:**
     - Faça as devidas correções no banco de dados.
     - Atualize o `content_hash` de versão do ponto.
     - Zere o `contador_alertas` na tabela `controle_erros`.
     - Marque os registros relacionados de `reportes_usuario` com status `'corrigido'` e `correcao_aplicada = TRUE`.
     - Incremente os pontos de reputação dos usuários denunciantes em `reputacao_usuario`.
     - Dispare uma notificação push via FCM/APNs informando: *"Obrigado! As informações do local [Nome do Ponto] foram atualizadas graças à sua colaboração."*.
   - **Caso a divergência NÃO seja confirmada (falso positivo):**
     - Apenas zere o `contador_alertas`.
     - Marque os reportes com status `'sem_divergencia'`.
     - Não envie nenhuma notificação de push.

**Critérios de Aceitação e Verificação:**
- Simule 3 reportes de horário incorreto para um ponto turístico fictício no Supabase.
- Verifique se o webhook dispara com sucesso a função `audit-priority`.
- Garanta que em caso de alteração válida, o banco seja atualizado, os alertas sejam zerados e as notificações push cheguem ao dispositivo simulado.

---

## Prompt para T-040: Implementar pipeline de Moderação de Sugestões

**Contexto do Projeto:**
Na funcionalidade de crowdsourcing, novos pontos são propostos pelos turistas. Ao atingir 10 votos em comum, a sugestão de local deve ser moderada pela Inteligência Artificial para verificar sua relevância turística antes do enriquecimento de mídia.

**Objetivo:**
Codificar a Edge Function `moderate-suggestions` para avaliar a elegibilidade e relevância turística de sugestões com base em heurísticas estruturadas e julgamento do LLM.

**Especificação Técnica de Referência:**
Consulte "Pipeline 4: Moderação de Sugestões" e "Prompt Base — Avaliação de Relevância Turística" em [04_ai_agent_spec.md](file:///c:/Projects/self-guide/refinamento/04_ai_agent_spec.md#L201-L236) e [04_ai_agent_spec.md#L315-L336].

**Instruções de Implementação:**
1. Crie a Edge Function `moderate-suggestions` no Supabase.
2. A lógica da função deve:
   - Buscar sugestões em `sugestoes_usuario` onde `status = 'aguardando_votos'` e `contador_votos >= 10`.
   - Para cada sugestão qualificada:
     - Obter os dados completos do estabelecimento no Google Places API usando o `place_id` canônico da sugestão.
     - Efetuar a deduplicação de segurança checando se o `place_id` já existe na tabela ativa `ponto_turistico`. Se já existir, marque a sugestão como `'rejeitado'` com motivo `'duplicata'`.
     - Enviar os dados recebidos do Google Places para a API da Anthropic (Claude) com o Prompt Base de Avaliação. O Claude deve julgar se o local é um atrativo turístico relevante (atendendo a critérios como rating >= 3.8, reviews >= 50 e categorias adequadas) e responder estritamente com o formato JSON: `{ "aprovado": boolean, "motivo": string }`.
     - **Se aprovado pela IA:** Insira o local na tabela `ponto_turistico` com `status = 'pendente_moderacao'` (aguardando moderação física final do administrador) e altere a sugestão para `status = 'aprovado'`. Enfileire o ponto no Batch Ingestion para gerar o conteúdo.
     - **Se rejeitado pela IA:** Altere a sugestão para `status = 'rejeitado'` e salve o `motivo_rejeicao` retornado pelo Claude. Notifique o administrador via e-mail ou webhook de comunicação (Slack/Discord/Telegram).

**Critérios de Aceitação e Verificação:**
- A Edge Function executa de maneira autônoma com sucesso no Supabase.
- Ao rodar o pipeline para uma sugestão de um restaurante comum: a IA deve classificá-lo como comercial não-turístico, alterando o status da sugestão para `'rejeitado'` e populando o motivo.
- Ao rodar para uma estátua ou monumento histórico real: o local é aprovado e inserido na tabela `ponto_turistico` para curadoria.

---

## Prompt para T-041: Configurar pg_cron jobs no Supabase

**Contexto do Projeto:**
Os pipelines assíncronos de Auditoria Diária e Moderação de Sugestões operam de forma automática através de acionamento periódico configurado no próprio agendador de tarefas nativo do Supabase (`pg_cron`).

**Objetivo:**
Escrever as regras SQL no banco de dados Supabase para agendar a execução da Edge Function de auditoria de dados e de avaliação de sugestões.

**Especificação Técnica de Referência:**
Consulte a seção "Cron Jobs (via pg_cron)" em [03_database_schema_final.md](file:///c:/Projects/self-guide/refinamento/03_database_schema_final.md#L477-L499).

**Instruções de Implementação:**
1. No SQL Editor do Supabase, configure o agendamento do cron job da auditoria de dados diária:
   - Nome do cron: `job_auditoria_diaria`
   - Periodicidade: `0 2 * * *` (Execução à 02:00 UTC diariamente)
   - Ação: Efetuar uma chamada HTTP POST utilizando a extensão `net` do PostgreSQL para o endpoint da Edge Function `/audit-daily` passando o JWT de autorização do administrador no cabeçalho.
2. Configure o agendamento do cron job para moderação automática de sugestões:
   - Nome do cron: `job_moderar_sugestoes`
   - Periodicidade: `*/30 * * * *` (Execução a cada 30 minutos)
   - Ação: Efetuar uma chamada HTTP POST utilizando a extensão `net` para o endpoint da Edge Function `/moderate-suggestions` com o cabeçalho de autenticação do administrador.
3. Habilite a inicialização automática do daemon `pg_cron` no Supabase nas configurações de parâmetros de inicialização do Postgres.

**Critérios de Aceitação e Verificação:**
- Os cron jobs aparecem cadastrados com sucesso na tabela `cron.job` do Postgres:
  ```sql
  SELECT * FROM cron.job;
  ```
- O log de execução da tabela `cron.job_run_details` indica execuções bem-sucedidas com status `SUCCEEDED`.
