# Prompts de Desenvolvimento — Épico 2: Autenticação & Perfil

Este arquivo contém os prompts detalhados para cada tarefa do Épico 2. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-008: Implementar cadastro e login com email/senha

**Contexto do Projeto:**
Estamos usando o Supabase Auth para gerenciar a autenticação dos usuários do app. O JWT gerado servirá para identificar unicamente o turista e autorizar operações baseadas em Row Level Security (RLS) no Supabase.

**Objetivo:**
Implementar o fluxo de cadastro e login usando e-mail e senha no app compartilhado KMP, salvando a sessão ativa localmente de maneira segura.

**Requisitos Relacionados:**
- RF-01 (Cadastro com email/senha)
- RF-04 (Sessão ativa entre reinicializações)
- RF-05 (Associação de histórico e compras)
- RNF-01 (Armazenamento seguro de tokens no Keychain / EncryptedSharedPreferences)

**Instruções de Implementação:**
1. No diretório `shared`, integre o plugin de autenticação do Supabase (`postgrest-kt` e `auth-kt`).
2. Implemente na camada de repositório comum um serviço de autenticação (`AuthRepository`) com os métodos:
   - `signUp(email, password): Result<Unit>`
   - `signIn(email, password): Result<Unit>`
   - `signOut(): Result<Unit>`
   - `getCurrentUser(): User?`
3. Crie uma classe utilitária de expect/actual no módulo shared para o armazenamento seguro do token de autenticação (JWT):
   - **Android (actual):** Usar `EncryptedSharedPreferences` da biblioteca Jetpack Security para persistir os tokens de acesso e refresh.
   - **iOS (actual):** Usar APIs nativas de `Keychain` (Keychain Services) para persistir as credenciais.
4. Ao inicializar o app, o `AuthRepository` deve verificar se existe um token persistido e tentar restaurar a sessão em background usando a função do SDK do Supabase.

**Critérios de Aceitação e Verificação:**
- O usuário consegue se cadastrar informando um e-mail válido e senha, aparecendo na aba "Authentication > Users" do painel do Supabase.
- O usuário consegue efetuar login e deslogar com sucesso.
- Ao fechar o aplicativo e reabri-lo, a sessão deve permanecer ativa (se logado) sem exigir novas credenciais.
- Verifique que o token persistido está criptografado e não acessível via armazenamento simples (SharedPreferences comuns ou NSUserDefaults).

---

## Prompt para T-009: Implementar login social Google

**Contexto do Projeto:**
Para reduzir a fricção na atração de novos usuários, o login social é uma ferramenta essencial no onboarding de aplicativos de turismo móveis.

**Objetivo:**
Habilitar a autenticação com contas Google no Android e iOS, integrando com o Supabase Auth.

**Requisitos Relacionados:**
- RF-02 (Login social Google nas duas plataformas)
- RF-04 (Sessão ativa persistente)
- Decisões de Refinamento: Seção 5.1 (Stack Mobile)

**Instruções de Implementação:**
1. No console de desenvolvedor do Google Cloud (Google Cloud Console), crie as credenciais de OAuth 2.0 (IDs de cliente para Android, iOS e Web).
2. Configure o provedor do Google nas configurações de Autenticação do Supabase adicionando as credenciais (Client ID e Client Secret).
3. No app mobile:
   - **Android:** Integre o SDK do Google Sign-In no módulo `:androidApp` ou use a nova API de `Credential Manager`. Obtenha o `idToken` e passe para a função `signInWith(Google)` do SDK do Supabase no repositório comum.
   - **iOS:** Integre a biblioteca `GoogleSignIn` do iOS. Obtenha o token ID e envie para o endpoint de autenticação do Supabase.
4. Após o login bem-sucedido, certifique-se de salvar a sessão no armazenamento seguro (Keychain / EncryptedSharedPreferences) usando o mesmo fluxo de T-008.

**Critérios de Aceitação e Verificação:**
- A interface de login apresenta o botão "Entrar com Google".
- Ao clicar no botão, o fluxo nativo do Google de seleção de conta é exibido.
- A conta selecionada é cadastrada/autenticada no Supabase e uma nova sessão é iniciada com sucesso.

