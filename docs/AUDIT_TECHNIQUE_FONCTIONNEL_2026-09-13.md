# Audit technique et fonctionnel — LMS INPG

**Date :** 13 septembre 2026  
**Révision examinée :** `97657c1`, avec l’état local du dépôt.  
**Objet :** sécurité, fiabilité, qualité du code, pertinence des fonctionnalités et rapprochement avec le TDR de mise en place du LMS.  
**Livrable :** diagnostic, plan de priorités et état des correctifs appliqués dans le dépôt local.

> **Suivi de mise en œuvre — 14 septembre 2026.** Les actions directement
> réalisables dans le dépôt ont été appliquées : A01 à A05, A06, A10, A12,
> l'exécution qualité d'A13, A14 et A16. A07 était déjà couvert par le contrôle d'état lors du dépôt de
> pièce. A08 dispose désormais d'un contrôle de signature binaire pour les
> pièces d'admission et A11 renvoie une indisponibilité au lieu de confirmer un
> contact non transmis. Les JWT sont également révoqués après changement de mot
> de passe ou déconnexion, et l'inscription ne renvoie plus de détail technique.
> La réinitialisation vérifie aussi le nouveau mot de passe avant de consommer
> l'OTP, ce qui évite de bloquer un utilisateur ayant simplement corrigé sa saisie.
> Restent dépendants d'une décision ou d'une infrastructure
> réelle : analyse antivirus/désarmement de documents (A08), réconciliation des
> fichiers historiques et politique de conservation (A09), messagerie persistante
> avec reprise (A11), pagination métier de toutes les listes (A15, la liste principale
> des candidatures est désormais paginée), sauvegarde et
> restauration, intégration de paiement/SMS/SIGB/visioconférence et déploiements
> effectifs. Ces éléments ne sont pas simulés par les présentes modifications.

## 1. Avis général

**Le projet possède une base métier utile et mérite d’être consolidé. Je déconseille une réécriture complète et je recommande de sécuriser les accès, fiabiliser les parcours et rétablir les tests avant d’étendre le périmètre ou de généraliser une refonte visuelle.**

Les fonctionnalités essentielles sont présentes : candidature, admission, gestion pédagogique, cours, notes, documents délivrés, échanges, entreprises et accréditation. Plusieurs protections ont déjà été correctement introduites : contrôle des rôles côté serveur, contrôle de propriété de dossiers, séparation des fichiers publics et privés, migrations de base et journal des étapes de candidature.

Le problème principal est l’accumulation de comportements de démonstration, de morceaux d’interface inachevés et de règles dispersées. Certaines fonctionnalités donnent une impression de maturité supérieure à leur fonctionnement réel : champs bancaires sans passerelle, anciennes fonctions OTP dans la connexion, étapes de déploiement qui affichent seulement un message.

### Les cinq décisions à prendre en premier

1. Isoler les comptes et données de démonstration de tout environnement réel.
2. Renforcer la récupération de compte et invalider les sessions après une réinitialisation de mot de passe.
3. Retirer les champs de carte bancaire tant qu’aucun prestataire de paiement n’est intégré.
4. Remettre les tests et la chaîne d’intégration en état avant une livraison.
5. Stabiliser le parcours candidature → pièces → validation → règlement → admission avant d’ajouter des écrans.

## 2. Méthode, portée et limites

L’audit repose sur la lecture du code local Angular et Spring Boot, des routes, des configurations, des contrôleurs et services sensibles, de plusieurs tests et du workflow GitHub. Les zones d’authentification, de candidature, de paiement et de fichiers ont reçu l’examen le plus détaillé. Les espaces pédagogie, alumni, entreprises et accréditation ont fait l’objet d’un échantillonnage de leurs contrôles et de leur organisation.

Ce rapport n’est pas un test d’intrusion ni une certification de sécurité. Les configurations réelles de Railway, Vercel, du proxy, de la messagerie, des sauvegardes et de la base de production n’ont pas été inspectées. Une absence de protection dans le dépôt ne prouve donc pas l’absence d’une protection au niveau de l’infrastructure. Aucun compte réel n’a été utilisé, aucun e-mail envoyé et aucune transaction financière déclenchée.

Les commentaires historiques ont servi d’orientation ; les conclusions s’appuient sur le code actuel. Par exemple, le candidat ne peut plus auto-confirmer un paiement et les pièces privées ne sont plus exposées par le gestionnaire statique des uploads.

**Niveaux :** P0 = à traiter immédiatement si l’application est exposée ; P1 = avant une mise en service réelle ; P2 = fiabilisation prioritaire ; P3 = simplification. Ce sont des priorités de livraison, pas des scores CVSS.

**Preuves :** « confirmé par lecture » signifie que le comportement découle du code inspecté ; « reproduit » signifie qu’une commande a été exécutée ; « risque conditionnel » précise les conditions nécessaires, sans prétendre qu’une exploitation a eu lieu.

### Vérifications exécutées

| Vérification | Résultat | Portée |
|---|---|---|
| Compilation Angular en configuration development | Réussie, code de sortie 0 | Le frontend compile ; cela ne valide ni les parcours ni le bundle de production |
| Compilation Angular en configuration production | Réussie, code de sortie 0 | Bundle de production généré après les correctifs |
| Tests Angular avec `--watch=false` | Réussis : 22 tests dans 21 fichiers | Configuration de test réparée, avec tests HTTP et composants exécutés |
| Tests backend ciblés | Non exécutables localement | Le wrapper Windows échoue avant de lancer Maven (`Cannot start maven from wrapper`) ; la CI Linux doit exécuter `./mvnw test` |
| Tests backend ajoutés | Politique de mot de passe et signature de fichier | Présents dans le dépôt ; exécution à confirmer en CI une fois le wrapper rétabli |

Les quatre classes backend visées étaient `PaiementServiceWorkflowTest`, `PerimetreFormateurServiceTest`, `CertificatSignatureServiceTest` et `CandidatureServiceChangerStatutTest`. L’absence locale de dépendances ne démontre pas une erreur du code métier. Le test `LmsApplicationTests`, qui démarre Spring sans profil isolé, n’a pas été lancé : la configuration locale peut connecter Flyway à une base utilisée par l’équipe.

## 3. Registre des problèmes

