# Authentification INPG

État vérifié le 16 septembre 2026. Ce document décrit le code local ; les anciens rapports et messages concernant un OTP de connexion ne décrivent plus le flux actif.

## Composants et requêtes

- Angular : core/auth/services/authService.ts, intercepteur HTTP et gardes. Les formulaires utilisent core/auth/validators/password-policy.ts pour la validation.
- API : AuthController, UserService, PasswordPolicy, OtpService, LoginAttemptService.
- Spring Security : SecurityConfig, JwtRequestFilter, JwtUtils. Les règles de rôle et de propriété des ressources restent vérifiées côté serveur.

1. POST /api/auth/login reçoit email et mot de passe. L'adresse est normalisée ; AuthenticationManager vérifie le hash BCrypt. Au maximum dix demandes de connexion par adresse et par fenêtre de quinze minutes, réussies ou non, sont autorisées par instance. Le dépassement répond 429, y compris pour une adresse inconnue.
2. Le serveur renvoie un JWT signé et un profil sans mot de passe. Le JWT contient un identifiant unique (jti), le username, les rôles, la version de session et les dates d'émission/expiration. La durée reste pilotée par JWT_EXPIRATION (100 minutes par défaut).
3. Angular conserve le jeton dans localStorage et le profil dans un signal en mémoire. L'intercepteur ajoute Authorization: Bearer uniquement aux URL de même origine ET dans le chemin de l'API configurée.
4. Le filtre vérifie signature, expiration, sujet et sessionVersion. Il charge les rôles actuels en base ; les rôles embarqués dans le JWT ne décident pas des autorisations. Un compte disparu ou un jeton rejeté laisse la requête anonyme : les routes protégées répondent 401. Une permission insuffisante répond 403.
5. Le frontend efface une session rejetée par l'API. Un 401 externe ou la réponse tardive d'une ancienne session ne détruit pas la session courante. Une panne réseau conserve le jeton et annule la navigation protégée, sans boucle de redirection.

L'inscription publique crée un compte sans rôle métier et ouvre sa session. Le type d'inscription ne confère aucun privilège. Le login ne demande pas de second facteur : les OTP servent à récupérer le mot de passe.

## Mots de passe et révocation

Tous les mots de passe choisis passent par la même politique : au moins huit caractères Unicode, pas uniquement des espaces, sans longueur maximale (au-delà de 72 octets, limite de BCrypt, le mot de passe est condensé en SHA-256 avant hachage : voir `PasswordConfig`). Les comptes existants peuvent toujours se connecter avec leur ancien mot de passe ; la règle s'applique à une création ou un changement.

POST /api/auth/change-password exige le mot de passe actuel. Le nouveau hash et l'incrément de sessionVersion sont écrits ensemble par une requête SQL atomique. Le frontend efface alors sa session et demande une nouvelle connexion. La récupération révoque également les JWT existants.

POST /api/auth/logout incrémente atomiquement la version en base : **toutes les sessions du compte** sont révoquées. Cette opération ne réenregistre pas un ancien hash chargé en début de requête. Angular nettoie sa session même si le réseau échoue ; dans ce cas, la révocation serveur ne peut pas être garantie et le jeton reste potentiellement valide jusqu'à expiration.

La colonne session_version existe déjà via V43__sessions_et_roles_initialises.sql. Aucun changement de migration n'est nécessaire pour cette correction ; ne pas modifier une migration appliquée.

## Récupération par OTP

POST /api/auth/send-otp reçoit l'email. Il applique les mêmes compteurs aux adresses connues et inconnues, mais envoie un email seulement si le compte existe. Le corps et le statut de succès restent identiques, même lors d'une panne SMTP ; celle-ci est journalisée sans adresse, code ni identifiants. Le délai de réponse SMTP peut encore différer : ce mécanisme n'offre pas une uniformité temporelle.

Le code SecureRandom à six chiffres expire après cinq minutes. Renvoi au plus une fois par minute. Après cinq erreurs, le code est bloqué jusqu'à la fin de sa durée de validité, y compris si un renvoi est demandé. La consommation est atomique : deux validations concurrentes ne réussissent pas ensemble. Un échec SMTP invalide le code sans supprimer le délai de renvoi.

POST /api/auth/reset-password reçoit email, code et newPassword en une seule requête. Le mot de passe est validé avant de consommer le code. L'ancien endpoint verify-otp ne fait plus partie du flux. Le changement du hash et la révocation sont persistés ensemble ; une panne de base après consommation du code exige d'en demander un nouveau.

## Configuration et limites opérationnelles

JWT_SECRET, les identifiants PostgreSQL et SMTP viennent de l'environnement ou de backend/.env non versionné. Ne pas mettre de secrets réels dans les tests. Les JWT utilisent les octets UTF-8 du secret configuré, sans décodage Base64 supplémentaire. HTTPS doit être assuré par le déploiement.

Les caches OTP et tentatives de connexion sont en mémoire, bornés à 10 000 adresses chacun et purgés des entrées expirées. Un redémarrage efface les OTP et compteurs. Plusieurs instances nécessitent un stockage partagé et une limitation au niveau du proxy pour une protection globale contre les abus ; le limiteur par adresse permet aussi de bloquer temporairement une adresse ciblée.

Le jeton demeure accessible au JavaScript dans localStorage. Cette correction ne remplace pas la protection contre les XSS par un cookie HttpOnly. Aucun refresh token n'est émis : une expiration exige une nouvelle connexion. Une évolution vers des cookies HttpOnly demanderait un flux CSRF/CORS dédié. Aucun MFA obligatoire n'est ajouté.

## Vérification

Tests unitaires sans base de données :

- Backend : UserServiceTest, PasswordPolicyTest, OtpServiceTest, LoginAttemptServiceTest, AuthControllerTest, JwtUtilsTest, JwtRequestFilterTest.
- Frontend : tests sous core/auth, tests des écrans d'authentification, compilation Angular de production.

Ne pas lancer LmsApplicationTests sur une base partagée : son contexte Spring peut appliquer les migrations Flyway.

Validation du 16 septembre 2026 : 23 tests backend et 19 tests frontend réussis ; build Angular de production réussi. Un avertissement de budget CSS subsiste dans responsable-admission-statistiques.component.css, hors de ces modifications. Les tests backend ont été exécutés sans démarrage du contexte Spring ni accès à une base partagée.
