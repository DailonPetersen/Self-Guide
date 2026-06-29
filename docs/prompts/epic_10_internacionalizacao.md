# Prompts de Desenvolvimento — Épico 10: Internacionalização

Este arquivo contém os prompts detalhados para cada tarefa do Épico 10. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-046: Configurar sistema i18n com strings para pt-BR

**Contexto do Projeto:**
Como pretendemos receber turistas estrangeiros em diversas capitais brasileiras, a base do app móvel deve ser internacionalizada desde o MVP. Nenhuma string da interface visual deve ser mantida de forma fixa (hardcoded) nos arquivos do Compose Multiplatform.

**Objetivo:**
Integrar o sistema de internacionalização (i18n) usando a biblioteca `Lyricist` ou similar no módulo compartilhado KMP, configurando a base de dados de tradução no idioma nativo do projeto (Português - `pt-BR`).

**Requisitos Relacionados:**
- RF-61 (Interface traduzida via i18n seguindo o idioma do dispositivo automaticamente)
- Decisões de Refinamento: Decisão 19 (Preparação de idiomas via i18n)

**Instruções de Implementação:**
1. No arquivo `shared/build.gradle.kts`, adicione a dependência da biblioteca de localização multiplatform `Lyricist` (ou solução nativa de recursos do Jetpack Compose).
2. Crie uma estrutura para definição das chaves e strings locais. A pasta recomendada é `shared/src/commonMain/kotlin/strings/`.
3. Crie a interface `SelfGuideStrings` mapeando todas as frases do aplicativo, contendo chaves como:
   - `appName`, `onboardingWelcomeTitle`, `onboardingWelcomeDesc`, `onboardingHowItWorksTitle`, `onboardingHowItWorksDesc`, `onboardingPremiumTitle`, `onboardingPremiumDesc`, `gpsPermissionDialogText`, `listTabName`, `mapTabName`, `paywallUnlockTitle`, `markAsConsumedButton`, `reportIssueButton`, `settingsStorageCache`.
4. Crie o arquivo de implementação concreta `PtSelfGuideStrings.kt` contendo todos os valores em português brasileiro (`pt-BR`).
5. Configure a detecção e o carregamento do locale do sistema operacional de forma que o app selecione o dicionário português automaticamente ao detectar o locale do celular.

**Critérios de Aceitação e Verificação:**
- O projeto compila com o Lyricist integrado.
- Todas as telas desenvolvidas (Onboarding, Lista, Detalhe, Perfil) utilizam chaves do dicionário `SelfGuideStrings` (ex: `Strings.onboardingWelcomeTitle`) e não possuem strings hardcoded.
- Quando o celular do teste está configurado para português do Brasil (`pt-BR`), todos os elementos visuais são exibidos corretamente.

---

## Prompt para T-047: Adicionar strings en-US ao sistema i18n

**Contexto do Projeto:**
O idioma de fallback obrigatório para usuários de dispositivos cuja língua local não seja suportada pelo app (ex: turistas franceses, italianos, alemães) é o Inglês Americano (`en-US`).

**Objetivo:**
Escrever o arquivo de tradução para inglês no app mobile e definir a lógica de fallback de idioma automática.

**Requisitos Relacionados:**
- RF-62 (Fallback de idioma para en-US em idiomas não suportados)
- Decisões de Refinamento: Decisão 19 (Fallback para en-US se locale não for suportado)
- RNF-16 (Formatos de moedas e datas baseados no locale)

**Instruções de Implementação:**
1. Crie o arquivo de strings `EnSelfGuideStrings.kt` no diretório de traduções `shared/src/commonMain/kotlin/strings/`.
2. Implemente a mesma interface `SelfGuideStrings` e realize a tradução de todas as frases do português brasileiro para inglês (`en-US`).
3. No orquestrador de inicialização de i18n, insira a lógica de fallback: se o locale retornado pelo sistema operacional do dispositivo for diferente de `pt-BR` e não corresponder a outra língua suportada, carregue por padrão o dicionário `EnSelfGuideStrings`.
4. Certifique-se de que os formatadores de dados (moedas e data/hora) utilizem classes locais expect/actual de Locale para formatar valores no padrão do turista (ex: exibir `$` e formato de data `MM/DD/YYYY` para en-US e `R$` e `DD/MM/YYYY` para pt-BR).

**Critérios de Aceitação e Verificação:**
- O projeto compila com os dois idiomas configurados.
- Ao rodar o app no simulador com o idioma do sistema alterado para "Inglês (Estados Unidos)" ou "Francês", a interface visual do app é traduzida e exibida por completo em Inglês.

---

## Prompt para T-048: Adicionar suporte a novo idioma no pipeline do agente (configurável)

**Contexto do Projeto:**
No servidor, a tabela `conteudo_midia` foi projetada para aceitar N idiomas para o mesmo ponto turístico. Adicionar suporte a um novo idioma exige que o Agente de IA consiga ler um parâmetro configurável e rodar o pipeline para a nova língua, sem mexer no schema SQL.

**Objetivo:**
Configurar o pipeline do Agente de IA para aceitar um idioma de destino flexível, gerando roteiros e áudios de acordo com o solicitado.

**Requisitos Relacionados:**
- RF-63 (Conteúdo gerado de forma independente por idioma)
- RF-64 (Adicionar novo idioma requer apenas rodar batch com novo parâmetro, sem alteração de schema ou código)
- Decisões de Refinamento: Decisão 18 (Arquitetura de conteúdo multi-idioma pronta para expansão)

**Instruções de Implementação:**
1. Na Edge Function `batch-ingestion` (T-037), parametrize as chamadas de geração de texto (Claude API) e áudio (TTSProvider) para receberem a variável `idioma` (ex: `'en-US'` ou `'es-ES'`) como parâmetro de entrada na chamada HTTP POST.
2. Na geração do roteiro (Claude): injete a variável de idioma no Prompt do LLM (*"Idioma de saída: [IDIOMA]"*).
3. Na geração de áudio (TTSProvider): passe a string de idioma para a factory buscar a voz neural adequada correspondente no Google Cloud TTS (conforme tabela de vozes de T-036).
4. Ao salvar no banco, crie o registro de inserção na tabela `conteudo_midia` definindo o campo `idioma` correspondente (ex: `'en-US'`). A unicidade do índice composto `(ponto_id, idioma)` garantirá que novos idiomas entrem como linhas extras, sem duplicar dados.

**Critérios de Aceitação e Verificação:**
- A Edge Function de Ingestão aceita chamadas com o corpo `{"cidades": ["Rio de Janeiro"], "idiomas": ["en-US"]}`.
- O Claude gera a descrição e o storytelling no idioma solicitado (inglês).
- O arquivo MP3 de áudio correspondente é sintetizado usando a voz adequada do Google TTS configurada para inglês e gravado no bucket com a flag de idioma correta.