| ID | Priorité | Problème | État de preuve |
|---|---|---|---|
| A01 | P0 | Comptes de démonstration avec mots de passe prédéfinis créés au démarrage | Code confirmé ; exposition réelle à vérifier |
| A02 | P1 | OTP sans limitation des essais et consommation non atomique | Confirmé par lecture |
| A03 | P1 | Changer ou réinitialiser le mot de passe ne révoque pas les JWT existants | Confirmé par lecture |
| A04 | P1 | Politique de mot de passe insuffisante et incohérente côté serveur | Confirmé par lecture |
| A05 | P1 | Collecte de données de carte bancaire sans traitement bancaire | Confirmé par lecture du frontend et de l’appel API |
| A06 | P2 | Changement de rôle non atomique, entrées invalides destructives | Corrigé localement : validation puis mise à jour transactionnelle |
| A07 | P2 | Ajout de pièces sans contrôle de l’étape du dossier | Confirmé par lecture ; règle métier à formaliser |
| A08 | P2 | Contrôle du format des fichiers sans analyse de leur contenu | Réduit : signatures PDF/JPEG/PNG/WebP vérifiées ; antivirus reste requis |
| A09 | P2 | Fichiers et base de données peuvent se désynchroniser | Réduit : compensation à l'échec/rollback des nouveaux dépôts et suppression après commit ; historique à réconcilier |
| A10 | P2 | Paiements : tentatives écrasées et concurrence non protégée | Corrigé localement par verrou pessimiste ; concurrence à confirmer en intégration |
| A11 | P2 | Contact et notifications : succès trompeur ou envoi non fiable | Réduit : erreur SMTP signalée au client ; reprise persistante reste requise |
| A12 | P1 | Tests frontend inexécutables et sécurité HTTP insuffisamment testée | Corrigé : 22 tests Angular réussissent ; tests backend bloqués par le wrapper local |
| A13 | P1 | Workflow CI incomplet et déploiements factices | Qualité frontend ajoutée ; déploiements réels exigent les accès d'infrastructure |
| A14 | P2 | Association Spring Boot / springdoc hors matrice de compatibilité | POM + documentation officielle |
| A15 | P2 | Listes chargées intégralement et filtrées en mémoire | Réduit : endpoint paginé pour les candidatures ; autres listes à traiter |
| A16 | P2 | JWT envoyé par l’intercepteur à toute URL HttpClient | Confirmé ; fuite vers un tiers non démontrée |
| A17 | P3 | Recherche inactive, code ancien et responsabilités trop concentrées | Confirmé par lecture et recherche des références |

### A01 — Les données de démonstration ne sont pas isolées

**Ce qui ne va pas.** `DataInitializer` est une configuration Spring ordinaire. Son runner crée notamment un compte d’admission si son nom d’utilisateur n’existe pas, avec un mot de passe littéral présent dans le dépôt. `PedagogieDataInitializer` crée également un compte étudiant de démonstration. Aucun `@Profile` ou interrupteur conditionnel n’isole ces deux classes.

**Conséquence.** Sur une base initialisée avec ce code, un compte privilégié peut exister avec des identifiants connus. Si ce compte est supprimé sans désactiver le runner, un redémarrage peut le recréer. Le code prouve le mécanisme ; il ne prouve pas que ce compte existe encore avec son mot de passe initial en production.

**Il fallait faire.** Séparer l’initialisation des rôles/référentiels de celle des comptes et contenus fictifs. Placer la démonstration derrière un profil explicite et une propriété désactivée par défaut. Créer le premier administrateur par une procédure d’invitation à usage unique. Si une instance réelle utilise ces comptes, les inventorier puis les désactiver ou renouveler leurs accès.

Le Compose de développement publie aussi PostgreSQL sur `5432` et prévoit un mot de passe de repli connu. Conserver cette commodité uniquement dans une configuration locale explicitement identifiée, avec accès limité à la machine. Le fichier ne permet pas de conclure que la base de production est exposée.

**Validation attendue.** Démarrage sur base vide en profil production : les référentiels nécessaires existent, aucun compte de démonstration n’est créé ; la configuration refuse les secrets manquants.

**Sources :** [DataInitializer.java:67](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/config/DataInitializer.java:67), [création du compte:220](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/config/DataInitializer.java:220), [PedagogieDataInitializer.java:46](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/config/PedagogieDataInitializer.java:46), [docker-compose.yml](F:/Real_projects/Inpg/LMS_Repo/docker-compose.yml).

### A02 — La récupération par OTP manque de protections

**Ce qui ne va pas.** Le service génère un code à six chiffres valable cinq minutes, mais ne compte pas les échecs et ne limite pas les nouveaux envois. Les codes expirés ne sont supprimés que lors d’une validation ou d’une invalidation. Le stockage est une map en mémoire de chaque instance. Enfin, la lecture du code et sa suppression sont deux opérations distinctes : deux requêtes concurrentes peuvent lire le même code avant sa consommation.

**Conséquence.** Tentatives automatisées, abus de messagerie, accumulation d’entrées et comportement instable après redémarrage ou avec plusieurs instances. Un dispositif externe peut atténuer le trafic, mais aucune limitation applicative correspondante n’a été trouvée. La course entre validations est déduite du code, pas reproduite sous charge.

**Il fallait faire.** Stocker un challenge expirant avec compteur d’essais et consommation atomique ; normaliser l’adresse ; limiter séparément les demandes par compte et par origine ; ne pas conserver inutilement des codes pour des adresses arbitraires. Par exemple, cinq essais et un délai entre envois peuvent constituer un point de départ à calibrer. Séparer le challenge de récupération de toute future MFA et éviter qu’un endpoint de simple vérification consomme le challenge nécessaire à une autre action.

**Validation attendue.** Limite d’essais effective côté API, expiration nettoyée, un seul succès sur deux validations simultanées, fonctionnement cohérent après changement d’instance.

**Sources :** [OtpService.java:31](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/service/OtpService.java:31), [AuthController.java:84](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/controller/AuthController.java:84). Les recommandations d’expiration, d’usage unique et de protection contre les essais automatisés sont cohérentes avec [OWASP — récupération de mot de passe](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).

### A03 — Un ancien jeton reste valable après changement de mot de passe

**Ce qui ne va pas.** `JwtUtils.validateToken` vérifie le nom d’utilisateur et l’expiration, sans version de session ni date de changement de mot de passe. Les deux chemins de changement de mot de passe remplacent uniquement le hash. L’endpoint de déconnexion répond avec succès sans révoquer le jeton.

**Conséquence.** Une copie d’un JWT peut rester utilisable après une récupération de compte, jusqu’à son expiration. La valeur de repli configurée est de 6 000 000 ms, soit 100 minutes. Les rôles sont cependant relus en base par le filtre : je ne conclus pas que retirer un rôle laisse automatiquement les anciens droits actifs.

**Il fallait faire.** Ajouter une version de session utilisateur vérifiée par le serveur et l’incrémenter après récupération de compte ; prévoir la révocation des sessions et des jetons de renouvellement. Une déconnexion de l’appareil courant et une déconnexion de tous les appareils doivent avoir des effets explicites.

