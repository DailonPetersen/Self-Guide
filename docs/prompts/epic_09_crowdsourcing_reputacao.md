# Prompts de Desenvolvimento — Épico 9: Crowdsourcing & Reputação

Este arquivo contém os prompts detalhados para cada tarefa do Épico 9. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-042: Implementar fluxo de reporte de dado incorreto

**Contexto do Projeto:**
Desejamos aproveitar a inteligência coletiva dos turistas para manter os dados atualizados sem intervenção manual contínua. Para isso, adicionamos uma opção na UI de detalhes do ponto turístico para denunciar divergências em horários, preços ou fechamento.

**Objetivo:**
Desenvolver o formulário de reporte de dados incorretos e a integração de gravação de denúncias na tabela do banco de dados relacional Supabase.

**Requisitos Relacionados:**
- RF-44 (Botão de reporte no card do ponto)
- RF-45 (Exibição de categorias predefinidas com severidade e limites correspondentes)
- RF-46 (Gatilho automático da auditoria prioritária ao atingir threshold)

**Instruções de Implementação:**
1. No arquivo `PontoDetalheScreen.kt` (T-026), configure o clique do botão "Reportar dado incorreto" para abrir uma folha deslizante inferior (BottomSheet) ou diálogo customizado.
2. No BottomSheet, liste de forma amigável as opções de reportes descritas em `reportes_usuario`:
   - *"Local fechado permanentemente"* (Severidade Crítica - Threshold 1 voto)
   - *"Horário de funcionamento incorreto"* (Severidade Alta - Threshold 3 votos)
   - *"Preço do ingresso desatualizado"* (Severidade Alta - Threshold 3 votos)
   - *"Foto do local incorreta"* (Severidade Baixa - Threshold 3 votos)
   - *"Outro problema"* (Severidade Média - Threshold 3 votos)
3. Adicione um campo opcional de texto livre para observações adicionais.
4. Ao clicar em enviar:
   - Faça uma requisição HTTP POST para a tabela `reportes_usuario` do Supabase registrando o ID do usuário (`usuario_id`), o ID do ponto (`ponto_id`), a categoria escolhida e a descrição.
   - Escreva uma Trigger ou RPC no banco de dados do Supabase que ao detectar um novo registro na tabela de reportes incremente a coluna `contador_alertas` em `controle_erros` para aquele ponto.
   - Caso o `contador_alertas` alcance o threshold específico daquela severidade (ex: >= 1 para fechado, >= 3 para outros), a trigger deve disparar o webhook HTTP para rodar a Edge Function de Auditoria Prioritária (`audit-priority`).

**Critérios de Aceitação e Verificação:**
- O BottomSheet de reporte renderiza com todas as categorias indicadas e o campo de texto livre opcional.
- Ao submeter um reporte, os dados são salvos corretamente na tabela `reportes_usuario` e o `contador_alertas` correspondente da tabela `controle_erros` é incrementado no banco.

---

## Prompt para T-043: Implementar notificação de correção ao usuário que reportou

**Contexto do Projeto:**
Turistas se sentem mais engajados em colaborar com o guia turístico se perceberem que suas denúncias geraram correções reais. Enviamos notificações push específicas confirmando a auditoria concluída.

**Objetivo:**
Codificar o envio seletivo de push de agradecimento aos usuários colaboradores quando o Agente de IA corrige o banco de dados.

**Requisitos Relacionados:**
- RF-47 (Notificação push somente após confirmação e correção real, silêncio se for falso positivo)

**Instruções de Implementação:**
1. No pipeline de Auditoria Prioritária (`audit-priority` em T-039):
   - Ao comprovar a inconsistência relatada e atualizar os dados do local:
     - Realize uma consulta na tabela `reportes_usuario` para obter a lista de `usuario_id` únicos que enviaram reportes recentes para aquele `ponto_id` com status `'pendente'`.
     - Atualize o status desses registros para `'corrigido'` e mude o booleano `correcao_aplicada` para `TRUE`.
     - Busque os tokens FCM ou APNs ativos desses usuários denunciantes.
     - Dispare uma notificação push direcionada: *"Obrigado! A informação de [Nome do Ponto] foi atualizada no guia com base no seu reporte."*.
   - Caso a auditoria verifique que o Google Maps está idêntico ao banco (denúncia inválida/falso positivo):
     - Apenas mude o status dos reportes para `'sem_divergencia'` e `correcao_aplicada = FALSE`.
     - Garanta que **nenhum** push seja enviado neste cenário para evitar spam.

**Critérios de Aceitação e Verificação:**
- Um teste com correção real aprovada pela IA mostra que todos os usuários que registraram denúncias para aquele local recebem a notificação push.
- Se a IA constatar que o reporte do usuário era falso positivo, o status do reporte muda para `'sem_divergencia'` e nenhuma notificação é disparada.

---

## Prompt para T-044: Implementar sistema de reputação e badges

