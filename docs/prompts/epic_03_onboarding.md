# Prompts de Desenvolvimento — Épico 3: Onboarding

Este arquivo contém os prompts detalhados para cada tarefa do Épico 3. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-012: Criar as 3 telas de onboarding (conteúdo estático)

**Contexto do Projeto:**
As primeiras impressões contam. A fim de reter o usuário, precisamos de um onboarding moderno que demonstre a proposta de valor do produto de maneira premium e dinâmica, sem depender de requisições de rede.

**Objetivo:**
Construir exatamente 3 telas de onboarding estáticas no módulo compartilhado Compose Multiplatform do KMP.

**Requisitos Relacionados:**
- RF-07 (Exibição exata de 3 telas estáticas)
- RNF-03 (Sem requisições de rede - carregamento local)
- Decisões de Refinamento: Decisão 20 (Fluxo de primeiro acesso)

**Instruções de Implementação:**
1. Crie o componente `OnboardingScreen.kt` em `shared/src/commonMain/kotlin/ui/onboarding/`.
2. Desenvolva as 3 telas em formato de carrossel de páginas (Pager):
   - **Tela 1:** Proposta de valor. Título: "Guia de Bolso Automatizado". Descrição: "Explore a cidade com um guia no seu ouvido. Áudios históricos que disparam automaticamente quando você chega perto."
   - **Tela 2:** Como funciona. Título: "Caminhe e Ouça". Descrição: "Caminhe normalmente. Ao se aproximar de um ponto histórico, você recebe uma notificação com a história daquele lugar."
   - **Tela 3:** Gratuito vs Premium. Título: "Acesso Livre e Premium". Descrição: "Acesse 10 pontos históricos gratuitamente em qualquer cidade. Desbloqueie o acesso total com um único pagamento."
3. Utilize ilustrações estáticas em formato vetorial empacotadas nos assets locais da aplicação (sem requisições HTTP).
4. Implemente transições suaves e indicadores visuais (dots) na parte inferior da tela para indicar o progresso do carrossel.
5. Adicione um botão "Próximo" que avança o carrossel, mudando para "Começar" na última tela.

**Critérios de Aceitação e Verificação:**
- O onboarding carrega instantaneamente no primeiro acesso do app, mesmo sem conexão com a internet.
- As 3 telas contêm exatamente as propostas especificadas de forma legível e esteticamente rica.
- O botão "Começar" na terceira tela direciona o usuário para a próxima etapa (T-013).

---

## Prompt para T-013: Implementar tela de seleção de localização pós-onboarding

**Contexto do Projeto:**
Como o app opera com base no GPS do usuário para disparar as geofences nativas, o fluxo pós-onboarding deve solicitar a permissão de localização do sistema de forma contextualizada ou permitir a seleção de cidade manualmente para planejamento de viagem.

**Objetivo:**
Criar a tela de seleção de localização pós-onboarding com fluxo de solicitação de permissão do S.O. precedido de uma explicação contextual no app mobile.

**Requisitos Relacionados:**
- RF-08 (Duas opções de localização pós-onboarding)
- RF-09 (Explicação contextual antes do prompt de permissão do S.O.)
- Decisões de Refinamento: Decisão 20 (GPS automático vs Busca de cidade manual)

**Instruções de Implementação:**
1. Crie a tela `SelecaoLocalizacaoScreen.kt` em `shared/src/commonMain/kotlin/ui/onboarding/`.
2. Adicione dois botões proeminentes:
   - **"Usar minha localização atual":** 
     - Exibe primeiro um modal informativo/customizado (explicando o uso da localização em background): *"Usamos sua localização para disparar áudios automáticos quando você se aproxima de pontos históricos, mesmo com o app fechado."*
     - Em seguida, aciona a requisição nativa de permissão de geolocalização (`ACCESS_FINE_LOCATION` no Android, `Always Authorization` no iOS via APIs nativas).
   - **"Escolher uma cidade":** 
     - Exibe um campo de entrada de texto com funcionalidade de autocomplete (utilizando a Google Places Autocomplete API).
     - Permite que o usuário digite e selecione uma cidade manualmente.
3. Garanta o tratamento adequado para ambos os fluxos, salvando as coordenadas ou a cidade selecionada nas preferências compartilhadas do app.