**Validation attendue.** Après réinitialisation, une requête avec l’ancien JWT reçoit 401 ; une nouvelle connexion fonctionne. Tester aussi suppression de compte et modification des rôles.

**Sources :** [JwtUtils.java:79](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/security/JwtUtils.java:79), [UserService.java:144](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/service/UserService.java:144), [AuthController.java:236](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/controller/AuthController.java:236), [configuration JWT](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/resources/application.properties).

### A04 — L’inscription accepte un mot de passe trop faible

**Ce qui ne va pas.** L’inscription serveur exige seulement un mot de passe non vide : un seul caractère non blanc passe ce contrôle. Les changements exigent six caractères. `UserDto` ne porte pas de contraintes de validation et `register` n’utilise pas `@Valid`. L’adresse est normalisée à l’inscription, mais pas de manière homogène dans la connexion et la récupération.

**Conséquence.** Les règles du navigateur peuvent être contournées en appelant directement l’API. L’utilisateur peut aussi subir des échecs liés aux espaces ou à la casse de l’adresse suivant le parcours.

**Il fallait faire.** Une politique unique côté serveur, partagée entre inscription et changements ; validation de l’adresse, des longueurs et des champs requis par des DTO dédiés. Préférer des phrases de passe longues et contrôler les mots de passe compromis ; introduire une MFA pour les fonctions privilégiées. Ne pas présenter les restes d’OTP de l’écran de connexion comme une MFA : le JWT est actuellement émis dès le mot de passe validé.

**Validation attendue.** Les mêmes cas limites sont refusés sur toutes les API ; les variantes d’une même adresse sont traitées de façon cohérente.

**Sources :** [UserService.java:47](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/service/UserService.java:47), [AuthController.java:66](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/controller/AuthController.java:66), [UserDto.java](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/dto/UserDto.java), [login.ts](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/auth/login/login.ts). Référence de conception : [OWASP — authentification](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).

### A05 — Les champs bancaires doivent disparaître dans le fonctionnement actuel

**Ce qui ne va pas.** L’interface demande titulaire, numéro de carte, expiration et CVC. Pourtant, `initierPaiement` envoie seulement l’identifiant de candidature et le libellé du moyen choisi. Aucune passerelle n’est connectée ; la validation est administrative.

**Conséquence.** On demande des données sensibles sans nécessité fonctionnelle et on laisse croire qu’un débit peut être exécuté. Je n’ai pas constaté leur enregistrement dans le backend : le problème démontré est leur collecte inutile dans le navigateur et la promesse du parcours.

**Il fallait faire.** À court terme, afficher « Déclarer un règlement », les instructions, la référence et les justificatifs nécessaires, puis « En attente de vérification ». À terme, confier la saisie bancaire à une page hébergée par un prestataire choisi par l’établissement et traiter sa confirmation vérifiée côté serveur. Conserver la validation manuelle si c’est le processus opérationnel retenu.

**Validation attendue.** Aucune saisie carte/CVC dans le mode manuel ; le candidat ne peut jamais passer son propre paiement à SUCCES ; les statuts affichés reflètent ceux du serveur.

**Sources :** [paiement.component.html:277](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/recrutement/candidature/paiement/paiement.component.html:277), [paiement.component.ts:151](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/recrutement/candidature/paiement/paiement.component.ts:151), [CandidatController.java:558](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/CandidatController.java:558).

### A06 — Le changement de rôle peut laisser un compte incohérent

**Ce qui ne va pas.** `changeUserRoles` efface les rôles avant de valider complètement la demande. Une liste vide ou contenant uniquement des codes inconnus peut retirer le rôle actuel. L’utilisateur est enregistré avant la création du profil formateur ou étudiant, sans transaction englobant ces écritures. Les autres types de profils ne sont pas créés par ce chemin.

**Conséquence.** Compte privé de son espace ou rôle attribué avec profil manquant. Le responsable admission peut également affecter des rôles de domaines différents : ce pouvoir doit être une décision explicite, pas une permission administrative implicite. Aucun accès anonyme à cette opération n’a été constaté.

**Il fallait faire.** Déplacer l’opération dans un service transactionnel, valider le rôle cible avant toute mutation, contrôler les transitions et profils nécessaires, protéger le dernier gestionnaire des comptes et journaliser auteur, motif, ancien rôle et nouveau rôle. Ne pas introduire un multirôle général sans matrice de permissions.

**Validation attendue.** Rôle invalide : erreur et compte inchangé. Échec de création de profil : rollback complet. Changement sensible : trace persistée.

**Source :** [AdminController.java:52](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/auth/controller/AdminController.java:52).

### A07 — Les pièces d’un dossier ne sont pas figées selon son état

**Ce qui ne va pas.** `televerserPiece` contrôle la propriété, le type et certaines limites, mais ne vérifie pas le statut de la candidature avant d’ajouter une pièce. Il enregistre une nouvelle pièce non conforme sans déclencher de révision de la décision existante.

**Conséquence.** Le contenu d’un dossier peut évoluer après son examen sans que l’étape métier reflète ce nouvel état. Il ne s’agit pas d’un accès au dossier d’un autre candidat : le contrôle de propriétaire est bien présent.

**Il fallait faire.** Définir les états autorisant un complément, puis figer une version des pièces utilisées pour chaque décision. Après validation, un ajout doit être refusé ou passer par une demande de complément/réouverture tracée. La règle exacte doit être alignée avec le service admission.

**Validation attendue.** Tester le dépôt de pièce pour chaque statut, notamment dossier rejeté et dossier admis, avec vérification du journal et de la version examinée.

**Source :** [CandidatController.java:472](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/CandidatController.java:472).

### A08 — L’extension ne garantit pas le contenu d’un fichier

**Ce qui ne va pas.** `ValidationFichier.exiger` contrôle l’extension et le type MIME déclaré, sans examiner les octets. Renommer un contenu arbitraire avec une extension autorisée peut donc franchir ce contrôle. La liste blanche, l’exclusion de HTML/SVG et la protection du chemin sont néanmoins de bons garde-fous déjà présents.

**Il fallait faire.** Ajouter détection du format réel, limites adaptées et quarantaine avant diffusion ; analyser les documents par un antivirus local ou un service approuvé pour ces données, sans expédier les dossiers à un service public. Réencoder les images lorsque pertinent. Garder les fichiers privés derrière leur contrôle d’accès.

**Validation attendue.** Refus d’un faux PDF, d’une image invalide et d’un format interdit ; fichier en quarantaine inaccessible ; aucun contournement du répertoire autorisé.

**Sources :** [ValidationFichier.java:86](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/fichier/ValidationFichier.java:86), [OWASP — dépôt de fichiers](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html). Ce constat ne démontre pas une exécution de code sur le serveur.

### A09 — La gestion physique des fichiers manque de compensation

