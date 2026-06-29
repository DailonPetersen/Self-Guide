# Prompts de Desenvolvimento — Épico 1: Fundação & Infraestrutura

Este arquivo contém os prompts detalhados para cada tarefa do Épico 1. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-001: Criar projeto Supabase e configurar extensões

**Contexto do Projeto:**
Estamos desenvolvendo o SelfGuide, um micro-SaaS de guia turístico baseado em geolocalização e áudio guiado por IA. Este projeto utiliza o Supabase como backend serverless e banco de dados PostgreSQL com a extensão PostGIS para buscas geográficas.

**Objetivo:**
Inicializar o projeto no Supabase e habilitar as extensões essenciais que serão utilizadas pelo banco de dados e agendamento de tarefas.

**Requisitos Relacionados:**
- RNF-01 (Segurança de armazenamento de tokens)
- RNF-02 (Não exposição de service_role_key)
- Decisões de Refinamento: Seção 5.2 (Tech Stack Backend - Supabase)

**Instruções de Implementação:**
1. Acesse o console do Supabase e crie um novo projeto chamado `self-guide`.
2. Configure a região como `South America (São Paulo)` (menor latência para o público brasileiro).
3. Habilite as seguintes extensões no SQL Editor:
   - `postgis` (para suporte a dados geográficos e queries de proximidade)
   - `uuid-ossp` (para geração automática de identificadores UUIDv4)
   - `pg_cron` (para agendamento de tarefas do Agente de IA em background)
   - `pg_trgm` (para suporte futuro a fuzzy search nos nomes de locais)
4. Obtenha e armazene de forma segura as seguintes credenciais para uso posterior:
   - `Project URL`
   - `anon_key` (chave pública para uso seguro no app client-side)
   - `service_role_key` (chave administrativa secreta, que NUNCA deve ser exposta no app mobile)

**Critérios de Aceitação e Verificação:**
- O projeto Supabase está criado na região indicada.
- As extensões `postgis`, `uuid-ossp`, `pg_cron` e `pg_trgm` aparecem listadas como habilitadas na aba "Database > Extensions" no dashboard do Supabase ou retornam com sucesso na query:
  ```sql
  SELECT extname FROM pg_extension;
  ```

---

## Prompt para T-002: Executar DDL completo no Supabase (todas as tabelas)

**Contexto do Projeto:**
Necessitamos estruturar o banco de dados relacional para persistir os pontos turísticos, mídias geradas por IA, histórico dos usuários, controle de erros, reportes de crowdsourcing e reputação.

**Objetivo:**
Executar a criação de todas as tabelas, índices espaciais, índices compostos e funções utilitárias/triggers necessárias para o funcionamento do banco no Supabase.

