# Especificação de Comportamento do Sistema — Tour Guiado Automatizado

Este documento descreve o comportamento esperado do sistema em cada cenário relevante. É o documento de referência para desenvolvimento, QA e prompts de IA.

---

## 1. Ciclo de Vida do Geofencing

### 1.1 Inicialização dos Top 20

**Gatilho:** Usuário abre o app pela primeira vez, seleciona uma cidade manualmente, ou a macro-cerca detecta deslocamento > 1km.

**Fluxo:**
1. App obtém coordenadas atuais (GPS ou entrada manual de cidade)
2. Chama Edge Function `buscar_pontos_proximos(usuario_id, lat, lng, idioma)`
3. Query retorna até 20 pontos não consumidos ordenados por distância
4. `GeofenceOrchestrator` registra as 20 geofences no S.O.
5. App pré-carrega metadados dos próximos 5 pontos além dos 20 (cache de substituição offline)
6. Pontos 1-10: desbloqueados na UI
7. Pontos 11-20: bloqueados na UI com ícone de cadeado (gatilho de paywall)

### 1.2 Trigger de Geofence (Usuário se aproxima de um ponto)

**Gatilho:** Usuário permanece dentro do raio do ponto por 30 segundos contínuos (dwell time).

**Fluxo com tela bloqueada:**
1. S.O. acorda o app em background
2. App dispara notificação push rica: título do ponto + subtítulo curto
3. Modo Econômico: aguarda usuário abrir a notificação para iniciar download do MP3
4. Modo Offline: MP3 já estava baixado — notificação inclui ação de play direto

**Fluxo ao abrir a notificação:**
1. App abre o card do ponto turistico
2. Carrega instantaneamente: texto + imagem (dados leves, já em cache)
3. Inicia download do MP3 em background se ainda não disponível
4. Reproduz áudio assim que download completo (ou imediato no Modo Offline)
5. Exibe botão "Ouvir versão [Completa/Rápida]" para solicitar a versão alternativa sob demanda

**Proteção contra falso trigger:**
- Timer de 30s é cancelado se o usuário sair do raio antes do tempo
- Nenhuma notificação é disparada para passagens rápidas (carro, ônibus)

### 1.3 Marcação de Consumo

**Gatilho:** Usuário clica em "Já consumi este ponto".

**Com conexão:**
1. `GeofenceOrchestrator` remove a geofence do ponto do S.O.
2. Atualiza `historico_usuario` no Supabase (status = 'consumido')
3. Busca próximo ponto da cauda longa via API
4. Registra nova geofence no slot vago
5. Atualiza cache local SQLDelight

**Sem conexão:**
1. `GeofenceOrchestrator` remove a geofence do S.O.
2. Marca como consumido no SQLDelight local (flag `pendente_sync = true`)
3. Insere próximo ponto do cache de substituição (5 pontos pré-carregados) no slot vago
4. Quando conexão voltar: sincroniza `historico_usuario` no Supabase e revalida cache

### 1.4 Macro-Cerca e Recálculo dos Top 20

**Gatilho:** Usuário cruza o perímetro da macro-cerca (raio de 1km ao redor da posição inicial).

**Fluxo:**
1. S.O. acorda app em background
2. `GeofenceOrchestrator` destrói todas as 20 geofences ativas
3. App roda novamente a query `buscar_pontos_proximos` com novas coordenadas
4. Reconstrói as 20 geofences com os novos Top 20
5. Atualiza cache local com novos 5 pontos de substituição

---

## 2. Modelo de Monetização

### 2.1 Usuário Novo (Nunca pagou)

- Vê 10 pontos desbloqueados + 10 bloqueados com cadeado
- Banner AdMob no rodapé em todas as telas
- Cards nativos de anúncio integrados na lista de pontos (aparência similar a cards de ponto, mas identificados como "Patrocinado")
- Ao tentar acessar ponto 11-20: modal de paywall com opção de compra da cidade atual

### 2.2 Usuário com Pagamento por Cidade

- Vê todos os 20 pontos desbloqueados na cidade paga
- Zero anúncios na cidade paga
- Ao visitar nova cidade: vê 10 pontos gratuitos, zero anúncios, banner sutil no topo: "Desbloqueie [Cidade] ou acesse todas as cidades"
- Na segunda compra por cidade: modal apresenta passe vitalício com desconto proporcional

### 2.3 Usuário com Passe Vitalício

- Acesso ilimitado a todos os pontos em todas as cidades
- Zero anúncios permanentemente
- Badge visual de "Explorador" no perfil

### 2.4 Lógica de Upsell

```
Usuário acessa nova cidade
    └─ Nunca pagou → paywall padrão por cidade
    └─ Já pagou 1 cidade → banner sutil + upsell vitalício com desconto ao tentar desbloquear
    └─ Já pagou 2+ cidades → modal agressivo de upsell vitalício com desconto maior
```

---

## 3. Onboarding

### 3.1 Primeiro Acesso

**Tela 1 — Proposta de Valor:**
"Explore a cidade com um guia no seu ouvido. Áudios históricos que disparam automaticamente quando você chega perto."

**Tela 2 — Como Funciona:**
"Caminhe normalmente. Quando se aproximar de um ponto histórico, você recebe uma notificação com a história daquele lugar."

**Tela 3 — Gratuito vs Premium:**
"10 pontos históricos gratuitamente em qualquer cidade. Desbloqueie todos com um único pagamento."