**Ce qui ne va pas.** Le fichier est copié avant sa ligne en base ; la pièce est encore enregistrée séparément. Une erreur de base peut laisser un fichier orphelin. Lors d’une suppression, une erreur de disque est ignorée puis la ligne est supprimée.

**Conséquence.** Documents oubliés sur disque, stockage qui grossit et suppression annoncée sans disparition effective du fichier.

**Il fallait faire.** Prévoir un état temporaire, un nettoyage compensatoire et une tâche de rapprochement fichiers/base ; journaliser les échecs de suppression et les réessayer. Une transaction SQL seule n’annule pas une copie sur disque. Sauvegarder et restaurer ensemble la base et les documents.

**Validation attendue.** Simuler une erreur de base après copie, puis une erreur disque à la suppression ; aucune perte silencieuse, reprise ou nettoyage observable.

**Source :** [FichierStockeService.java:51](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/FichierStockeService.java:51).

### A10 — Les paiements sont idempotents en séquence, pas garantis en concurrence

**Ce qui ne va pas.** Le service évite de rejouer une confirmation déjà réussie, mais le contrôle et la mise à jour ne sont pas atomiques. Ni version optimiste ni verrou de lecture n’ont été trouvés sur ce chemin. Une nouvelle tentative réutilise la même ligne et remplace la référence précédente.

**Conséquence.** Deux confirmations simultanées peuvent rejouer les notifications ; confirmation et rejet concurrents peuvent se contredire. L’historique des tentatives est perdu. Un futur callback tardif aurait besoin de retrouver sa propre tentative. Aucun double débit bancaire n’a été démontré, puisqu’il n’y a pas de passerelle.

**Il fallait faire.** Séparer paiement attendu et tentatives, conserver les références, effectuer les transitions avec version/verrou ou mise à jour conditionnelle, et utiliser une clé d’idempotence pour les effets externes. En mode manuel, garder également la preuve et l’auteur de validation.

**Validation attendue.** Deux confirmations simultanées : une seule transition et une seule notification. Une relance conserve la première référence ; une confirmation tardive est rattachée à la bonne tentative.

**Sources :** [PaiementService.java:94](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/PaiementService.java:94), [confirmation:223](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/PaiementService.java:223), [Paiement.java](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/entity/Paiement.java), [PaiementRepository.java](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/repository/PaiementRepository.java).

### A11 — Le contact peut être perdu malgré un message de succès

**Ce qui ne va pas.** `NotificationService.envoyerEmail` absorbe les exceptions SMTP et les journalise. `poserQuestion` renvoie ensuite « Votre question a bien été envoyée » ; même son propre bloc d’erreur conserve cette réponse. Ce chemin ne persiste pas la demande. Dans le service de paiement, des e-mails sont aussi envoyés pendant une transaction métier sans reprise persistante de l’envoi.

**Conséquence.** Le visiteur pense avoir contacté l’institut alors que sa demande n’a pas été transmise. Sur les opérations transactionnelles, l’e-mail peut partir avant un échec ultérieur de commit, ou ne jamais être réessayé.

**Il fallait faire.** Enregistrer la demande avant de confirmer sa réception. Pour les notifications, enregistrer dans la transaction un événement d’envoi, traité ensuite avec reprise et statut visible : c’est le principe d’une outbox. À défaut de persistance, afficher un échec honnête au visiteur.

**Validation attendue.** SMTP indisponible : la demande est conservée et réexpédiable, ou une erreur est affichée ; aucune confirmation métier n’est envoyée pour une transaction annulée.

**Sources :** [PublicController.java:132](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/PublicController.java:132), [NotificationService.java:46](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/NotificationService.java:46), [PaiementService.java:140](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/PaiementService.java:140).

### A12 — La présence de tests ne garantit pas la sécurité des routes

**Ce qui ne va pas.** Les tests Angular ne démarrent pas ; `tsconfig.spec.json` est absent. Un test d’application attend encore « Hello, frontend ». De nombreux tests backend construisent directement les contrôleurs : ils peuvent vérifier les contrôles explicites mais ne traversent pas le filtre JWT, les règles d’URL ou les annotations de sécurité Spring. Les recherches sur le répertoire de tests n’ont pas trouvé de suite MockMvc couvrant cette matrice.

**Il fallait faire.** Restaurer une configuration de tests Angular complète, remplacer les assertions de squelette et ajouter des tests HTTP avec Spring Security actif. Garder les tests unitaires existants : ils couvrent déjà des règles métier utiles. Isoler la base de test et les e-mails, et ajouter quelques parcours navigateur de bout en bout.

**Validation attendue.** Exécution des suites sur un checkout propre ; tests 401/403/200 pour chaque famille sensible ; candidat A incapable de lire une pièce de B ; formateur hors périmètre refusé ; ancien JWT invalidé après récupération.

**Sources :** [tsconfig.json](F:/Real_projects/Inpg/LMS_Repo/frontend/tsconfig.json), [angular.json](F:/Real_projects/Inpg/LMS_Repo/frontend/angular.json), [app.spec.ts](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/app.spec.ts), [MessagerieControllerTest.java](F:/Real_projects/Inpg/LMS_Repo/backend/src/test/java/inpg/sn/lms/espace_etudiant/controller/MessagerieControllerTest.java), [LmsApplicationTests.java](F:/Real_projects/Inpg/LMS_Repo/backend/src/test/java/inpg/sn/lms/LmsApplicationTests.java).

### A13 — La chaîne CI ne correspond pas encore à une livraison complète

**Ce qui ne va pas.** Le workflow lance Maven mais ne construit ni ne teste Angular. Il fournit les variables PostgreSQL, sans définir les propriétés obligatoires JWT et SMTP nécessaires au chargement complet du contexte. Il impose `ddl-auto=update`, différent de `validate` en configuration applicative. Les jobs appelés déploiement n’exécutent qu’un `echo`.

**Conséquence.** La CI ne démontre pas que le produit complet est livrable. Le chargement du contexte sur un runner propre risque d’échouer avec ce workflow. Les résultats réels de GitHub Actions n’ont pas été consultés ; d’autres mécanismes de déploiement peuvent exister hors de ce fichier.

**Il fallait faire.** Jobs backend et frontend, propriétés de test non secrètes dédiées, SMTP simulé, migrations testées sur base éphémère avec validation du schéma, puis déploiement réel d’un artefact identifié suivi d’un contrôle de santé. Supprimer ou renommer les étapes factices et prévoir restauration/retour arrière vérifiés.

**Validation attendue.** Une modification cassant Angular bloque la livraison ; une migration incohérente échoue ; une version déployée est identifiable et son état vérifié.

