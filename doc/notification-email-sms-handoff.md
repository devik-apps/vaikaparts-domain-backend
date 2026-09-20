# Passation — notifications IN_APP, email et SMS

État du code au 19 septembre 2026. Ce document décrit ce qui existe dans le dépôt et les actions nécessaires avant d'utiliser EMAIL et SMS en production. Il complète [notification-preferences.md](notification-preferences.md).

## Ce qui est implémenté

Le domaine connaît trois canaux : `IN_APP`, `EMAIL` et `SMS` ([NotificationChannelType](../src/main/java/com/devikapps/vaikaparts/model/classifier/NotificationChannelType.java)). `IN_APP` est obligatoire. EMAIL et SMS nécessitent **deux autorisations** : activation globale dans la configuration et accord de l'utilisateur. Les deux préférences sont désactivées par défaut pour tous les types d'utilisateur.

| Élément | Rôle |
| --- | --- |
| [User](../src/main/java/com/devikapps/vaikaparts/model/user/User.java), [JUser](../src/main/java/com/devikapps/vaikaparts/repository/model/user/JUser.java) | Exposent et stockent `emailNotificationsEnabled` et `smsNotificationsEnabled` pour vendeur, chercheur et manager. |
| [V1_0_28](../src/main/resources/db/migration/V1_0_28__Add_user_notification_preferences.sql) | Ajoute les deux colonnes à `users`, `NOT NULL DEFAULT FALSE`. |
| [NotificationPreferencesUpdater](../src/main/java/com/devikapps/vaikaparts/service/util/NotificationPreferencesUpdater.java) | Applique uniquement les booléens JSON présents dans `user_metadata` ; une clé absente, `null` ou invalide conserve la valeur enregistrée. |
| [UserCreationService](../src/main/java/com/devikapps/vaikaparts/service/UserCreationService.java), [UserSyncService](../src/main/java/com/devikapps/vaikaparts/service/UserSyncService.java) | Initialisent les préférences à la création et les synchronisent aux mises à jour du webhook. [UserService](../src/main/java/com/devikapps/vaikaparts/service/UserService.java) les reprend aussi lorsqu'un chercheur est créé avant son webhook. |
| [InAppNotificationChannel](../src/main/java/com/devikapps/vaikaparts/service/notification/InAppNotificationChannel.java) | Enregistre la notification dans `notifications`, puis tente l'envoi WebSocket. |
| [EmailNotificationChannel](../src/main/java/com/devikapps/vaikaparts/service/notification/EmailNotificationChannel.java) | Vérifie l'adresse, choisit un sujet selon `NotificationType`, échappe le contenu HTML et appelle `Mailer.sendOrThrow`. |
| [SmsNotificationChannel](../src/main/java/com/devikapps/vaikaparts/service/notification/SmsNotificationChannel.java) | Transforme la notification en `SmsMessage` et appelle l'interface [SmsProvider](../src/main/java/com/devikapps/vaikaparts/sms/SmsProvider.java). |
| [BefianaSmsClient](../src/main/java/com/devikapps/vaikaparts/sms/befiana/BefianaSmsClient.java) | Implémente `SmsProvider` avec l'API HTTP d'envoi immédiat BEFIANA ; le canal SMS ne dépend pas de ce fournisseur précis. |
| [NotificationService](../src/main/java/com/devikapps/vaikaparts/service/notification/NotificationService.java) | Construit la notification, choisit les canaux et coordonne la transaction. |

Les préférences peuvent être fournies dans `raw_user_meta_data` du webhook Supabase, sous forme de **booléens JSON** :

```json
{
  "email_notifications_enabled": true,
  "sms_notifications_enabled": false
}
```

Un `false` explicite désactive le canal. La valeur de la BDD est utilisée quand la clé est absente. Le webhook lit le type de l'utilisateur dans `raw_app_meta_data.user_type`. Les préférences sont visibles dans le profil retourné par `GET /users/me` et dans la [spec API](api.yaml). Il n'existe actuellement **aucun endpoint de modification directe des préférences** dans ce backend : la modification passe par `user_metadata` puis sa synchronisation. Il faut donc conserver ces métadonnées cohérentes avec les choix affichés par le frontend.

## Parcours d'une notification

1. La publication d'une demande crée un événement `DemandPublishedRequested`. Le [service parent](../src/main/java/com/devikapps/vaikaparts/service/event/DemandPublishedRequestedService.java) sélectionne les vendeurs actifs, en tenant compte de `handleAllCategory` et de la catégorie de la pièce, puis crée un `DemandPublishedNotificationRequested` par vendeur. La publication d'une offre utilise le même événement parent, avec `offerId`, et crée un seul événement enfant pour le chercheur propriétaire de la demande.
2. Le [service enfant](../src/main/java/com/devikapps/vaikaparts/service/event/DemandPublishedNotificationRequestedService.java) enregistre son journal de traitement (`notification_requested`) et appelle `NotificationService.createAndSendNotification`.
3. `NotificationService` charge l'utilisateur depuis la BDD, construit le message et enregistre d'abord la notification via `IN_APP`. Si cette opération échoue, il annule le traitement des canaux externes.
4. Il retient EMAIL et/ou SMS seulement si le canal est activé globalement, que la préférence de l'utilisateur vaut `true` et qu'une coordonnée non vide est disponible. Chaque canal vérifie ensuite son propre format.
5. En présence de la transaction Spring, EMAIL et SMS sont appelés dans `afterCommit`. Un rollback n'entraîne donc aucun envoi externe. Les échecs des canaux externes sont isolés : un échec EMAIL n'empêche pas SMS, et la notification IN_APP déjà validée en BDD n'est pas annulée. Sans synchronisation transactionnelle, le code émet un avertissement et envoie immédiatement.