**Tela 4 — Localização:**
Duas opções claras:
- "Usar minha localização atual" → solicita permissão de GPS com explicação: "Usamos sua localização para disparar áudios automáticos quando você se aproxima de pontos históricos."
- "Escolher uma cidade" → campo de busca de cidade para planejamento antecipado

### 3.2 Permissão de Localização Negada

- App entra em Modo Degradado: lista manual de pontos por cidade/bairro
- Banner suave persistente no topo: "Ative a localização para experiência automática completa → [Ativar]"
- Botão "Ativar" abre configurações do dispositivo diretamente
- Todas as funcionalidades de conteúdo (texto, áudio, imagem) funcionam normalmente

---

## 4. Gestão de Conteúdo de Áudio

### 4.1 Versões de Áudio

Cada ponto turístico possui duas versões de áudio geradas no Batch Ingestion:
- **Versão Rápida:** ~45 segundos, tom objetivo e direto, ideal para deslocamento
- **Versão Completa:** ~3 minutos, tom narrativo de storytelling, ideal para paradas

### 4.2 Preferência do Usuário

- Configurável nas preferências do app (acessível a todos os usuários)
- Padrão inicial: Versão Rápida
- Dentro do card de qualquer ponto: botão "Ouvir versão [Completa/Rápida]" baixa sob demanda a versão alternativa

### 4.3 Download no Modo Offline

- Apenas a versão padrão configurada é pré-baixada para os 20 pontos ativos
- Versão alternativa sempre baixada sob demanda ao solicitar
- Gerenciamento de storage: ao recalcular Top 20, áudios dos pontos removidos são deletados do cache local

---

## 5. Crowdsourcing & Reputação

### 5.1 Reporte de Dado Incorreto

**Categorias e Severidades:**

| Categoria | Threshold de Auditoria | Prioridade |
|-----------|----------------------|------------|
| Local fechado permanentemente | 1 reporte | Crítica — imediata |
| Horário errado | 3 reportes | Alta |
| Preço desatualizado | 3 reportes | Alta |
| Foto incorreta | 3 reportes | Baixa |
| Outros (texto livre) | 3 reportes | Média |

**Fluxo:**
1. Usuário seleciona categoria + texto opcional
2. Incrementa `contador_alertas` na tabela `controle_erros`
3. Se threshold atingido: dispara Edge Function de auditoria prioritária
4. Agente re-verifica dados no Google Places
5. Se divergência confirmada: corrige banco, zera contador, envia notificação push ao(s) usuário(s) que reportaram
6. Se sem divergência: zera contador silenciosamente, nenhuma notificação enviada

### 5.2 Sistema de Reputação

- Cada reporte válido (que resultou em correção real) concede pontos de reputação ao usuário
- Badges desbloqueáveis: "Explorador Atento" (1 correção), "Guardião da Cidade" (5 correções), "Curador Oficial" (20 correções)
- Badges exibidos no perfil do usuário
- Sem benefícios monetários no MVP — reconhecimento social apenas

### 5.3 Sugestão de Novos Pontos

**Fluxo:**
1. Usuário sugere novo ponto via botão "Sugerir lugar" no mapa
2. App normaliza coordenadas via Google Places API e obtém `place_id`
3. Registra sugestão na tabela `sugestoes_usuario`
4. Se `place_id` já existe em `ponto_turistico`: notifica usuário que o ponto já está no app
5. Se `place_id` já tem sugestão pendente: incrementa contador de votos
6. Ao atingir 10 votos distintos: dispara moderação da IA

**Moderação da IA:**
1. Valida existência e status atual no Google Places
2. Verifica duplicata no banco (via `place_id`)
3. Avalia se é um local com interesse turístico (baseado em rating, número de reviews, categorias)
4. Se aprovado: cria registro em `ponto_turistico` com `status = 'pendente_moderacao'`, entra na fila do Batch Ingestion
5. Se rejeitado: notifica administrador com motivo para decisão final

---

## 6. Internacionalização

### 6.1 Detecção de Idioma

- App detecta idioma do dispositivo automaticamente no primeiro acesso
- Idiomas suportados no MVP: preparado para expansão via arquivos de localização (i18n)
- Fallback: en-US para idiomas não suportados

### 6.2 Conteúdo Multi-idioma

- Cada ponto turístico tem conteúdo gerado independentemente por idioma
- Adicionar novo idioma = rodar Batch Ingestion nos pontos existentes com parâmetro de idioma
- Tabela `conteudo_midia` suporta N idiomas por ponto sem alteração de schema

### 6.3 Interface do App

- Todos os labels, menus e textos de UI traduzidos por idioma via sistema i18n
- Datas, moedas e formatos numéricos seguem locale do dispositivo

---

## 7. Invalidação de Cache

### 7.1 Estratégia de Hash de Versão

- Campo `content_hash` em `ponto_turistico` é atualizado sempre que auditoria altera dados
- App envia lista de `{ponto_id, content_hash}` dos 20 pontos em cache para endpoint de verificação
- Servidor retorna apenas os IDs com hash divergente
- App baixa dados frescos apenas dos pontos com divergência

### 7.2 TTL e Recálculo Espacial

- Cache dos Top 20 é invalidado por distância (> 1km percorrido) via macro-cerca
- Metadados leves (sem MP3) têm TTL de 5 minutos como fallback de segurança
- MP3s não têm TTL — só são deletados quando o ponto sai dos Top 20 ativos