**Source :** [build.yml:58](F:/Real_projects/Inpg/LMS_Repo/.github/workflows/build.yml:58), [étapes de déploiement:111](F:/Real_projects/Inpg/LMS_Repo/.github/workflows/build.yml:111).

### A14 — La dépendance de documentation API n’est pas alignée

**Ce qui ne va pas.** Le POM associe Spring Boot 4.1.0 à springdoc 2.2.0. La documentation officielle place springdoc 2.x avec Spring Boot 3 et springdoc 3.x avec Spring Boot 4.

**Il fallait faire.** Aligner cette dépendance sur une version compatible, puis tester la génération du schéma OpenAPI dans un environnement de développement sécurisé. Ne pas simplement ouvrir Swagger au public. L’incompatibilité déclarée est vérifiée ; aucun crash runtime dû à cette dépendance n’a été reproduit ici.

**Validation attendue.** Contexte de test démarré et document OpenAPI valide ; contrôle des permissions d’accès à la documentation.

**Sources :** [pom.xml:9](F:/Real_projects/Inpg/LMS_Repo/backend/pom.xml:9), [springdoc:136](F:/Real_projects/Inpg/LMS_Repo/backend/pom.xml:136), [matrice officielle springdoc](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).

### A15 — Les listes complètes empêcheront une montée en charge sereine

**Ce qui ne va pas.** Les candidatures et paiements sont chargés via `findAll()`, puis transformés ou filtrés en mémoire. Les relances parcourent toutes les candidatures. Le chargement Hibernate par lots réduit certaines requêtes supplémentaires mais ne remplace pas la pagination.

**Conséquence.** Temps de réponse, mémoire et durée des traitements croissent avec le volume. Il s’agit d’un risque structurel, pas d’une lenteur mesurée en production.

**Il fallait faire.** Filtrer et paginer en base, avec taille maximale ; utiliser des projections de liste et des agrégations pour les indicateurs ; traiter les exports et relances volumineux par lots. Mesurer sur un jeu représentatif avant d’ajouter cache ou infrastructure.

**Validation attendue.** Avec un jeu de données augmenté, une page ne charge que son lot ; les compteurs restent exacts ; une relance ne monopolise pas une requête HTTP jusqu’au dernier e-mail.

**Sources :** [CandidatureService.java:295](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/CandidatureService.java:295), [ApplicationController.java:66](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/ApplicationController.java:66), [PaiementService.java](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/PaiementService.java).

### A16 — L’intercepteur doit limiter les destinataires du jeton

**Ce qui ne va pas.** L’intercepteur attache `Authorization: Bearer …` à toute requête HttpClient dès qu’un token existe, sans vérifier l’origine ni le chemin API. Le token est stocké dans localStorage.

**Conséquence.** Un futur appel HttpClient vers un domaine tiers autorisant la requête pourrait recevoir le jeton. Aucun appel tiers exploitable ni aucune XSS n’ont été démontrés. Les images chargées via une balise `img` ne passent pas automatiquement par cet intercepteur.

**Il fallait faire.** N’ajouter le jeton qu’aux origines et chemins API approuvés, avec comparaison d’URL robuste. Étudier ensuite stockage en mémoire et renouvellement protégé, ou cookies HttpOnly avec une stratégie CSRF adaptée. Ne pas migrer aux cookies en conservant mécaniquement la désactivation actuelle de CSRF.

**Validation attendue.** URL API autorisée : jeton présent ; URL externe ou nom de domaine ressemblant : aucun jeton. Réinitialisation et déconnexion révoquent les accès conformément à A03.

