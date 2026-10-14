# Audit complet de la plateforme — 2026-10-14

Périmètre : les 14 modules du serveur (384 routes) et les 12 espaces du frontend, sur le code de `develop` (`40df8c7`).
Les audits ciblés précédents restent valables et ne sont pas refaits : [parcours candidat](AUDIT_PARCOURS_CANDIDAT_2026-10-13.md),
[admission](AUDIT_ADMISSION_STATUTS_ET_COMPLETUDE_2026-10-13.md), [responsable admission](AUDIT_RESPONSABLE_ADMISSION_2026-10-12.md),
[recrutement / TDR](AUDIT_RECRUTEMENT_TDR_2026-09-13.md), [technique et fonctionnel](AUDIT_TECHNIQUE_FONCTIONNEL_2026-09-13.md).

## Méthode

Chaque constat ci-dessous a été **mesuré**, pas déduit de la lecture :

| Axe | Comment |
|---|---|
| Contrat frontend ↔ serveur | extraction automatique des 366 appels HTTP du frontend et des 384 routes du serveur, rapprochement dans les deux sens |
| Cloisonnement par rôle | matrice réelle : 8 comptes × 13 espaces + accès anonyme, jetons forgés, CORS |
| Accès croisés (IDOR) | 21 tentatives réelles de lire la donnée d'un autre en changeant l'identifiant de l'URL |
| Parcours réels | navigateur (Playwright) : **106 pages** visitées pour 9 profils, en bureau puis en largeur téléphone (390 px) — erreurs serveur, erreurs JavaScript, textes « undefined/NaN », noms techniques affichés, titres, débordements |
| Installation neuve | le serveur démarré sur une **base vierge**, en configuration démonstration puis production |
| Données | contraintes de la base ⇄ énumérations Java ; chaîne promotion → formation → cours → formateur ; comptes incohérents |
| Dépendances, secrets, code | `npm audit`, versions Java, secrets versionnés, dette, charte graphique, couverture de tests |

Suites de tests au moment de l'audit : **581 tests serveur et 285 tests frontend, tous verts** (hors `LmsApplicationTests`).

## Synthèse

La plateforme est saine sur l'essentiel : le cloisonnement entre rôles tient partout (aucune fuite sur la matrice
8 × 13), les jetons forgés sont refusés, les pièces des candidats ne sont pas exposées, le frontend et le serveur
parlent le même contrat (364 appels sur 366). Mais **deux défauts empêchent une installation neuve** de fonctionner,
et **une élévation de privilèges** reste ouverte.

| Gravité | Nombre | À traiter |
|---|---|---|
| Critique | 2 | avant toute nouvelle installation (production, nouveau poste) |
| Élevée | 4 | avant la mise en production |
| Moyenne | 6 | prochain cycle |
| Faible | 7 | au fil de l'eau |

## Critique

### C-1 — Sur une base neuve avec données de démonstration, le serveur ne démarre pas
**Mesure** : serveur lancé sur une base vierge avec `APP_DEMO_DATA_ENABLED=true` (valeur de `docker-compose.yml` pour le poste local) →
`Application run failed`, `violates check constraint "sessions_candidature_status_check"`.
**Cause** : `DataInitializer` écrit les statuts de session `"OUVERT"` et `"CLOTURE"` ; la base n'accepte que `OUVERTE`, `CLOTUREE`…
Invisible en recette parce que l'initialiseur ne tourne que sur une base vide. `DashboardController#creerSession`
(route inutilisée) écrit le même `"OUVERT"`.
**Impact** : tout nouveau poste de développement, toute base de démonstration recréée, refusent de démarrer.
**Correctif** : écrire `OUVERTE`/`CLOTUREE` (idéalement une énumération Java pour ce champ, comme les autres statuts) ; test de démarrage sur base vierge.

### C-2 — Sur une base neuve en production, personne ne peut se connecter
**Mesure** : même essai avec `APP_DEMO_DATA_ENABLED=false` → le serveur démarre, 64 migrations appliquées, 22 formations
publiées… et **zéro compte**. Seul un responsable admission peut attribuer un rôle ; aucun n'existe.
**Impact** : la mise en production exige une insertion SQL à la main (mot de passe haché compris).
**Correctif** : création du premier administrateur au démarrage à partir de variables d'environnement (e-mail + mot de passe
initial à changer à la première connexion), idempotente.

## Élevée

### E-1 — Le responsable admission peut donner n'importe quel rôle à n'importe qui (C3 du 12/10, toujours ouvert)
**Mesure** : `PUT /api/admin/users/{id}/roles` avec `ROLE_RESPONSABLE_PEDAGOGIQUE` sur un compte jetable → **200**.
Aucune liste de rôles autorisés, aucune protection contre la modification de son propre compte.
**Correctif** : liste blanche (candidat, étudiant, formateur), refus sur son propre compte et sur les comptes de responsables ;
à terme, un rôle d'administration distinct.

