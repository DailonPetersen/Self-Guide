# Prompts de Desenvolvimento — Épico 7: Monetização & Paywall

Este arquivo contém os prompts detalhados para cada tarefa do Épico 7. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-032: Integrar AdMob (banner + nativo)

**Contexto do Projeto:**
A estratégia de monetização do SelfGuide é baseada em freemium. Usuários que não realizaram compras devem visualizar banners e anúncios nativos estilizados ao longo do app para compensar os custos operacionais básicos.

**Objetivo:**
Integrar o SDK do Google AdMob no app KMP e criar os componentes de anúncio para serem renderizados condicionalmente na tela de lista de pontos turísticos.

**Requisitos Relacionados:**
- RF-29 (Exibição de banner fixo no rodapé e cards nativos na lista, sem intersticiais)
- Decisões de Refinamento: Decisão 5 (Sem intersticiais - contexto de turismo exige experiência não-bloqueante)

**Instruções de Implementação:**
1. Adicione a dependência do SDK do Google Mobile Ads nas configurações do Gradle do Android e do CocoaPods no iOS.
2. Inicialize o SDK em ambas as plataformas na inicialização do app.
3. Crie representações expect/actual para os componentes do AdMob no Compose Multiplatform:
   - `AdmobBannerView`: Componente de UI que renderiza um banner adaptativo fixo (Adaptive Banner) no rodapé.
   - `AdmobNativeAdCard`: Componente de UI que renderiza um anúncio nativo integrado com o visual dos demais cards da lista de pontos turísticos (incluindo imagem, título, corpo de texto curto e botão call-to-action). Marque-o claramente com uma tag discreta "Patrocinado".
4. Adicione regras condicionais: Estes anúncios só devem ser inflados e exibidos se o estado de acesso do usuário for `free` (nunca exibir para usuários pagantes de cidades ou vitalícios).

**Critérios de Aceitação e Verificação:**
- O app compila com o SDK do AdMob.
- Em ambiente de testes (usando Ad Units IDs de teste do Google AdMob):
  - Um banner de teste é exibido no rodapé da lista de pontos turísticos para um usuário gratuito.
  - Anúncios nativos de teste aparecem intercalados na lista vertical de pontos turísticos.
  - Para um usuário com passe de cidade ou vitalício ativo, os anúncios somem por completo da interface do app.

---

## Prompt para T-033: Implementar modal de paywall

**Contexto do Projeto:**
Quando o turista tenta acessar os pontos premium ou quando ele visita uma cidade nova e tem direito apenas aos 10 pontos gratuitos iniciais, o app deve disparar um modal de paywall premium, incentivando a conversão de forma persuasiva.

**Objetivo:**
Criar a interface e a lógica de apresentação dinâmica do modal de paywall do app com base no perfil de compras prévias do usuário.

**Requisitos Relacionados:**
- RF-30 (Modal de paywall ao acessar pontos 11-20 para usuários gratuitos)
- RF-32 (Apresentar passe vitalício com desconto proporcional na segunda compra)
- RF-33 (Banner sutil em nova cidade para usuários com pagamentos anteriores)
- Decisões de Refinamento: Decisão 4 (Desconto no passe vitalício na segunda compra) e Decisão 6 (Sem anúncios e banner sutil para quem já comprou)

**Instruções de Implementação:**
1. Crie o componente `PaywallModal.kt` no diretório `shared/src/commonMain/kotlin/ui/monetizacao/`.
2. Desenvolva o layout do modal com design premium (gradientes vibrantes, ícones modernos de benefícios e textos objetivos):
   - **Cenário A: Usuário Novo (nunca comprou nada):** Exiba a compra da cidade atual por um preço baixo (ex: R$ 9,90) e o passe vitalício (ex: R$ 39,90).
   - **Cenário B: Usuário com 1 compra prévia:** Ao tentar desbloquear a segunda cidade, exiba um modal especial promovendo o "Passe Vitalício" com um desconto proporcional ao valor já pago (ex: Passe Vitalício de R$ 39,90 por R$ 29,90).
   - **Cenário C: Usuário com 2+ compras prévidas:** Exiba uma oferta ainda mais agressiva de upgrade vitalício.
3. Se o usuário já comprou pelo menos uma cidade e entra em outra cidade nova, não exiba anúncios ou paywalls intrusivos. Em vez disso, mostre apenas um banner discreto no topo da lista: *"Desbloqueie [Cidade] ou acesse todas as cidades com o passe vitalício → [Desbloquear]"*.

**Critérios de Aceitação e Verificação:**
- O modal de paywall abre imediatamente ao clicar em qualquer ponto numerado de 11 a 20 quando o usuário for gratuito.
- O layout do modal adapta as ofertas e preços corretamente com base no histórico de compras que vem de `compras_usuario`.
- Para usuários com pelo menos 1 compra em outra cidade, os anúncios somem da nova cidade e o banner sutil é exibido no topo.