Le `afterCommit` utilisé ici est un **callback en mémoire, sur le thread qui termine la transaction**. Il ne crée ni tâche persistante ni garantie de reprise après redémarrage. La préférence consultée est celle chargée lors de la création de la notification : elle n'est pas relue au moment du callback.

## Configurer et activer EMAIL

Un serveur mail hébergé par l'application n'est pas nécessaire ; il faut un **service SMTP** accessible et des identifiants. La configuration actuelle ([EmailConf](../src/main/java/com/devikapps/vaikaparts/config/EmailConf.java)) utilise l'authentification SMTP et STARTTLS. Renseigner les variables suivantes dans l'environnement de déploiement, sans placer de secret dans Git :

```dotenv
SPRING_MAIL_HOST=smtp.exemple.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=identifiant-smtp
SPRING_MAIL_PASSWORD=secret-smtp
SPRING_MAIL_FROM_EMAIL=notifications@exemple.com
NOTIFICATIONS_EMAIL_ENABLED=true
```

Le port est un **exemple** : utiliser celui du service SMTP choisi, compatible avec STARTTLS. L'adresse d'expédition doit être autorisée par ce service ; suivre ses instructions de validation du domaine et de réputation. [NotificationChannelsConf](../src/main/java/com/devikapps/vaikaparts/config/NotificationChannelsConf.java) n'enregistre le canal email que lorsque l'activation globale vaut `true`.

L'ancien `Mailer.accept()` continue à absorber les erreurs pour ses autres appelants. Le canal utilise `Mailer.sendOrThrow()` : une erreur SMTP remonte jusqu'à `NotificationService`, qui la journalise sans empêcher les autres canaux. Les timeouts SMTP sont bornés et les traces SMTP détaillées sont désactivées. Une réponse SMTP positive prouve la prise en charge par le serveur, pas l'arrivée dans la boîte de réception.

Pour recevoir l'email, le destinataire doit aussi avoir une adresse enregistrée et `email_notifications_enabled: true` en BDD. Un simple changement de `NOTIFICATIONS_EMAIL_ENABLED` n'inscrit pas tous les utilisateurs au canal.

## Configurer et activer SMS

Obtenir auprès de BEFIANA **l'URL d'origine HTTPS**, la clé API, un compte autorisé et du crédit SMS. [SmsConf](../src/main/java/com/devikapps/vaikaparts/config/SmsConf.java) crée le fournisseur et le canal uniquement lorsque `NOTIFICATIONS_SMS_ENABLED=true`. La configuration est :

```dotenv
NOTIFICATIONS_SMS_ENABLED=true
BEFIANA_SMS_BASE_URL=https://hote-fourni-par-befiana
BEFIANA_SMS_API_KEY=cle-api-secrete
SMS_CONNECT_TIMEOUT=5s
SMS_REQUEST_TIMEOUT=10s
```

`BEFIANA_SMS_BASE_URL` est l'origine seule, **sans** `/api/smsko/v1/send/`, paramètres d'URL ou identifiants. Le client ajoute le chemin `POST /api/smsko/v1/send/`. Il met la clé directement dans l'en-tête `Authorization`, sans préfixe `Bearer`, et envoie :

```json
{"phone_number":"321234567","message":"Votre notification VaikaParts"}
```

Le client accepte `0321234567`, `+261321234567`, `00261321234567` et `321234567`, puis transmet `321234567` au fournisseur. Il rejette les autres formats et les messages vides ou dépassant 320 points de code Unicode. Les redirections HTTP sont désactivées pour protéger la clé. La réponse positive doit contenir `clientCorrelator` et `address` ; `SmsReceipt` conserve le premier comme identifiant fournisseur. Cela confirme la **soumission**, pas la livraison au téléphone. Une erreur réseau, un timeout ou une réponse positive inexploitable est classé `UNKNOWN_OUTCOME` : le fournisseur peut avoir accepté le SMS malgré l'absence de réponse utilisable. Le client ne réessaie pas automatiquement.

Le destinataire doit aussi avoir un numéro enregistré et `sms_notifications_enabled: true` en BDD. La sélection actuelle teste seulement la présence de la coordonnée ; elle ne prouve pas que le numéro appartient à l'utilisateur. Le canal et le client vérifient ensuite le format.

## Ce qu'il reste à faire