**Contexto do Projeto:**
Como recompensa intangível aos colaboradores e elemento de gamificação do guia, construímos um sistema de reputação. O turista ganha pontos por denúncias corretas e desbloqueia badges visuais.

**Objetivo:**
Escrever as regras SQL e as funções de trigger para cálculo de reputação no Supabase e expor os badges na UI de perfil.

**Requisitos Relacionados:**
- RF-06 (Perfil exibe nome, total de pontos visitados, badges de reputação e cidades compradas)
- RF-48 (badges baseados em reportes válidos: Explorador Atento [1], Guardião da Cidade [5] e Curador Oficial [20])
- Decisões de Refinamento: Decisão 17 (Feedback e badges por reportes válidos)

**Instruções de Implementação:**
1. Crie uma trigger no Postgres do Supabase (`trg_atualizar_reputacao`) que escuta atualizações na tabela `reportes_usuario`.
2. Quando um registro for atualizado para `status = 'corrigido'` e `correcao_aplicada = TRUE`:
   - Incremente a coluna `total_reportes_validos` por 1 na tabela `reputacao_usuario` para o `usuario_id` correspondente.
   - Adicione 10 pontos à coluna `pontos_reputacao` do usuário.
3. No app mobile (camada KMP common):
   - Ao buscar os dados do perfil (T-011), leia os dados de `reputacao_usuario` correspondentes.
   - Mapeie o badge a ser desenhado na UI de acordo com a regra:
     - `total_reportes_validos >= 1` → Badge "Explorador Atento"
     - `total_reportes_validos >= 5` → Badge "Guardião da Cidade"
     - `total_reportes_validos >= 20` → Badge "Curador Oficial"
     - Adicione o badge especial "Explorador Vitalício" caso o usuário possua compra de passe vitalício ativa em `compras_usuario`.

**Critérios de Aceitação e Verificação:**
- Ao simular uma atualização de reporte para corrigido no banco do Supabase, o registro correspondente em `reputacao_usuario` deve incrementar os pontos de reputação e o total de reportes corretos automaticamente.
- A tela de perfil do usuário renderiza os badges corretos de acordo com o total de reportes válidos computados.

---

## Prompt para T-045: Implementar fluxo de sugestão de novos pontos

**Contexto do Projeto:**
Permitir que usuários sugiram locais históricos de interesse turístico ainda não mapeados no app auxilia na expansão de cidades e cria curadoria orgânica.

**Objetivo:**
Implementar o botão de propor novo ponto turístico no mapa com autocomplete geográfico no app móvel.

**Requisitos Relacionados:**
- RF-49 (Botão "Sugerir lugar" no mapa para usuários autenticados)
- RF-50 (Normalizar coordenadas e obter o place_id canônico via Google Places API)
- RF-51 (Exibir mensagem e invalidar se place_id já existir no banco)
- RF-52 (Disparar moderação da IA automaticamente ao atingir 10 votos distintos)
- Decisões de Refinamento: Decisão 9 (10 votos distintos para moderação IA) e Decisão 11 ( place_id canônico para deduplicação)

**Instruções de Implementação:**
1. Na visualização de Mapa (`PontosListScreen` de T-025), exiba um botão flutuante discreto "Sugerir lugar" (visível apenas para usuários autenticados).
2. Ao clicar no botão, abra uma caixa de diálogo ou tela de busca de endereços (integrada ao Google Places Autocomplete API).
3. Quando o usuário selecionar o local sugerido:
   - Obtenha o `place_id` canônico e as coordenadas exatas do Google Places.
   - **Caso o `place_id` já exista na tabela principal `ponto_turistico`:** Exiba um alerta suave: *"Este local já faz parte do SelfGuide!"* e aborte o envio.
   - **Caso o `place_id` já possua uma sugestão pendente em `sugestoes_usuario`:** Registre um voto do usuário atual na tabela `votos_sugestao` e incremente a coluna `contador_votos` na tabela `sugestoes_usuario`.
   - **Caso o `place_id` seja inédito:** Crie um novo registro em `sugestoes_usuario` com `contador_votos = 1` e insira o voto na tabela de relacionamento.
4. Escreva uma trigger no banco que, quando a coluna `contador_votos` em `sugestoes_usuario` alcançar o valor de `10`, altere o status da sugestão para `'em_moderacao'` e envie o webhook que inicia a moderação por IA (`moderate-suggestions`).

**Critérios de Aceitação e Verificação:**
- A interface de sugestão com autocomplete de locais funciona.
- Sugestões para pontos turísticos que já estão no guia são interceptadas de forma clara na UI.
- Ao sugerir um local novo ou votar em um existente, os registros correspondentes em `sugestoes_usuario` e `votos_sugestao` são computados no Supabase.
- Ao bater 10 votos na mesma sugestão, o status muda automaticamente no Supabase para `'em_moderacao'`.