---

## Prompt para T-034: Implementar fluxo de compra por cidade (PIX + cartão)

**Contexto do Projeto:**
Para processar os pagamentos e liberar o acesso, o app deve integrar os sistemas de faturamento oficiais das lojas (Google Play Billing no Android e Apple In-App Purchase no iOS) e fornecer suporte a PIX no Brasil usando um gateway de pagamentos externo.

**Objetivo:**
Codificar o fluxo completo de compra e checkout de cidade única e do passe vitalício no aplicativo móvel.

**Requisitos Relacionados:**
- RF-31 (Suporte a pagamento por cidade - PIX e cartão)
- RF-35 (Registrar transações na tabela compras_usuario com status da compra)
- RNF-09 (Seguir diretrizes de In-App Purchase para iOS, Google Play Billing para Android e gateways web para PIX)
- RNF-10 (Tratamento claro de mensagens de erro e falhas)

**Instruções de Implementação:**
1. Configure as dependências de faturamento:
   - **Android:** Integrar biblioteca Play Billing Library do Google.
   - **iOS:** Integrar o framework `StoreKit` da Apple.
2. Crie uma abstração expect/actual no módulo shared para gerenciar compras nativas (`BillingService`):
   - `fun iniciarCompraInApp(productId: String, onResultado: (sucesso: Boolean, erro: String?) -> Unit)`
3. Crie um endpoint/Edge Function no Supabase para processamento de pagamentos via gateway de terceiros (ex: Stripe ou Mercado Pago) para compras com **PIX**:
   - O endpoint deve gerar o QR Code de pagamento e a chave copia-e-cola do PIX.
   - Implemente um webhook que recebe a confirmação de pagamento do gateway externo e insere um registro na tabela `compras_usuario` com `status_pagamento = 'confirmado'`.
4. No app, ao receber a confirmação de sucesso do StoreKit, Google Play Billing ou webhook do PIX:
   - Apresente um modal premium de comemoração / sucesso ("Cidade Desbloqueada!").
   - Atualize a UI para desbloquear os pontos imediatamente.

**Critérios de Aceitação e Verificação:**
- O checkout nativo abre com sucesso no Android e iOS ao clicar para comprar no modal de paywall.
- Ao efetuar o pagamento simulado (sandbox/testes), o registro correspondente é inserido na tabela `compras_usuario` no Supabase com o valor, método (pix/cartao/IAP) e status corretos.
- Mensagens amigáveis de erro são apresentadas se o usuário cancelar o fluxo de compra ou se o cartão for recusado.

---

## Prompt para T-035: Implementar lógica de acesso por tier (free / cidade / vitalício)

**Contexto do Projeto:**
Não podemos confiar apenas em validações no lado do cliente (app mobile) para conceder acesso às mídias exclusivas. A lógica de permissão deve ser forçada nas políticas RLS do Supabase Storage e do PostGIS, e cacheada localmente para economizar bateria e rede.

**Objetivo:**
Escrever as regras de validação de acesso baseadas nos tiers do usuário no banco de dados e no app mobile.

**Requisitos Relacionados:**
- RF-29 (10 primeiros desbloqueados por padrão para gratuitos)
- RF-33 (Desbloqueio de cidade comprada)
- RF-34 (Acesso irrestrito total para passe vitalício)
- RF-36 (Validação server-side via RLS do Supabase - vital)

**Instruções de Implementação:**
1. No Supabase, crie uma função SQL customizada que checa o tier do usuário:
   - Uma query em `compras_usuario` que verifica se existe algum registro com `tipo_compra = 'vitalicio'` e `status_pagamento = 'confirmado'` para o `usuario_id` logado.
   - Caso não exista vitalício, verifica se há registro com `tipo_compra = 'cidade'`, `status_pagamento = 'confirmado'` e `cidade_id` correspondente à cidade buscada pelo usuário.
2. Integre essa função nas políticas de RLS das tabelas de dados (`conteudo_midia`, `contexto` e do Supabase Storage bucket `audios`).
   - Apenas o roteiro resumido e informações básicas de ranks 1 a 10 podem ser lidos se o usuário não possuir passe daquela cidade ou vitalício.
3. No app mobile:
   - Armazene o tier do usuário nas preferências locais.
   - Adicione um TTL de 5 minutos ao cache local do tier para evitar requisições repetidas ao Supabase durante o deslocamento na rua.

**Critérios de Aceitação e Verificação:**
- Tentativas de ler o conteúdo da tabela `conteudo_midia` ou baixar arquivos MP3 correspondentes aos ranks 11 a 20 usando um token de usuário gratuito (via Postgrest API direta) devem retornar erro de acesso negado pelo Supabase RLS.
- O cache local do tier de acesso expira e se revalida a cada 5 minutos em background.
