# Prompts de Desenvolvimento — Épico 6: Notificações Push

Este arquivo contém os prompts detalhados para cada tarefa do Épico 6. Use cada prompt individualmente para guiar a implementação da tarefa correspondente.

---

## Prompt para T-029: Configurar FCM (Firebase Cloud Messaging) para Android

**Contexto do Projeto:**
As notificações push são o canal de comunicação para acordar o app em segundo plano e notificar o turista quando o sistema de auditoria do Agente de IA corrige uma informação que ele reportou como errada. No Android, isso é orquestrado via FCM.

**Objetivo:**
Integrar o SDK do Firebase Cloud Messaging no módulo Android do app e sincronizar o token FCM do dispositivo com o perfil do usuário no Supabase.

**Requisitos Relacionados:**
- RF-18 (Formato da notificação push)
- RF-47 (Notificação push de confirmação de correção de reporte)
- RNF-05 (Funcionamento com app em background e tela bloqueada)

**Instruções de Implementação:**
1. Crie um projeto no console do Firebase e adicione um aplicativo Android usando o ID do pacote (`package name`) definido no projeto KMP.
2. Baixe o arquivo `google-services.json` e insira no diretório `/androidApp/`.
3. Adicione as dependências do Firebase BOM e Firebase Cloud Messaging no Gradle do projeto Android.
4. Crie a classe `MyFirebaseMessagingService` estendendo `FirebaseMessagingService`:
   - No `onNewToken(token: String)`: Envie o token do dispositivo para a tabela de perfis de usuário do Supabase (coluna `fcm_token` da tabela associada à conta do usuário) para mapeamento de destino.
   - No `onMessageReceived(remoteMessage: RemoteMessage)`: Trate mensagens push de dados que chegam em background, despachando para os canais de notificação adequados do Android.
5. Certifique-se de inicializar o Firebase no `Application` do Android.

**Critérios de Aceitação e Verificação:**
- O app Android compila sem erros de dependência do Firebase.
- Ao abrir o app e logar com um usuário, o console de logs do Android (Logcat) mostra a geração de um token FCM válido.
- Verifique no banco de dados do Supabase que o token do dispositivo foi salvo no registro do respectivo usuário autenticado.

---

## Prompt para T-030: Configurar APNs (Apple Push Notification service) para iOS

**Contexto do Projeto:**
No iOS, a entrega de notificações em segundo plano para atualização de dados do usuário e alertas de auditoria concluída deve ser feita de forma nativa através do serviço APNs (Apple Push Notification service) da Apple.

**Objetivo:**
Configurar as permissões e certificados do APNs no console da Apple Developer, integrar o registro do token APNs no app iOS e sincronizar com o Supabase Auth.

**Requisitos Relacionados:**
- RF-18 (Formato da notificação push)
- RF-47 (Notificação push de confirmação de correção de reporte)
- RNF-05 (Funcionamento com app em background e tela bloqueada)

**Instruções de Implementação:**
1. No portal Apple Developer, habilite a capability "Push Notifications" no App ID do projeto e crie uma chave/certificado de APNs (.p8).
2. Adicione a chave APNs criada nas configurações de autenticação e notificações no painel do Supabase.
3. No arquivo `AppDelegate.swift` do módulo `:iosApp` no Xcode:
   - Solicite a permissão do usuário para envio de notificações (`UNUserNotificationCenter.current().requestAuthorization`).
   - Registre o app para notificações remotas (`UIApplication.shared.registerForRemoteNotifications()`).
   - No callback `didRegisterForRemoteNotificationsWithDeviceToken`: Capture o token APNs nativo e sincronize-o com o Supabase gravando na coluna correspondente do perfil de usuário (`apns_token`).
   - No callback `didReceiveRemoteNotification`: Processe payloads silenciosos e payloads normais recebidos em segundo plano.

**Critérios de Aceitação e Verificação:**
- O projeto iOS compila e assina o app usando um perfil de Provisionamento de Desenvolvimento que possua suporte a Push habilitado.
- Ao rodar o app em um dispositivo iOS real (simuladores não recebem pushes remotos por padrão), o prompt de permissão para notificações é exibido.
- O token APNs do dispositivo é capturado e atualizado com sucesso no Supabase.

---

## Prompt para T-031: Implementar disparo de notificação local ao completar dwell time

**Contexto do Projeto:**
O disparo do áudio histórico guiado ocorre no momento em que o turista se aproxima do local e permanece lá por pelo menos 30 segundos. Esta notificação é disparada de forma local (client-side) pelo próprio app mobile, sem depender de uma chamada push de servidor externo.

**Objetivo:**
Escrever o serviço de agendamento e disparo de notificação push local em ambas as plataformas Android e iOS após o timer de dwell time ser satisfeito.

**Requisitos Relacionados:**
- RF-17 (Dwell time de 30s obrigatório com cancelamento se houver saída prévia)
- RF-18 (Formato da notificação: Título do ponto e subtítulo curto)
- RNF-05 (Funcionamento em background e tela bloqueada)

**Instruções de Implementação:**
1. Crie uma interface expect/actual no módulo shared para o serviço de notificações locais `LocalNotificationService`:
   - `fun enviarNotificacaoLocal(pontoId: String, titulo: String, mensagem: String)`
2. **Android (actual):**
   - Utilize a classe `NotificationCompat.Builder` e o `NotificationManager`.
   - Crie um canal de notificação ("Ponto Turístico Próximo") com prioridade alta e som ativado.
   - Defina um `PendingIntent` de deep link configurado para abrir diretamente a tela de detalhes do ponto turístico (`PontoDetalheScreen`) ao ser clicado.
3. **iOS (actual):**
   - Utilize a classe `UNMutableNotificationContent` e `UNNotificationRequest` do framework `UserNotifications`.
   - Defina o som padrão do sistema e adicione no dicionário `userInfo` o ID do ponto turístico para tratar a navegação interna ao abrir a notificação.
4. No orquestrador de geofencing, assim que o timer de 30 segundos de dwell time for completado com sucesso:
   - Obtenha os dados do ponto da tabela SQLite local.
   - Invoque o método de envio de notificação local correspondente à plataforma.

**Critérios de Aceitação e Verificação:**
- O mecanismo compila para ambas as plataformas.
- Execute um teste simulando a entrada do usuário em um ponto do geofencing:
  - Aguarde 30 segundos fora do app e verifique se a notificação local com o nome do local e o roteiro resumido é exibida.
  - Clicar na notificação deve acordar o app e abrir diretamente a tela de detalhes do ponto.
  - Se sair do raio do ponto em 15 segundos, certifique-se de que o timer seja destruído e nenhuma notificação seja disparada ao final.
