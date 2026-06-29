# Prompts de Desenvolvimento — Épico 11: Painel Administrativo

Este arquivo contém os prompts detalhados para cada tarefa do Épico 11. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-049: Criar Edge Function de Batch Ingestion com autenticação admin

**Contexto do Projeto:**
O pipeline de Batch Ingestion (T-037) é um processo pesado e custoso (consome tokens de IA e requisições da Places API). Não podemos permitir que usuários comuns acionem essa função. Ela deve ser restrita apenas para contas administrativas.

**Objetivo:**
Escrever a camada de segurança e verificação de permissões na Edge Function de ingestão de dados.

**Requisitos Relacionados:**
- RF-65 (Iniciar ingestão de nova cidade via admin)
- RF-66 (Proteção por autenticação separada - role 'admin' no Supabase)

**Instruções de Implementação:**
1. No arquivo de código da Edge Function `batch-ingestion` (ou em um wrapper de middleware de segurança):
   - Extraia o token JWT do cabeçalho `Authorization: Bearer <JWT>` da requisição.
   - Use o cliente do Supabase para verificar a validade do JWT e recuperar o perfil do usuário logado.
   - Verifique nas claims do usuário ou na tabela de usuários se o campo `role` possui o valor `'admin'`.
   - **Caso o usuário NÃO seja administrador:** Retorne imediatamente com código de erro HTTP `403 Forbidden` contendo o JSON: `{ "error": "Acesso não autorizado para esta conta administrativa." }`.
   - **Caso seja administrador:** Permita a execução do fluxo completo de batch ingestion.
2. Retorne o identificador único da execução (`job_id`) para que o administrador possa consultar o progresso do pipeline de carregamento em tempo real.

**Critérios de Aceitação e Verificação:**
- Requisições enviadas ao endpoint sem o token JWT ou usando o token de um turista comum (tier free/premium comum) devem receber o erro de acesso negado (`403 Forbidden`).
- Apenas requisições que enviem um JWT válido contendo a claim `role = 'admin'` no payload são processadas e retornam o status de sucesso do job.

---

## Prompt para T-050: Criar interface web simples de painel admin (MVP)

**Contexto do Projeto:**
O administrador do guia turístico necessita de uma tela central para disparar novos carregamentos de cidades, validar sugestões da comunidade de novos locais e ajustar as cercas de proximidade geográficas sem precisar rodar queries SQL manuais no editor do Supabase.

**Objetivo:**
Construir um Painel Administrativo web minimalista integrado com a API e Edge Functions do Supabase.

**Requisitos Relacionados:**
- RF-65 (Visualizar e resolver sugestões rejeitadas pela IA, criar/editar/desativar pontos manualmente, definir override de raio de gatilho, visualizar logs de jobs com status)
- RF-66 (Acesso restrito a usuários com a role admin no Supabase Auth)
- RF-67 (Confirmação obrigatória para ações destrutivas)
- RNF-17 (Interface web simples usando ferramentas nativas ou Next.js minimalista)

**Instruções de Implementação:**
1. Crie uma aplicação web simples (Next.js, React ou puro HTML/JavaScript) hospedada na Vercel/Netlify ou utilizando o próprio editor estático do Supabase.
2. Implemente o fluxo de login usando o Supabase Auth. Impeça a visualização do painel se a role do usuário logado não for `'admin'`.
3. Desenvolva as seguintes seções na UI:
   - **Ingestão de Cidades:** Formulário com campo de texto para digitar o nome da cidade (ex: *"Salvador, BA"*) e checkboxes para as categorias e idiomas desejados. Botão "Iniciar Batch" dispara o endpoint T-049.
   - **Gerenciamento de Pontos:** Tabela com a lista dos pontos turísticos cadastrados com opção de ativar/desativar, editar texto dos roteiros e alterar o input numérico do `raio_gatilho_metros`.
   - **Sugestões pendentes:** Lista de locais propostos pela comunidade (`sugestoes_usuario`) que foram rejeitados pela IA com a descrição do motivo. Adicione botões de *"Ignorar"* (rejeitar de forma permanente) ou *"Aprovar manualmente"* (override administrativo que insere o local no banco mesmo com parecer negativo da IA).
   - **Logs do Sistema:** Visualização básica da lista de jobs executados pelo Batch Ingestion extraídos de `log_jobs` (mostrando sucesso, falhas e tentativas).
4. Adicione modais de aviso de confirmação obrigatória antes de deletar ou desativar locais no banco de dados.

**Critérios de Aceitação e Verificação:**
- O painel admin web carrega e exige autenticação inicial.
- Um usuário comum que efetua login recebe a tela de acesso negado.
- O administrador acessa a tela e consegue disparar a Edge Function de ingestão e salvar overrides manuais de raios geográficos no banco com confirmação visual na interface.