**Especificação Técnica de Referência:**
Consulte o arquivo de modelagem DDL: [03_database_schema_final.md](file:///c:/Projects/self-guide/refinamento/03_database_schema_final.md)

**Instruções de Implementação:**
1. Abra o SQL Editor no painel do Supabase.
2. Crie a função de trigger automático de timestamp que atualiza a coluna `data_atualizacao` nas tabelas:
   ```sql
   CREATE OR REPLACE FUNCTION trigger_atualizar_timestamp()
   RETURNS TRIGGER AS $$
   BEGIN
       NEW.data_atualizacao = NOW();
       RETURN NEW;
   END;
   $$ LANGUAGE plpgsql;
   ```
3. Crie e execute o DDL para as seguintes tabelas na ordem de dependência (pais antes de filhas):
   - `ponto_turistico` (com colunas `geom` [geometry] e `geog` [geography])
   - `conteudo_midia` (relacionada a `ponto_turistico.id` com chave única composta por `ponto_id` e `idioma`)
   - `contexto` (informações de horários, custos, categorias e fonte)
   - `controle_erros` (contador de alertas para auditoria)
   - `reportes_usuario` (reportes individuais de usuários sobre erros de pontos)
   - `reputacao_usuario` (pontuação e total de reportes válidos dos usuários)
   - `sugestoes_usuario` (sugestões crowdsourced baseadas em Google `place_id`)
   - `votos_sugestao` (votos em sugestões de novos pontos)
   - `historico_usuario` (registro de pontos consumidos)
   - `compras_usuario` (registro de microtransações e passes vitalícios)
4. Crie todos os índices de performance, incluindo:
   - Índice espacial GIST em `ponto_turistico(geom)`
   - Índice espacial GIST em `ponto_turistico(geog)`
   - Índices de filtro convencionais (`ativo`, `status`, `place_id`, `categoria_principal`)
   - Índices compostos e GIN (ex: `idx_conteudo_midia_ponto_idioma`, `idx_contexto_categorias` usando GIN)
5. Crie as views auxiliares:
   - `vw_ponto_completo` (agregação de ponto, conteúdo e contexto de pontos ativos)
   - `vw_usuario_acesso` (determina o nível de acesso/tier do usuário: 'vitalicio' ou 'cidade_especifica')

**Critérios de Aceitação e Verificação:**
- Todas as tabelas e views acima foram criadas sem erros no banco de dados.
- Execute a query a seguir e verifique se todas as tabelas listadas estão presentes:
  ```sql
  SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
  ```
- Verifique a presença dos índices espaciais listando-os através do dashboard do Supabase ou da query correspondente do sistema de catálogos do Postgres.

---

## Prompt para T-003: Configurar Row Level Security (RLS)

**Contexto do Projeto:**
Como o app mobile e possíveis web clientes consomem a API gerada pelo Supabase diretamente, precisamos garantir que as políticas de acesso a nível de linha (RLS) impeçam que usuários modifiquem dados alheios ou leiam informações sensíveis.

**Objetivo:**
Habilitar RLS nas tabelas pertinentes e escrever as políticas (Policies) que permitam a leitura pública de dados dos pontos turísticos e restrinjam a leitura/escrita de histórico, reportes e compras apenas ao respectivo dono do registro.

**Especificação Técnica de Referência:**
Consulte a seção "Configurar Row Level Security (RLS)" em [05_supabase_guide.md](file:///c:/Projects/self-guide/refinamento/05_supabase_guide.md).

**Instruções de Implementação:**
1. Habilite o RLS nas tabelas que armazenam dados de usuários:
   - `historico_usuario`
   - `compras_usuario`
   - `reputacao_usuario`
   - `reportes_usuario`
   - `votos_sugestao`
2. Crie as seguintes políticas de segurança (Policies):
   - **historico_usuario**: Usuário autenticado só pode ler/escrever registros onde `auth.uid() = usuario_id`.
   - **compras_usuario**: Usuário autenticado só pode ler seus próprios registros (`auth.uid() = usuario_id`).
   - **reputacao_usuario**: Usuário autenticado só pode ler seus próprios registros (`auth.uid() = usuario_id`).
   - **reportes_usuario**: Usuário autenticado só pode ver ou criar seus próprios reportes (`auth.uid() = usuario_id`).
   - **votos_sugestao**: Usuário autenticado pode ler e criar seus próprios votos (`auth.uid() = usuario_id`).
3. Crie políticas de leitura pública para dados que devem ser disponibilizados aos turistas:
   - **ponto_turistico**: Permite `SELECT` público ou para usuários autenticados onde `ativo = TRUE` e `status = 'ativo'`.
   - **conteudo_midia**: Permite `SELECT` público ou para usuários autenticados.
   - **contexto**: Permite `SELECT` público ou para usuários autenticados.

**Critérios de Aceitação e Verificação:**
- O RLS está habilitado para todas as tabelas indicadas.
- Teste executando consultas usando a `anon_key` fingindo ser um usuário autenticado e garanta que:
  - Não é possível ler registros de `compras_usuario` de outros IDs.
  - É possível realizar `SELECT` em `ponto_turistico` sem restrição de ID de usuário.

---

## Prompt para T-004: Configurar Supabase Storage (buckets audios/ e imagens/)

**Contexto do Projeto:**
Os roteiros reescritos pela IA são sintetizados em arquivos de áudio MP3 de alta fidelidade e salvos no servidor de storage compatível com AWS S3. Além disso, as imagens oficiais extraídas das APIs geográficas devem ser armazenadas de forma estática.

**Objetivo:**
Criar os buckets públicos de storage no Supabase para áudios e imagens, e configurar as políticas de acesso de leitura para qualquer usuário e de escrita restrita apenas à role administrativa (`service_role`).

**Especificação Técnica de Referência:**
Consulte a seção "Configurar Storage" em [05_supabase_guide.md](file:///c:/Projects/self-guide/refinamento/05_supabase_guide.md).

**Instruções de Implementação:**
1. No painel do Supabase, vá em "Storage" e crie dois buckets:
   - `audios` (Marque como "Public")
   - `imagens` (Marque como "Public")
2. Defina as políticas de acesso para os buckets:
   - Permita leitura pública (`SELECT`) para qualquer usuário anônimo ou autenticado.
   - Permita escrita (`INSERT`, `UPDATE`, `DELETE`) apenas se o usuário tiver a role administrativa do sistema (`auth.role() = 'service_role'`). Isso garante que somente o Agente de IA executando no backend possa fazer o upload dos MP3s gerados e imagens.
3. A estrutura física recomendada de armazenamento é:
   - `/audios/{ponto_id}/{idioma}/{versao}.mp3` (onde versao é 'rapida' ou 'completa')
   - `/imagens/{ponto_id}/thumb.jpg`

**Critérios de Aceitação e Verificação:**
- Os buckets `audios` e `imagens` aparecem como públicos na interface do Supabase.
- Tentativas de upload de arquivos sem a chave `service_role` (ex: usando apenas a `anon_key`) devem retornar erro de permissão negada.
- Um upload feito com a `service_role_key` é concluído com sucesso e o arquivo fica acessível via URL pública no formato: `https://[PROJECT_ID].supabase.co/storage/v1/object/public/audios/[caminho_do_arquivo]`.

---

## Prompt para T-005: Criar função SQL `buscar_pontos_proximos` (RPC)

**Contexto do Projeto:**
Para alimentar a interface com os 20 pontos ativos do geofencing e guardar 5 adicionais em cache local em caso de perda de sinal, necessitamos de uma busca espacial otimizada com ordenação de proximidade e filtros baseados no histórico de consumo do usuário.

**Objetivo:**
Escrever uma função PostgreSQL armazenada (RPC) que calcula a distância do turista em relação aos pontos cadastrados, exclui os locais já marcados como consumidos e retorna os 25 pontos mais próximos com rankeamento e marcação de conteúdo premium (pontos 11 a 20).

**Especificação Técnica de Referência:**
Consulte o algoritmo e a query SQL final otimizada em [03_database_schema_final.md](file:///c:/Projects/self-guide/refinamento/03_database_schema_final.md#L413-L473) ou no blueprint de arquitetura.

**Instruções de Implementação:**
1. No SQL Editor do Supabase, crie a função `buscar_pontos_proximos` que recebe os parâmetros:
   - `p_usuario_id` (UUID)
   - `p_latitude` (DECIMAL / NUMERIC)
   - `p_longitude` (DECIMAL / NUMERIC)
   - `p_idioma` (VARCHAR, padrão 'pt-BR')
2. A lógica da query na função deve:
   - Usar um CTE (`pontos_consumidos`) para listar os IDs de pontos já marcados como 'consumido' pelo `p_usuario_id` em `historico_usuario`.
   - Executar busca espacial utilizando `ST_Distance` com projeção métrica (SRID 3857) para cálculo rápido de distância, aproveitando o índice GIST espacial.
   - Filtrar pontos ativos (`ativo = TRUE` e `status = 'ativo'`) que não estejam no CTE de consumidos.
   - Limitar o resultado em 25 registros para incluir os 20 pontos georreferenciados imediatos e os 5 substitutos offline.
   - Adicionar o ranking baseado na proximidade utilizando `ROW_NUMBER() OVER (ORDER BY distancia_metros ASC)`.
   - Adicionar o booleano `is_premium` que é `FALSE` para os ranks de 1 a 10 e `TRUE` para ranks de 11 a 25.
   - Adicionar o booleano `is_ativo` que é `TRUE` para ranks de 1 a 20 e `FALSE` para ranks de 21 a 25 (cache de substitutos).
3. A assinatura de retorno da função deve corresponder ao schema de colunas esperado pelo app (coordenadas, URLs de áudio rápidas e completas, descrição, horários, custo, etc.).

**Critérios de Aceitação e Verificação:**
- A função RPC foi criada no banco de dados com sucesso.
- Execute o teste chamando a função com coordenadas fictícias e valide o retorno das colunas calculadas:
  ```sql
  SELECT * FROM buscar_pontos_proximos('usuario-uuid-valido', -23.55052, -46.633308, 'pt-BR');
  ```
- Garanta que as colunas `rank` (1 a 25), `is_premium` (False até o 10, True do 11 em diante) e `is_ativo` (True até o 20, False para os 5 finais) sejam calculadas corretamente.

---

## Prompt para T-006: Criar projeto KMP (Kotlin Multiplatform) com estrutura de módulos

**Contexto do Projeto:**
Buscamos agilidade no desenvolvimento mobile compartilhando a lógica de negócios, banco local, repositórios e ViewModels entre Android e iOS, mantendo a flexibilidade de lidar com APIs nativas de localização quando necessário.

**Objetivo:**
Configurar o esqueleto de um projeto Kotlin Multiplatform Mobile (KMP) com suporte a Compose Multiplatform e organizar a árvore de módulos e dependências chave especificadas no blueprint.

**Especificação Técnica de Referência:**
Consulte a seção 5.1 (Stack Mobile - Kotlin Multiplatform) de [planejamento_micro_saas_tour_guiado.md](file:///c:/Projects/self-guide/planejamento_micro_saas_tour_guiado.md#L111-L140).

**Instruções de Implementação:**
1. Crie a estrutura de diretórios para o projeto KMP na raiz do repositório:
   - `/shared` (Módulo de código Kotlin comum, incluindo `/src/commonMain`, `/src/androidMain` e `/src/iosMain`)
   - `/androidApp` (App Android nativo que consome o shared)
   - `/iosApp` (App iOS nativo em Swift/Xcode que importa o framework shared)
2. Configure o arquivo `build.gradle.kts` do módulo shared adicionando as seguintes bibliotecas multiplatform:
   - Kotlinx Coroutines (`kotlinx-coroutines-core`)
   - Kotlinx Serialization (`kotlinx-serialization-json`)
   - Compose Multiplatform (Material 3) para UI declarativa compartilhada
   - Koin (Injeção de dependência compartilhada)
   - SQLDelight (Banco de dados local em comum)
   - Ktor Client (HttpClient compartilhado com engines `android` e `darwin` para iOS)
3. Configure o SDK do SQLDelight indicando a geração de banco e o arquivo de queries `.sq` inicial no caminho `shared/src/commonMain/sqldelight`.
4. Garanta que o projeto compile sem erros para as plataformas Android e iOS (gerando o framework Objective-C/Swift no iOS).

**Critérios de Aceitação e Verificação:**
- O projeto compila com sucesso no Gradle executando `./gradlew assemble`.
- A estrutura de diretórios do projeto reflete a separação entre `shared`, `androidApp` e `iosApp`.
- As dependências fundamentais (SQLDelight, Ktor, Koin, Coroutines) estão declaradas no `shared/build.gradle.kts` e disponíveis para importação no `commonMain`.

---

## Prompt para T-007: Configurar variáveis de ambiente e secrets

**Contexto do Projeto:**
As chaves de acesso a APIs comerciais (Supabase, Google Places, OpenWeather, etc.) e as secrets administrativas variam entre o ambiente de desenvolvimento local e produção, exigindo gestão cuidadosa para evitar vazamento em repositórios de controle de versão.

**Objetivo:**
Estabelecer um fluxo de carregamento de variáveis de ambiente no frontend (App Client) e backend (Edge Functions) que isole dados sensíveis e impeça a compilação de chaves administrativas (`service_role_key`) no binário do app móvel.

**Especificação Técnica de Referência:**
Consulte a seção "Variáveis de Ambiente Necessárias" em [05_supabase_guide.md](file:///c:/Projects/self-guide/refinamento/05_supabase_guide.md#L295-L315).

**Instruções de Implementação:**
1. Crie um arquivo `.env.example` na raiz do projeto contendo as chaves necessárias (sem valores reais):
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
   - `SUPABASE_SERVICE_ROLE_KEY` (marcado como admin-only)
   - `GOOGLE_PLACES_API_KEY`
   - `GOOGLE_TTS_API_KEY`
   - `ANTHROPIC_API_KEY`
2. No app móvel (KMP):
   - Configure o build Gradle para expor as constantes `SUPABASE_URL` e `SUPABASE_ANON_KEY` via `BuildConfig` gerado.
   - Certifique-se de que a `SUPABASE_SERVICE_ROLE_KEY` **nunca** seja injetada no Gradle do app.
3. No backend (Supabase Edge Functions):
   - Configure o arquivo `.env` local para testes da CLI do Supabase.
   - Adicione regras no `.gitignore` para ignorar o arquivo `.env` real nas pastas de funções e no projeto root.

**Critérios de Aceitação e Verificação:**
- Existe um arquivo `.env.example` descrevendo as chaves necessárias.
- A chave `service_role_key` não é referenciada em nenhum arquivo de código-fonte no diretório do app móvel.
- Um teste de compilação do app mostra que `BuildConfig.SUPABASE_ANON_KEY` e `BuildConfig.SUPABASE_URL` são carregados corretamente a partir de variáveis de sistema ou arquivos locais ignorados pelo git (ex: `local.properties`).