**Sources :** [auth.interceptor.ts:12](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/core/auth/interceptors/auth.interceptor.ts:12), [authService.ts:20](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/core/auth/services/authService.ts:20). Pour les compromis de stockage et de révocation : [OWASP — gestion de session](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

### A17 — Le code et la navigation comportent des restes à nettoyer

**Ce qui ne va pas.** La recherche de l’en-tête admission n’a ni liaison de valeur ni gestionnaire d’action. L’ancien composant `ResponsableAdmissionSettingsComponent` contient des utilisateurs fictifs et plusieurs réglages en mémoire ; aucune référence d’import vers ce composant n’a été trouvée dans les fichiers TypeScript. La route actuelle mène à `parametres`, un autre composant. La connexion conserve aussi des méthodes OTP alors que son chemin normal connecte et redirige immédiatement.

L’organisation concentre encore beaucoup de responsabilités : environ 1 648 lignes non vides dans `EspaceEtudiantController`, 947 dans `PedagogieScolariteController`, 886 dans `AdminRecrutementController` et 681 dans le service frontend de recrutement. La taille seule n’est pas une faute, mais elle rend les frontières d’autorisation et de transaction difficiles à suivre.

**Il fallait faire.** Retirer la recherche inerte ou la brancher avec un périmètre annoncé ; supprimer les anciens composants après vérification des références et archives nécessaires ; découper les contrôleurs par cas d’usage. Les changements métier et transactions doivent vivre dans des services, avec DTO explicites, plutôt que dans de très grands contrôleurs.

**Validation attendue.** Aucun contrôle visible sans effet ; compilation et tests inchangés après retrait du code mort ; chaque opération sensible possède un seul chemin métier de référence.

**Sources :** [recherche admission:56](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/recrutement/responsable-admission/layout/responsable-admission-layout.component.html:56), [ancien settings](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/recrutement/responsable-admission/settings/responsable-admission-settings.component.ts), [routes:278](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/app.routes.ts:278), [login.ts](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/auth/login/login.ts).

## 4. Ce qui est utile et doit être conservé

| Fonctionnalité | Pourquoi la conserver | Amélioration ciblée |
|---|---|---|
| Catalogue et sessions de candidature | Point d’entrée concret pour les futurs apprenants | Clarifier le lien entre `Program` du recrutement et `Formation` de la pédagogie |
| Brouillon serveur de candidature | Évite de recommencer un dossier après interruption | Sauvegarde visible, reprise claire et limites de taille |
| Pièces, conformité et critères d’admission | Soutiennent le travail réel des agents | Versionner les pièces et tracer les décisions |
| Tests académiques, entretiens et délibérations | Correspondent au processus de sélection | Montrer clairement la prochaine action et les prérequis |
| Déclaration et validation administrative des règlements | Utiles si les encaissements sont vérifiés hors plateforme | Supprimer la fausse saisie bancaire ; conserver preuve et auteur |
| Promotions, formations, cours et habilitations | Cœur du LMS et de l’isolation des données | Garder une relation de référence unique pour les autorisations |
| Supports, calendrier, présence, notes et progression | Accompagnent l’apprentissage quotidien | Réunir l’accès autour de la fiche cours |
| Relevés délivrés avec données figées | Préservent la valeur d’un document déjà remis | Ajouter un circuit de remplacement/révocation si nécessaire |
| Certificats et vérification publique restreinte | Permettent une vérification par un tiers | Gérer explicitement révocation et rotation des clés |
| Journal des étapes de candidature | Donne une trace datée des décisions | Étendre ce principe aux rôles, notes et validations sensibles |
| Annonces, forum et messagerie | Répondent à trois usages distincts : diffusion, échanges collectifs, privé | Entrée « Communication » commune, avec sous-rubriques explicites |
| Entreprises et demandes de formation/devis | Utile à la formation continue | Garder dans un espace dédié, hors de la navigation étudiant |
| Accréditation écoles et évaluateurs | Domaine métier distinct déjà structuré | Préserver son cloisonnement et son vocabulaire propre |
| Alumni et enquêtes | Utiles au suivi des diplômés et à la qualité | Déployer selon les campagnes et les responsables disponibles |

**Points techniques positifs vérifiés :** BCrypt ; mots de passe masqués à la sérialisation de `User` ; secret JWT sans valeur de repli ; contrôles serveur au-delà des guards Angular ; fichiers privés hors du répertoire statique public ; protection contre les chemins sortant du répertoire ; montant du paiement calculé côté serveur ; migrations Flyway ; gestion centralisée des exceptions ; services de périmètre des formateurs ; relevés figés et signature HMAC de certificat.

Références : [SecurityConfig](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/config/SecurityConfig.java), [WebConfig](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/config/WebConfig.java), [PerimetreFormateurService](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/espace_etudiant/service/PerimetreFormateurService.java), [ReleveDelivranceService](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/espace_etudiant/service/ReleveDelivranceService.java), [JournalEtapeService](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/JournalEtapeService.java), [CertificatSignatureService](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/espace_etudiant/service/CertificatSignatureService.java).

## 5. Ce qu’il faut retirer, simplifier ou différer

Il n’existe pas de statistiques d’usage dans cet audit permettant de déclarer qu’un module métier entier est inutile. Les propositions suivantes sont donc des choix de produit motivés, à comparer aux obligations de l’établissement ; elles ne justifient pas la suppression de données historiques.

| Élément | Proposition | Motif |
|---|---|---|
| Champs carte/CVC sans prestataire | Retirer dès maintenant | Collecte sans utilité et parcours trompeur |
| Recherche globale inactive | Retirer ou connecter | Une commande sans résultat dégrade la confiance |
| Ancien écran `settings` non référencé | Nettoyer après dernière vérification des références | Doublon de code contenant des réglages fictifs |
| Méthodes OTP inutilisées du login | Retirer ou remplacer par un vrai challenge serveur | Ne pas entretenir une fausse impression de double authentification |
| Nom « simuler succès » dans un processus devenu manuel | Renommer selon le métier réel | Distinguer démonstration, encaissement vérifié et future passerelle |
| Nombreuses entrées de navigation | Regrouper par tâches | Un utilisateur cherche une action, pas une organisation de classes |
| Photothèque | Conserver comme option de cours/activité | Utile pour les ateliers, moins prioritaire que présence et évaluations |
| Rendez-vous et activités du personnel | Garder dans un groupe « Services » selon les rôles | Ne doivent pas encombrer l’espace d’apprentissage quotidien |
| Personnalisation décorative des tableaux de bord | Différer | Moins utile qu’une liste fiable des actions à traiter |
| Masquage des anciens sujets selon la date de création du compte | Revoir avec les enseignants | Un inscrit tardif peut manquer un historique pédagogique utile ; préférer l’appartenance à la formation/promotion pour l’accès, et les préférences pour les notifications |

La règle d’ancienneté est visible dans [ForumVisibiliteService.java:73](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/espace_etudiant/service/ForumVisibiliteService.java:73). C’est une recommandation fonctionnelle, pas l’affirmation d’une fuite de données.

### Navigation proposée

- **Candidat :** mon dossier, pièces à compléter, règlement, convocations, décisions.
- **Étudiant :** accueil, mes cours, calendrier, résultats et documents, communication.
- **Formateur :** mes cours, séances et présences, évaluations, suivi des apprenants, communication.
- **Admission :** dossiers à traiter, sessions et critères, sélection, règlements, rapports.
- **Pédagogie :** formations et promotions, enseignements, apprenants et formateurs, résultats et certification, qualité.
- **Autres espaces :** entreprises, alumni et accréditation conservent leurs menus métier ; profil, aide et rendez-vous restent à des positions cohérentes.

## 6. Fonctionnalités à ajouter ou à compléter

| Priorité | Proposition | Valeur attendue | Critère de réussite |
|---|---|---|---|
| P1 | Gestion des sessions et MFA des comptes sensibles | Protection des comptes et récupération fiable | Révocation testée ; second facteur vérifié avant émission d’un accès privilégié |
| P1 | Matrice des permissions automatisée | Éviter les régressions entre espaces | Tests par rôle, ressource propre et ressource étrangère |
| P1 | Journal d’audit transversal | Expliquer qui a changé une note, un rôle ou une validation | Auteur, date, cible, ancien/nouvel état et motif ; accès restreint |
| P1 | Version des dossiers et demandes de complément | Fiabiliser les décisions d’admission | Une décision reste rattachée aux pièces réellement examinées |
| P1 | Réception persistante et reprise des notifications | Éviter les demandes perdues | Échecs visibles et réessayables, sans doublon |
| P1 | Procédure de sauvegarde/restauration base + fichiers | Pouvoir récupérer le service | Exercice documenté de restauration sur un environnement isolé |
| P2 | File « Mes actions à traiter » par rôle | Réduire les menus et oublis | Chaque action a une échéance, une raison et un lien direct |
| P2 | Historique des tentatives de paiement et rapprochement | Clarifier les preuves d’encaissement | Référence, tentative, preuve et validateur conservés |
| P2 | Version/publication des notes et documents | Éviter une modification silencieuse après diffusion | Correction tracée, validation et remplacement explicites |
| P2 | Quotas et gestion du cycle de vie des documents | Maîtriser stockage et conservation | Fichiers orphelins détectés ; règles décidées par type de dossier |
| P2 | Pagination, recherche et exports ciblés | Garder le système utilisable avec plus de données | Filtres identiques entre liste et export ; volumes bornés |
| P2 | Suivi opérationnel des échecs | Identifier rapidement la cause d’un problème | Santé API/stockage et échecs de jobs visibles, sans secrets dans les logs |
| P3 | Import/export de calendrier et rappels maîtrisés | Réduire les oublis de séance | Pas de doublons, fuseau horaire cohérent, préférences de réception |

Pour les sauvegardes et la supervision, l’ajout concerne au minimum leur **preuve de fonctionnement** : cet audit ne permet pas d’exclure l’existence de services déjà configurés chez l’hébergeur. Pour le journal, les documents et notifications, il s’agit d’étendre les mécanismes existants, pas de développer un deuxième système concurrent.

Je ne recommande pas, à ce stade, de chatbot, de classement public des étudiants, de réseau social généraliste ou de microservices. Aucun besoin démontré dans les parcours inspectés ne justifie de leur donner la priorité.

## 7. Organisation technique recommandée

Conserver Angular et Spring Boot et faire évoluer le backend vers un monolithe organisé par domaines. Le projet a déjà cette base ; il faut surtout renforcer les limites de responsabilité.

| Couche | Responsabilité souhaitée |
|---|---|
| Contrôleur | Lire un DTO validé, identifier l’utilisateur, appeler un cas d’usage, retourner une réponse explicite |
| Service métier | Contrôler autorisation sur la ressource, transitions et transaction |
| Repository | Requêtes filtrées, projections, pagination et verrous nécessaires |
| Documents | Stockage, état de traitement, contrôles de contenu, accès, compensation et suppression |
| Notifications | Événements persistés, envoi après commit, reprises et idempotence |
| Frontend partagé | Navigation, tableaux, formulaires, états vide/chargement/erreur, permissions d’affichage |

`Program` et `Formation` ne doivent pas être fusionnés aveuglément : ils peuvent représenter des étapes différentes de l’offre. Il faut documenter leur correspondance et garantir qu’une admission se rattache sans ambiguïté à une formation/promotion. Références : [Program.java](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/entity/Program.java), [SessionCandidature.java:75](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/entity/SessionCandidature.java:75).

Pour le design, réutiliser les composants UI partagés et les jetons Tailwind existants. La maquette séparée produite précédemment est une piste visuelle ; son CSS ne doit pas devenir une deuxième charte concurrente dans l’application.

## 8. Plan d’action proposé

Les lots sont ordonnés par dépendances. Une estimation calendaire sérieuse nécessite de connaître l’équipe, les contraintes de recette et l’état réel du déploiement.

### Lot 1 — Fermer les risques immédiats

Traiter A01 à A05, restreindre l’intercepteur A16 et réparer l’exécution des tests A12. Responsable principal : backend pour les comptes/sessions, frontend pour la saisie bancaire et la configuration de tests. Le responsable d’exploitation confirme l’état des comptes de démonstration et des secrets sur les instances réelles.

**Sortie attendue :** pas de compte de démonstration en profil production, reset borné et atomique, anciens JWT révoqués après récupération, aucune saisie bancaire factice, tests exécutables.

### Lot 2 — Rendre les opérations métier fiables

Traiter A06 à A11 : rôles transactionnels, pièces versionnées, fichiers analysés et réconciliés, paiements historisés, notifications persistantes. L’admission décide les étapes où un dossier peut être complété ; la pédagogie valide les règles de publication/correction des résultats.

**Sortie attendue :** une action réussie correspond à un état réellement enregistré ; une erreur peut être comprise et reprise ; les opérations sensibles sont tracées.

### Lot 3 — Fiabiliser la livraison et la charge

Traiter A13 à A15 : CI complète, dépendances alignées, tests HTTP de sécurité, pagination, sauvegarde/restauration vérifiée et contrôles après déploiement.

**Sortie attendue :** une livraison défectueuse est bloquée, une version est identifiable, la restauration est éprouvée et les listes ne chargent pas toute la base.

### Lot 4 — Simplifier l’expérience

Traiter A17, regrouper les menus et harmoniser les composants. Mettre en place la file d’actions par rôle et seulement ensuite les extensions utiles retenues avec les métiers.

**Sortie attendue :** chaque rôle accède directement à ses tâches, les contrôles visibles fonctionnent, les parcours principaux restent compréhensibles sur mobile et au clavier.

## 9. Recette minimale avant ouverture réelle

1. Inscription, connexion, récupération et révocation des sessions testées avec entrées valides et invalides.
2. Tests d’accès croisé entre candidats, étudiants, formateurs et organisations ; fichier privé inaccessible anonymement.
3. Candidature complète, complément, rejet et admission avec pièces et historique cohérents.
4. Déclaration de règlement, validation, refus et nouvelle tentative ; aucun succès provoqué par le candidat.
5. Parcours cours → séance → présence → note → relevé/certificat, avec accès hors périmètre refusé.
6. Échec SMTP et stockage : retour utilisateur honnête, reprise et absence de données orphelines silencieuses.
7. Exécution de toutes les suites dans un environnement isolé, puis restauration base/fichiers et vérification de la version déployée.

**Décision recommandée :** conserver le périmètre métier utile, corriger les risques P0/P1, puis simplifier l’interface autour des tâches quotidiennes. Les preuves actuellement disponibles permettent de prioriser ce travail ; elles ne permettent pas de déclarer la plateforme prête pour la production.

## 10. Rapprochement avec le TDR « Mise en place du LMS — V02 »

Le TDR, daté de mars 2025, définit un portail multi-rôles et dix domaines métier : recrutement, étudiants, alumni, bibliothèque/SIGB, e-learning, accréditation, qualité, entreprises, activités du personnel et rendez-vous. Le tableau ci-dessous ne remplace pas une recette avec les équipes métier : il indique ce que le dépôt permet d’établir.

**Lecture des états :**

- **Couvert dans le code** : routes et mécanismes concrets identifiés ; à valider par recette métier et UI.
- **Partiel** : le socle existe mais une exigence du TDR reste incomplète, simulée ou insuffisamment démontrée.
- **Non trouvé dans le dépôt** : aucun modèle, route ou connecteur correspondant n’a été identifié ; cela ne préjuge pas d’un service exploité hors code.

| Domaine et exigences du TDR | État constaté | Éléments identifiés | Écart à fermer / décision à prendre |
|---|---|---|---|
| Comptes, rôles et gestion des utilisateurs | Partiel | Authentification JWT, rôles serveur, création de formateurs, profils candidat/étudiant/alumni/entreprise/évaluateur. | Appliquer A01 à A04 et A16 : pas de compte de démonstration en réel, récupération protégée, révocation des sessions, matrice de permissions et audit des changements de rôle. |
| Recrutement : compte candidat, brouillon, pièces, statut, critères, tests, entretiens, décisions, exports et statistiques | Couvert dans le code, sous réserve de recette | Brouillon, soumission, pièces, pré-sélection, tests, entretiens, liste d’attente, journal QMS-009, exports CSV/XLSX/PDF et indicateurs sont présents. | Figer les pièces examinées et versionner les décisions (A07) ; paginer listes et exports (A15) ; tester tous les statuts et les décisions collectives avec le service admission. |
| Recrutement : paiement par prestataire, notifications e-mail/SMS et confirmation INPG | Partiel — **bloquant avant encaissement réel** | Une tentative de paiement et une validation administrative existent ; le candidat ne peut plus la passer lui-même à succès. | Aucune passerelle réelle n’est connectée et l’API mentionne explicitement la simulation. Ne jamais demander carte/CVC sans prestataire ; choisir le processus manuel avec preuve, ou intégrer un prestataire avec webhook vérifié, idempotence et rapprochement. Les notifications SMS doivent être raccordées à un fournisseur ou retirées de la promesse fonctionnelle. |
| Étudiants : cours autorisés, calendrier, annonces, forum, photothèque, promotions, notes et relevés | Couvert dans le code, sous réserve de recette | Cours, supports, séances, présences, annonces, forum, photothèque, promotions, notes, relevés PDF et contrôles de périmètre formateur sont présents. | Remplacer le repli frontend vers des formations fictives lors d’une erreur API ; organiser l’archivage et la recherche par promotion ; vérifier mobile, clavier, faible débit et les droits de chaque profil. |
| Certificats automatiques, référence unique, QR, signature et vérification | Partiel | Génération PDF, référence, QR, signature HMAC et endpoint de vérification publique restreinte sont présents. | Formaliser la valeur juridique de la signature/cachet, la révocation et le remplacement d’un certificat ; faire valider le modèle par l’administration et réaliser une recette anti-fraude. |
| Alumni : profil, situation professionnelle, relevés, forum et documents | Couvert dans le code, sous réserve de recette | Profil alumni, situations, métriques, relevés, enquêtes, forum et documents sont implémentés. | Définir la politique de conservation, consentement et relance des enquêtes ; tester l’accès après changement de statut étudiant → alumni. |
| Bibliothèque et intégration SIGB | Non trouvé dans le dépôt | Une annonce de bibliothèque existe dans les données pédagogiques, sans intégration métier identifiée. | Prioriser un atelier avec la bibliothèque : SIGB cible, identifiant commun, données échangées, SSO, liens profonds, disponibilité et responsabilités. Ne pas annoncer l’intégration avant un contrat d’interface et une recette. |
| E-learning : parcours, suivi individuel/groupe, échanges et réunions virtuelles | Partiel | Supports de cours, séances, présence, notes, forums/messagerie et liens de visioconférence sont présents. | Un lien de réunion n’est pas une intégration de classe virtuelle : choisir et intégrer le fournisseur (Teams, Jitsi, Zoom ou autre), définir les droits, présences et enregistrements. Compléter le suivi de progression par des indicateurs métier validés, plutôt que des données de démonstration. |
| Accréditation : dépôt structuré, critères, évaluateur, décisions, notifications et liste d’écoles | Couvert dans le code, sous réserve de recette | Espaces école et évaluateur, pièces, critères, prises en charge, avis, décisions et liste d’écoles accréditées sont présents. | Tester l’indépendance des rôles, le caractère traçable des critères/décisions et les règles de publication. Documenter la procédure d’appel ou de complément demandée par la gouvernance. |
| Qualité : évaluations, relances, tableau de bord et rapports consolidés | Partiel | Évaluations de cours, rappels planifiés et services de rapport qualité sont présents. | Faire valider questionnaires, anonymat, calendrier post-formation et indicateurs par la direction qualité. Prévoir l’export consolidé, seuils d’alerte et actions correctives réellement suivies. |
| Entreprises : demande, catalogue, offre sur mesure, devis, suivi et messagerie sécurisée | Couvert dans le code, sous réserve de recette | Catalogue, demandes, fiches techniques, transmission/ajustements et messages entreprise–pédagogie sont présents. | Définir le cycle de validation d’un devis, ses versions, signatures et délais ; tester strictement l’étanchéité entre entreprises. |
| Activités du personnel : saisie mensuelle et rapports départementaux/consolidés | Partiel | Départements, activités et endpoints de rapports départemental/global sont présents. | Le seul traitement planifié identifié concerne les rappels d’évaluation ; planifier et journaliser la production mensuelle, les relances, le gel des périodes et la diffusion des rapports. |
| Rendez-vous : créneaux, demande à distance/présentiel, acceptation/refus/alternative | Couvert dans le code, sous réserve de recette | Créneaux, demandes, annulation, réponses et contre-propositions sont présents. | Ajouter notifications fiables, fuseau horaire, prévention des doubles réservations et éventuellement synchronisation de calendrier ; tester les conflits concurrents. |
| Déploiement, formation, assistance, garantie et transfert de compétences | Non démontrable par le code seul | Le dépôt contient une CI backend et des documents, mais les étapes de déploiement de préproduction/production sont des messages factices. | Produire le plan de déploiement, dossier d’exploitation, manuel utilisateur et administrateur, formation, SLA/support, plan de sauvegarde-restauration et critères de réception. Ces livrables sont indispensables pour répondre au TDR, même si le code est fonctionnel. |

### Priorités additionnelles imposées par le TDR

1. **Avant toute ouverture publique :** sécuriser les comptes et le paiement, finaliser les parcours de candidature, et faire une recette rôle par rôle avec des données non réelles.
2. **Avant la promesse commerciale :** décider explicitement de l’intégration SIGB, de la solution de visioconférence et du prestataire de paiement/SMS. Ce sont des dépendances externes, pas de simples écrans à terminer.
3. **Avant la réception finale :** remettre les tests frontend en état, rendre la CI réellement déployante, prouver restauration base + fichiers, puis produire les manuels et la procédure de support/transfert.
4. **Pendant la recette métier :** faire signer une matrice de conformité détaillée par exigence du TDR, avec responsable, scénario, preuve attendue, résultat et date. Les états « couvert dans le code » de ce rapport ne constituent pas une acceptation fonctionnelle.

### Grille de réception à créer

Pour chaque exigence du TDR, conserver dans un tableau de recette : `référence TDR`, `rôle`, `données de test`, `scénario`, `résultat attendu`, `preuve (capture/export/journal)`, `anomalie`, `responsable`, `statut` et `date de validation`. Cette traçabilité évitera qu’un parcours partiellement simulé soit considéré comme livré parce que son écran existe.

**Références code pour ce rapprochement :** contrôleurs de recrutement, d’espace étudiant, alumni, entreprise, accréditation, personnel et rendez-vous sous `backend/src/main/java/inpg/sn/lms/` ; routes Angular sous `frontend/src/app/features/` ; [PaiementService.java:79](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/PaiementService.java:79), [CandidatController.java:557](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/CandidatController.java:557), [PersonnelActiviteController.java:197](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/personnel/controller/PersonnelActiviteController.java:197), [build.yml](F:/Real_projects/Inpg/LMS_Repo/.github/workflows/build.yml).