### E-2 — Aucune limitation de débit hors connexion
Seules la connexion et l'envoi de code de réinitialisation sont limités (`LoginAttemptService`). Ouverts sans limite :
inscription (création de comptes en masse), question publique (e-mails envoyés à l'INPG), **vérification de certificat**
(balayage des références, 8 caractères hexadécimaux — déjà signalé dans `CLAUDE.md`).
**Correctif** : limiteur par adresse IP sur ces trois routes.

### E-3 — Le secret qui signe les certificats est, par défaut, le secret des jetons de connexion
`certificat.signature.secret=${CERTIFICAT_SIGNATURE_SECRET:${jwt.secret}}`. Changer le secret JWT (rotation, fuite)
**invaliderait la signature de tous les certificats déjà imprimés**, et un même secret sert deux usages.
**Correctif** : rendre `CERTIFICAT_SIGNATURE_SECRET` obligatoire et distinct (le démarrage échoue s'il manque).

### E-4 — Angular 21.2.18 : 4 alertes « élevées » en production
XSS via l'i18n, contournement de la désinfection par liaisons d'hôte, ambiguïté de cache `HttpTransferCache`. L'exposition réelle est faible
(ni i18n Angular ni rendu serveur ici), mais le correctif est une simple version de maintenance de la même branche (> 21.2.19).

## Moyenne

### M-1 — Un étudiant peut lire la fiche et la liste des supports de n'importe quel cours
**Mesure** : `test.etudiant` (formation 1) obtient **200** sur `/api/espace-etudiant/cours/2` et `/cours/2/supports` (formation 2) —
titres, descriptions, liens externes. Les routes « scolarité » équivalentes (`/scolarite/mes-cours/2`) refusent bien (403) et le
**téléchargement** reste cloisonné (404). `/espace-etudiant/cohortes` liste aussi toutes les promotions.
19 des 21 tentatives d'accès croisé sont correctement refusées (forum, certificats, notes, présences, dossiers de candidature…).
**Correctif** : appliquer le périmètre de l'étudiant à ces deux routes (ou les retirer au profit de `/scolarite/mes-cours`).

### M-2 — Photothèque : trois photos de démonstration ne s'affichent jamais
**Mesure** : 404 sur `/photheque/photos/1..3/fichier` (étudiant et formateur). **Cause** : `DataInitializer` enregistre
`demo-photo-ds.png` au lieu de `photos/demo-photo-ds.png`. Visible à chaque démonstration.

### M-3 — Quatre cours sans formateur dans une formation qui a des étudiants
Formation MPG : *Géologie des réservoirs*, *Techniques de forage*, *Production et traitement*, *Sécurité et environnement* —
aucun formateur affecté, 2 étudiants inscrits. Aucun support, aucune séance, aucune note n'y est possible (chaîne d'accès rompue,
signalée en rouge dans l'espace pédagogique). Donnée à corriger, pas du code.

### M-4 — Huit écrans débordent sur téléphone
Défilement horizontal à 390 px : pédagogie (cours, étudiants, formateurs, certificats, qualité), évaluateur (demandes, fiche),
annonces étudiantes. Les espaces candidat, admission, alumni, formateur et les pages publiques passent.

### M-5 — Couverture de tests très inégale
Serveur : accréditation 2 classes de test pour 34 classes, rendez-vous 1/15, recrutement des formateurs 3/31.
Frontend : **aucun test** pour l'accréditation, l'espace alumni, l'espace entreprise, le personnel, les rendez-vous ; 1 pour la pédagogie (16 fichiers).

### M-6 — L'accueil renvoie vers une page « Écoles accréditées » réservée aux comptes
Choix produit du 2026-08-25 (liste réservée aux connectés), mais le lien de l'accueil public mène le visiteur à l'écran de connexion
sans explication. Retirer le lien de l'accueil, ou rouvrir la liste (le TDR la voulait publique).

## Faible

- **F-1 Code mort.** Frontend : `appliquerFiltreAutomatique` et `getUserByEmail` visent des routes qui n'existent pas (jamais appelées).
  Serveur : `DashboardController` (5 routes) et `SettingsController` (CRUD des programmes, 4 routes) ne sont appelés par aucun écran ;
  le second lie directement l'entité JPA au corps de la requête. À supprimer.
- **F-2 Titres de page.** Pas de `<h1>` visible : liste des candidatures (admission), suivi du candidat ; sur mobile, connexion,
  inscription, mot de passe oublié.
- **F-3 Charte.** 23 classes hors charte (`bg-white`, `text-sm`…), dont 10 sur la page de suivi du candidat. Aucune couleur en dur.
- **F-4 Un compte sans aucun rôle** (id 21, créé le 12/10) : il peut se connecter mais n'ouvre aucun espace.
- **F-5 Mot de passe Postgres de développement** en valeur par défaut dans `docker-compose.yml` (aucun autre secret versionné ;
  `backend/.env` est bien ignoré et absent de l'historique).
- **F-6 Fichiers démesurés** : `EspaceEtudiantController` (1 904 lignes), `AdminRecrutementController` (1 257), `PedagogieScolariteController`,
  `CandidatController` (> 1 000) — à découper par responsabilité.
- **F-7 Documentation** : `CLAUDE.md` décrit `test.formateur` comme « sans cours » ; il a désormais le cours 8.

## Ce qui a été vérifié et tient

- **Cloisonnement** : 8 rôles × 13 espaces, aucun accès indu ; anonyme refusé partout (401) ; jeton modifié, « alg: none », tronqué → 401 ;
  origine étrangère refusée par CORS.
- **Fichiers** : la racine des téléversements n'est pas servie (404), remontée de dossier refusée (400) ; téléchargements de supports,
  pièces et certificats cloisonnés.
- **Contrat** : 364 appels du frontend sur 366 correspondent à une route existante (les 2 autres sont du code mort).
- **Données** : chaque contrainte de la base accepte toutes les valeurs de son énumération Java (le défaut des modèles d'e-mails
  corrigé hier était le seul) ; aucune promotion sans cours, aucun étudiant sans promotion, aucun compte à rôles multiples.
- **Hygiène** : 0 `TODO`, 0 `console.log`, 0 couleur en dur ; aucun secret versionné ; configuration sans valeur de repli pour les secrets.
- **Parcours réels** : sur 106 pages, aucune erreur JavaScript, aucun « undefined/NaN », aucun nom technique affiché.

## Chantiers ouverts des audits précédents (rappel)

Paiement en ligne non raccordé ; e-mails `@inpg.sn` rejetés tant que le SPF n'autorise pas Brevo ; délai butoir d'un complément de dossier ;
résultat de test automatique ou manuel ; candidat désisté qui ne peut pas redéposer ; correctif `9e28fa0` absent de `main`/`preprod`.

## Plan recommandé

1. **C-1, C-2** (démarrage sur base neuve, premier administrateur) — indispensables avant toute installation.
2. **E-1, E-3, E-2** — privilèges, secret des certificats, limitation de débit.
3. **M-1, M-2, M-3, E-4** — accès aux fiches de cours, photos, cours sans formateur, mise à jour Angular.
4. **M-4, M-5, M-6** puis les points faibles.

## Correctifs du 2026-10-14 (même jour)

| Constat | Correctif | Vérification réelle |
|---|---|---|
| C-1 | statuts de session par constantes `SessionCandidature.STATUT_*` ; routes inutilisées `POST /dashboard/sessions/new` et `PATCH …/close` retirées | base vierge avec démonstration : le serveur démarre, sessions `OUVERTE`/`BROUILLON`/`CLOTUREE` |
| C-2 | `ComptesInitiaux` : comptes admission, pédagogie, évaluation créés au premier démarrage depuis `APP_INIT_*` | base vierge en production : 3 comptes, connexion OK ; second démarrage : rien de recréé ; sans mot de passe : refus explicite |
| E-1 | rôles attribuables limités à candidat, étudiant, formateur ; personnel et propre compte protégés ; page « Utilisateurs » alignée | 403 sur pédagogie, évaluateur, propre compte, rétrogradation du pédagogue ; 200 sur formateur |
| E-3 | clé des certificats obligatoire, sans repli ; anciennes clés acceptées pour vérifier (rotation) ; avertissement si identique au JWT | sans clé : refus de démarrer avec message ; recette : avertissement affiché |
| M-1 | routes générales `/cours`, `/cours/{id}`, `/cours/{id}/supports`, `/cohortes` retirées (inutilisées) | 0 fuite sur les 21 essais d'accès croisés |
| M-2 | chemin `photos/` dans `DataInitializer` + migration `V202610141000` pour les bases existantes | photothèques étudiant et formateur : plus aucune erreur |

Tests : 596 serveur et 288 frontend verts ; contrôles de mutation (liste blanche, statut « OUVERT », anciennes clés) détectés.
Restent ouverts : E-2 (limitation de débit), E-4 (mise à jour Angular), M-3 à M-6 et les points faibles.

**À faire avant le prochain déploiement en production** : vérifier que `CERTIFICAT_SIGNATURE_SECRET` y est défini (sinon le serveur refuse
désormais de démarrer — reprendre la valeur actuelle, celle de `JWT_SECRET` si elle était recopiée), et renseigner les `APP_INIT_*` si la base est neuve.