1. **Revoir l'acquittement RabbitMQ.** Le [EventConsumer](../src/main/java/com/devikapps/vaikaparts/event/consumer/EventConsumer.java) soumet le traitement à un autre thread et retourne aussitôt du `@RabbitListener`. Les erreurs de désérialisation ou de dispatch sont seulement journalisées dans ce thread. L'acquittement du message n'est donc pas lié à la réussite du traitement : selon la configuration du listener, le message peut être acquitté avant d'être réellement traité. Vérifier la configuration effective du conteneur RabbitMQ, puis faire remonter les échecs au mécanisme d'acquittement/reprise choisi. Un ACK du broker côté producteur ne prouve ni le dispatch ni le traitement par ce consumer.
2. **Préparer l'environnement.** Résoudre l'accès aux dépendances privées Gradle, déployer les migrations Flyway (notamment [V1_0_28](../src/main/resources/db/migration/V1_0_28__Add_user_notification_preferences.sql)), configurer un vrai service SMTP et obtenir l'origine HTTPS et la clé BEFIANA. Garder les flags externes à `false` pendant la préparation.
3. **Donner le choix dans l'interface.** Afficher IN_APP comme toujours actif, proposer les interrupteurs EMAIL et SMS, enregistrer leurs valeurs comme booléens dans les métadonnées utilisateur, puis vérifier leur synchronisation en BDD. Tester aussi les passages de `true` à `false`. Un endpoint authentifié de mise à jour des préférences pourra remplacer ce détour par le webhook si l'application en a besoin ; il faudra alors décider quelle source fait foi pour éviter qu'un ancien webhook écrase un choix récent.
4. **Tester les fournisseurs en préproduction.** Vérifier la connexion SMTP, l'expéditeur autorisé et la réception d'un email réel ; vérifier avec un petit nombre de SMS réels le format téléphonique attendu, l'en-tête `Authorization`, le solde et le retour BEFIANA. Faire les essais avec un vendeur inscrit pour une demande et un chercheur inscrit pour une offre. Contrôler séparément les cas canal désactivé, préférence désactivée, coordonnée manquante, échec fournisseur et rollback.
5. **Fiabiliser avant des volumes importants.** Ajouter une tâche persistante par `(notification_id, channel)` avec statut, tentative, prochaine reprise et identifiant fournisseur. Créer ces tâches dans la transaction de la notification, puis les traiter séparément avec limites et reprise après crash. Définir la déduplication des événements et une politique spécifique à `UNKNOWN_OUTCOME` pour éviter des SMS facturés deux fois. Aujourd'hui, ni les callbacks `afterCommit` ni les journaux d'événements ne fournissent ce suivi par canal.
6. **Améliorer l'exploitation.** Vérifier les coordonnées réellement contrôlées par l'utilisateur, prévoir des plafonds de SMS, un suivi des rejets email/SMS et, si le fournisseur le permet, des retours de livraison. Revoir aussi le journal `UserSyncService` qui imprime actuellement le webhook entier, donc potentiellement des données personnelles. Le WebSocket IN_APP est tenté avant le commit ; si l'on veut une stricte cohérence temps réel/BDD, déplacer cette émission après commit.

La propriété `Seller.isDeliverying` ajoutée récemment indique seulement si le vendeur propose une livraison ; **elle ne modifie pas la sélection des canaux de notification**. Elle utilise [V1_0_29](../src/main/resources/db/migration/V1_0_29__Add_seller_delivery_option.sql) et `raw_user_meta_data.is_deliverying`.

## Vérifications et logs

Les tests ciblés couvrent la lecture des préférences pour les trois types d'utilisateur, le choix des canaux, les callbacks de commit/rollback, l'isolation des erreurs, le mapping des vendeurs et le contrat des adaptateurs. Des séries ciblées ont passé précédemment, mais **aucune validation complète Gradle + BDD + SMTP + BEFIANA en conditions de déploiement n'a été établie**. Le build Gradle hors ligne a été bloqué par les dépendances privées `vaikaparts-gen` et `vaikaparts-pecunia-client-gen`. Les tests HTTP qui démarrent un serveur local n'ont pas pu être validés dans l'environnement sandboxé précédent.

Pour diagnostiquer, filtrer les logs sur `[NOTIF-PIPELINE]` : `CHANNEL_SKIPPED` indique une préférence désactivée ou une coordonnée absente ; `EXTERNAL_WAIT_COMMIT` indique l'attente de la transaction ; `CHANNEL_SEND`, `CHANNEL_RETURNED`, `CHANNEL_FAILED` et `SMS_FAILED` décrivent les tentatives. `CHANNEL_RETURNED` n'est **pas** une confirmation de livraison. Les journaux parent/enfant peuvent indiquer `SUCCESS` alors qu'un canal externe a échoué après commit ; regarder les logs du canal ou, une fois implémentée, sa tâche persistante.

Le fichier [notification-preferences.md](notification-preferences.md) fournit des détails supplémentaires sur les trois étapes d'implémentation et les limites actuelles.