**Critérios de Aceitação e Verificação:**
- Ao selecionar "Usar minha localização atual", o modal de explicação contextual é apresentado antes do popup nativo do sistema operacional.
- O popup nativo do S.O. é disparado com sucesso após o fechamento do modal contextual.
- Ao selecionar "Escolher uma cidade", a barra de pesquisa exibe sugestões de cidades válidas através da API do Google.

---

## Prompt para T-014: Implementar Modo Degradado (localização negada)

**Contexto do Projeto:**
Se o turista negar a permissão de localização, não podemos inviabilizar o uso do app. Devemos fornecer uma experiência degradada onde ele consiga pesquisar a cidade manualmente e consumir o conteúdo de áudio e texto através de uma lista estática.

**Objetivo:**
Implementar o fluxo e a interface do Modo Degradado do aplicativo móvel quando a permissão de geolocalização for rejeitada pelo usuário.

**Requisitos Relacionados:**
- RF-10 (Entrada no Modo Degradado com busca manual e sem geofences)
- RF-11 (Banner persistente suave com botão para ativar nas configurações do dispositivo)
- Decisões de Refinamento: Decisão 21 (Navegação manual de pontos sem bloquear o app)

**Instruções de Implementação:**
1. No fluxo de navegação do app, caso a resposta do S.O. à solicitação de permissão de localização seja negativa, configure o estado global da aplicação como `Modo Degradado`.
2. No Modo Degradado:
   - Desative a inicialização de geofences locais no `GeofenceOrchestrator`.
   - Exiba a tela principal do app focada em uma barra de pesquisa manual, permitindo ao usuário buscar por cidade/bairro para carregar a lista de pontos turísticos próximos.
   - Apresente um banner persistente (esteticamente sutil) no topo da tela contendo o texto: *"Ative a localização para receber notificações de áudio automáticas enquanto caminha."* e um botão *"Ativar localização"*.
3. Implemente no botão *"Ativar localização"* o comportamento de abrir as configurações do dispositivo direto na tela de permissões do aplicativo:
   - **Android:** Intent `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` com a URI do pacote.
   - **iOS:** Abrir a URL `UIApplication.openSettingsURLString`.

**Critérios de Aceitação e Verificação:**
- Ao negar a permissão de localização, o aplicativo não trava e apresenta a tela principal em modo de listagem manual.
- O banner persistente é renderizado no topo.
- Clicar no botão do banner redireciona o usuário diretamente para as configurações de permissões do sistema operacional correspondente.

---

## Prompt para T-015: Implementar tela de preferência de download de áudio no onboarding

**Contexto do Projeto:**
Permitir que o usuário defina previamente como os arquivos de áudio pesados (.mp3) serão baixados previne cobranças desnecessárias em pacotes de dados 4G/5G móveis e melhora a experiência de uso.

**Objetivo:**
Criar a tela de configuração inicial para escolha entre o Modo Econômico e o Modo Offline de download no onboarding, salvando o estado de finalização do onboarding localmente.

**Requisitos Relacionados:**
- RF-12 (Configuração de preferência de download de áudio no onboarding)
- RF-13 (Não exibição do onboarding em sessões subsequentes via flag local)
- Decisões de Refinamento: Decisão 1 (Modo Econômico vs Modo Offline apresentado explicitamente)

**Instruções de Implementação:**
1. Crie a tela `PreferenciasDownloadScreen.kt` em `shared/src/commonMain/kotlin/ui/onboarding/`.
2. Renderize dois cards de seleção modernos e explicativos:
   - **Modo Econômico:** *"Consumir sob demanda. Baixa o áudio de cada ponto turístico somente quando você abrir a notificação correspondente. Recomendado para economizar dados móveis."*
   - **Modo Offline:** *"Pré-download antecipado. Baixa previamente o áudio dos 20 pontos ativos da região para que você possa ouvir sem internet na rua. Consome mais armazenamento local."*
3. Salve a opção escolhida no banco local SQLDelight ou nas chaves de preferências locais.
4. Ao clicar no botão de finalizar nesta tela, grave a flag `onboarding_concluido = true` localmente para garantir que o fluxo de onboarding não apareça na próxima abertura do app.

**Critérios de Aceitação e Verificação:**
- A tela de preferências apresenta os dois modos de download de forma clara e visualmente distinguível.
- A seleção do usuário é persistida no banco local com sucesso.
- Ao fechar e reabrir o aplicativo após concluir o onboarding, o app inicia direto na tela principal (Mapa/Lista), ignorando o onboarding.