---

## Prompt para T-010: Implementar login social Apple

**Contexto do Projeto:**
De acordo com as diretrizes de publicação da Apple App Store, qualquer aplicativo iOS que ofereça login social de terceiros (como Google) DEVE obrigatoriamente oferecer o botão de login social nativo da Apple ("Sign in with Apple").

**Objetivo:**
Configurar e implementar a autenticação "Sign in with Apple" para a plataforma iOS, comunicando o token gerado com o Supabase Auth.

**Requisitos Relacionados:**
- RF-03 (Login social Apple obrigatório para iOS)
- RF-04 (Sessão ativa persistente)

**Instruções de Implementação:**
1. Habilite a capability "Sign in with Apple" no Identifiers do Portal Apple Developer para o App ID do projeto.
2. Ative e configure a Apple como Provedor de Autenticação no painel do Supabase Auth.
3. No módulo `:shared` ou `:iosApp` (usando interoperabilidade Swift/Kotlin):
   - Chame a API nativa `ASAuthorizationController` para abrir o fluxo nativo do iOS de login com Apple ID.
   - Capture as credenciais geradas (`ASAuthorizationAppleIDCredential`) e envie o `identityToken` (JWT assinado pela Apple) para o Supabase Auth usando o método `signInWith(Apple)`.
4. Armazene o token de sessão retornado de forma segura no Keychain do iOS.

**Critérios de Aceitação e Verificação:**
- Na plataforma iOS, o botão nativo "Iniciar sessão com a Apple" (conforme design guideline da Apple) é exibido.
- Ao clicar, o fluxo nativo de autenticação facial (Face ID) ou senha da Apple ID é executado.
- O usuário é autenticado com sucesso e criado na tabela de usuários do Supabase.
- Na plataforma Android, este botão deve ser ocultado ou indisponibilizado de acordo com o design system.

---

## Prompt para T-011: Criar tela de perfil do usuário

**Contexto do Projeto:**
A tela de perfil serve para o usuário visualizar seus dados de identificação, suas conquistas (badges acumulados por reportes válidos de erros), o progresso de cidades visitadas e gerenciar suas preferências.

**Objetivo:**
Construir a tela de perfil do usuário usando Compose Multiplatform compartilhada no módulo comum, exibindo dados vindos do banco de dados relacional e a reputação do usuário.

**Requisitos Relacionados:**
- RF-06 (Exibição de nome, badges, total de pontos visitados e cidades desbloqueadas)
- RF-48 (badges baseados em reportes válidos)
- UI/UX Guidelines: Rich Aesthetics (vibrant colors, clean HSL palettes, smooth micro-animations).

**Instruções de Implementação:**
1. Crie o componente UI `PerfilScreen.kt` em `shared/src/commonMain/kotlin/ui/perfil/`.
2. Desenhe uma interface premium (recomenda-se modo escuro moderno, cantos arredondados, gradientes suaves e tipografia harmoniosa).
3. Busque os dados do perfil do Supabase associados ao usuário logado:
   - Nome e E-mail.
   - Total de registros com status = 'consumido' na tabela `historico_usuario`.
   - Cidades desbloqueadas na tabela `compras_usuario` (onde `tipo_compra = 'cidade'`).
   - Badges desbloqueados baseados em `reputacao_usuario.total_reportes_validos` no banco (via RLS):
     - `total_reportes_validos >= 1` → Badge "Explorador Atento"
     - `total_reportes_validos >= 5` → Badge "Guardião da Cidade"
     - `total_reportes_validos >= 20` → Badge "Curador Oficial"
     - Usuário com passe vitalício ativo (`tipo_compra = 'vitalicio'`) → Badge especial "Explorador Vitalício".
4. Adicione micro-animações nas bordas ou ao passar o cursor/clicar nos badges para criar uma experiência premium.

**Critérios de Aceitação e Verificação:**
- A tela de perfil carrega com sucesso exibindo o nome do usuário.
- Se o usuário visitou 3 pontos, a UI deve mostrar o contador "Pontos visitados: 3".
- Os badges correspondentes ao número de reportes corretos do usuário são mostrados com ícones ilustrativos apropriados (não placeholders simples).
