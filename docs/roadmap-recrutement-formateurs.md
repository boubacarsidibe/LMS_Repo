# Roadmap — Workflow de recrutement des formateurs de l'INPG

> Statut : **proposition, non commencé.** Rédigé le 2026-09-08.
> Source métier : `PROCÉDURES QUALITÉ DE L'INPG V-09-2025` — procédure **QMS-007
> « Recrutement des formateurs »**.

---

## 1. Contexte et cadrage

### Ce que dit QMS-007

| # | Étape | Responsable (doc) | Contributeur (doc) |
|---|---|---|---|
| 1 | Création de la fiche de poste | DAF | DPI |
| 2 | Diffusion de l'information | DC | DAF |
| 3 | Réception et tri des candidatures | DAF | DPI |
| 4 | Sélection et recrutement | DAF / DPI | Comité de recrutement |
| 5 | Gestion administrative des contrats et paiements | DAF | DPI |

### Ce qui existe déjà dans le LMS

- **`Formateur`** (`auth.entity.Formateur`) : fiche mince — `nom`, `prenom`,
  `telephone`, `@MapsId` vers `User`. Créée **directement** par le responsable
  pédagogique (`PedagogieController#creerFormateur` → `FormateurService.createFormateur`),
  sans aucun processus de candidature.
- **Module `recrutement`** (`inpg.sn.lms.recrutement`) : pipeline **des candidats
  étudiants** (QMS-009) — `Candidature`, `SessionCandidature`, `PieceJustificative`,
  `FichierStocke`/`FichierStockeService`, `NotificationService`/`NotificationTemplate`,
  `TestAcademique`, `Entretien`, `EtapeDossier`/`JournalEtapeService`, exports Excel,
  tableaux de bord. **Fortement couplé aux étudiants** (promotions, frais de dossier,
  tests académiques) → ne pas y greffer les formateurs, mais **réutiliser ses
  patrons**.
- **`RESPONSABLE_ADMISSION`** : rôle qui pilote déjà tout le recrutement étudiant
  (offres, tri, décisions). Écrans et compétences réutilisables.
- Patron « compte créé à la première soumission » : `EcoleAccreditationController`,
  `CandidatController`, `EntrepriseController` (rôle temporaire accordé au dépôt,
  bascule ensuite).

### Décisions prises avec le propriétaire (2026-09-08)

1. **Pilote = `RESPONSABLE_ADMISSION`** (pas de nouveau rôle DAF/RH). Un onglet
   « Formateurs » s'ajoute à son espace. La DPI (responsable pédagogique) n'a pas
   d'écran dédié dans ce lot.
2. **Périmètre de ce lot** : offre de poste → candidature avec pièces → tri /
   sélection → **création du compte formateur** → **notification e-mail à chaque
   étape**. Workflow en **3 états** : `DOSSIER_RECU` → `EN_COURS_DE_TRAITEMENT` →
   `TERMINE`.
3. **Hors de ce lot (phases suivantes)** :
   - Phase 2 : épreuves/tests, entretiens, grille du comité de recrutement,
     génération du **contrat PDF** (QMS-007 étape 5), export Excel, journal
     d'étapes détaillé.
   - Phase 3 : **certification INPG des formateurs** (formation des formateurs →
     certificat 3 ans renouvelable, registre des modules qu'un formateur peut
     enseigner — condition **1.3** du Référentiel DRCQ), à brancher sur
     l'accréditation.

---

## 2. Modèle de données — migration `V202609171200__recrutement_formateurs.sql`

> Dernière migration à ce jour : V37 (journal détapes QMS-009). La prochaine était **V38**,
> mais un conflit de numérotation avec `develop` (V38-V41 déjà pris par un autre lot) a
> forcé un renommage en **V42** lors de la fusion — cf. `LOG_BLOQUANT.md` §68. Un second
> conflit est survenu à la fusion du 17/09 (`develop` avait entre-temps pris V42 à V47
> pour un lot sans rapport) : cette fois renommées en horodatage
> (`V202609171200`/`V202609171201`), conformément à la règle du `CLAUDE.md` — un entier
> séquentiel choisi par une branche isolée finit toujours par retomber sur un numéro pris
> ailleurs.

### 2.1 `offres_recrutement_formateur` — la fiche de poste (QMS-007 §1)

| Colonne | Type | Notes |
|---|---|---|
| `id` | BIGINT IDENTITY PK | |
| `intitule` | VARCHAR(200) NOT NULL | ex. « Formateur spécialité Forage » |
| `domaine` | VARCHAR(120) | Ingénierie, HSE, Géosciences… (aligné sur `Formation.domaine`) |
| `description_missions` | VARCHAR(4000) | |
| `profil_recherche` | VARCHAR(4000) | diplômes / expérience attendus (cf. Fiche technique §9) |
| `type_contrat` | VARCHAR(20) NOT NULL | `VACATAIRE` \| `CDD` \| `CDI` \| `PRESTATION` |
| `lieu` | VARCHAR(200) | même nom que `Entretien.lieuOuLien` ailleurs |
| `nb_postes` | INTEGER NOT NULL DEFAULT 1 | |
| `formation_id` | BIGINT NULL FK `formations(id)` | programme/cours visé — sert plus tard à `cours_formateur` |
| `date_publication` | DATE NULL | posée au passage `PUBLIEE` |
| `date_limite_candidature` | DATE NULL | |
| `statut` | VARCHAR(20) NOT NULL DEFAULT `BROUILLON` | `BROUILLON` \| `PUBLIEE` \| `CLOTUREE` |
| `date_creation` | TIMESTAMP NOT NULL DEFAULT now() | |
| `cree_par_user_id` | BIGINT FK `users(id)` | traçabilité |

### 2.2 `candidatures_formateur` — la candidature (QMS-007 §3-4)

| Colonne | Type | Notes |
|---|---|---|
| `id` | BIGINT IDENTITY PK | |
| `offre_id` | BIGINT NOT NULL FK `offres_recrutement_formateur(id)` | |
| `candidat_user_id` | BIGINT NOT NULL FK `users(id)` | le compte du candidat formateur |
| `nom` / `prenom` | VARCHAR(120) NOT NULL | recopiés (le compte peut être supprimé plus tard) |
| `email` | VARCHAR(200) NOT NULL | |
| `telephone` | VARCHAR(30) NOT NULL | |
| `diplome_principal` | VARCHAR(300) | |
| `annees_experience` | INTEGER | |
| `lettre_motivation` | VARCHAR(4000) | |
| `statut` | VARCHAR(30) NOT NULL DEFAULT `DOSSIER_RECU` | `DOSSIER_RECU` \| `EN_COURS_DE_TRAITEMENT` \| `TERMINE` |
| `decision` | VARCHAR(20) NULL | `RETENU` \| `NON_RETENU` — posée au passage `TERMINE` |
| `motif_decision` | VARCHAR(2000) NULL | **obligatoire si `NON_RETENU`** (règle LMS : motif obligatoire au rejet) |
| `date_soumission` | TIMESTAMP NOT NULL DEFAULT now() | |
| `date_decision` | TIMESTAMP NULL | |
| `traite_par_user_id` | BIGINT NULL FK `users(id)` | le responsable admission qui a tranché |
| `formateur_cree` | BOOLEAN NOT NULL DEFAULT false | garde-fou : ne créer le compte formateur qu'une fois |

- **Index unique partiel** : une seule candidature active par `(offre_id,
  candidat_user_id)` tant que `statut <> 'TERMINE'` (patron
  `idx_demande_active_unique` de V15).

### 2.3 `pieces_candidature_formateur` — les documents

| Colonne | Type | Notes |
|---|---|---|
| `id` | BIGINT IDENTITY PK | |
| `candidature_id` | BIGINT NOT NULL FK … `ON DELETE CASCADE` | |
| `type_document` | VARCHAR(40) NOT NULL | `CV` \| `DIPLOME` \| `ATTESTATION_EXPERIENCE` \| `PIECE_IDENTITE` \| `AUTRE` |
| `fichier_stocke_id` | BIGINT FK `fichiers_stockes(id)` | réutilise le stockage du module recrutement |

- Types **requis** : `CV`, `DIPLOME`. **Recommandés** : `ATTESTATION_EXPERIENCE`,
  `PIECE_IDENTITE`. `AUTRE` multiple, facultatif. (Même logique requis/recommandés
  que la checklist d'accréditation.)

### 2.4 Rôle applicatif (pas en SQL)

- **`ROLE_CANDIDAT_FORMATEUR`** : ajouté dans `config/DataInitializer.java` via
  `ensureRole(roleRepository, "ROLE_CANDIDAT_FORMATEUR", "Candidat formateur", …)`
  (les rôles sont amorcés en Java, jamais en migration).
- Accordé au **premier dépôt** de candidature (patron `garantirRoleEcole`).
  Ajouté à `ROLES_OUVRANT_UN_ESPACE` partout où l'exclusivité d'espace est
  vérifiée (accréditation, entreprise, recrutement).
- Au passage `TERMINE` + `decision = RETENU` : le compte bascule en
  `ROLE_FORMATEUR` + fiche `Formateur` créée (patron
  `AdminRecrutementController#inscrireCommeEtudiant` ; CLAUDE.md §3 « l'admission
  ouvre l'accès »).

---

## 3. Backend — `inpg.sn.lms.recrutementformateur` (nouveau paquet, sibling de `recrutement`)

### 3.1 Enums

`EnumTypeContratFormateur`, `EnumStatutOffreFormateur`, `EnumStatutCandidatureFormateur`
(`DOSSIER_RECU`, `EN_COURS_DE_TRAITEMENT`, `TERMINE`), `EnumDecisionCandidatureFormateur`
(`RETENU`, `NON_RETENU`), `EnumTypeDocumentCandidatureFormateur`.

### 3.2 Entités / repos / DTOs

- `entity/` : `OffreRecrutementFormateur`, `CandidatureFormateur`
  (`@OneToMany cascade=ALL orphanRemoval=true` vers pièces),
  `PieceCandidatureFormateur` (`@OneToOne cascade=ALL` vers `FichierStocke` — le
  patron corrigé pour l'accréditation, sinon 500 `TransientPropertyValueException`).
- `repository/` : `OffreRecrutementFormateurRepository`
  (`findByStatutOrderByDatePublicationDesc`), `CandidatureFormateurRepository`
  (`findByCandidatUserIdOrderByDateSoumissionDesc`, `findByOffreId`,
  `findByOffreIdAndCandidatUserIdAndStatutNot`), `PieceCandidatureFormateurRepository`.
- `dto/` : `OffreFormateurDto` / `OffreFormateurRequest`,
  `CandidatureFormateurDto` / `CandidatureFormateurCreateRequest`,
  `PieceCandidatureFormateurDto`, `DecisionCandidatureFormateurRequest`
  (`decision`, `motif`), `ChangementStatutRequest` (`statut`).

### 3.3 Contrôleurs

**`RecrutementFormateurPublicController`** — `@RequestMapping("/api/recrutement-formateurs/public")`, `permitAll` :
- `GET /offres` — offres `PUBLIEE` (intitulé, domaine, type contrat, lieu, date
  limite). Vue restreinte.
- `GET /offres/{id}` — détail (missions, profil recherché).

**`CandidatFormateurController`** — `@RequestMapping("/api/recrutement-formateurs")`, `authenticated()` :
- `POST /candidatures` — dépose une candidature (`offreId`, infos, lettre) ;
  crée `ROLE_CANDIDAT_FORMATEUR` si compte neuf ; refuse (`403`) si le compte
  ouvre déjà un autre espace ; refuse (`409`) si candidature active déjà sur
  cette offre ; refuse (`409`) si l'offre n'est pas `PUBLIEE` ou la date limite
  passée.
- `GET /mes-candidatures` — les miennes (+ statut, décision, motif).
- `GET /candidatures/{id}` — une des miennes (sinon `404`).
- `POST /candidatures/{id}/pieces` — upload multipart (`typeDocument`, `fichier`),
  tant que `statut <> TERMINE` ; `MultipartException` → 400, `IOException` → 500
  (handlers globaux déjà en place).
- `DELETE /candidatures/{id}/pieces/{pieceId}` — retrait, tant que `<> TERMINE`.
- `GET /candidatures/{id}/pieces/{pieceId}/fichier` — relire une pièce.

**`AdminRecrutementFormateurController`** — `@RequestMapping("/api/admin/recrutement-formateurs")`.
Couvert par le joker `/api/admin/** → hasRole("RESPONSABLE_ADMISSION")` de
`SecurityConfig` (ligne 111) ; ajouter quand même `@PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")`
au niveau classe (double protection, patron `EvaluateurAccreditationController`).
- **Offres** : `GET /offres` (tous statuts, filtrables) · `POST /offres` (crée en
  `BROUILLON`) · `PUT /offres/{id}` · `PUT /offres/{id}/publier` (→ `PUBLIEE` +
  `date_publication`) · `PUT /offres/{id}/cloturer` (→ `CLOTUREE`) ·
  `DELETE /offres/{id}` (seulement si `BROUILLON` et aucune candidature).
- **Candidatures** : `GET /candidatures?offreId=&statut=` · `GET /candidatures/{id}`
  (détail + pièces) · `GET /candidatures/{id}/pieces/{pieceId}/fichier`.
- `PUT /candidatures/{id}/statut` — corps `{statut}` : transitions
  `DOSSIER_RECU → EN_COURS_DE_TRAITEMENT → TERMINE`. **Notifie le candidat** à
  chaque transition. Refuse un saut arrière ou incohérent (`400`).
- `PUT /candidatures/{id}/decision` — corps `{decision, motif}`.
  Passe la candidature en `TERMINE` si elle ne l'est pas déjà. `motif`
  **obligatoire** si `NON_RETENU`. Si `RETENU` :
  - pré-check `formateurRepository.findByTelephone(...)` (téléphone unique — même
    garde que `PedagogieController#creerFormateur`, sinon `DataIntegrityViolationException`).
  - `FormateurService.createFormateur(...)` **ou** bascule de rôle du compte
    existant du candidat (`ROLE_CANDIDAT_FORMATEUR` → `ROLE_FORMATEUR`) + création
    de la fiche `Formateur` à partir de `candidature.nom/prenom/telephone`.
    → choix : **basculer le compte existant** (le candidat garde ses identifiants),
    pas de compte neuf. `formateur_cree = true`.
  - notifie « retenu, votre espace formateur est actif ».
  - Si `NON_RETENU` : notifie avec le motif.

### 3.4 Service de notification — `RecrutementFormateurNotificationService`

Patron `AccreditationNotificationService` / `EntrepriseNotificationService` :
préfixe `[INPG Recrutement Formateurs]`, envoi **best-effort** (une panne SMTP ne
bloque jamais l'acte métier). Méthodes :
- `notifierDossierRecu(email, nom, intituleOffre)` — accusé de réception.
- `notifierPriseEnCharge(email, nom)` — passage `EN_COURS_DE_TRAITEMENT`.
- `notifierDecisionRetenu(email, nom, intituleOffre)` — + rappel identifiants.
- `notifierDecisionNonRetenu(email, nom, motif)`.
- `notifierNouvelleCandidature(emailAdmission, nomOffre)` — vers la boîte
  admission (`app.admission.email`), à chaque dépôt.

### 3.5 Sécurité — `config/SecurityConfig.java`

- Ligne `permitAll` (~85) : ajouter `"/api/recrutement-formateurs/public/**"`.
- Rien d'autre : `/api/recrutement-formateurs/**` tombe sur
  `.anyRequest().authenticated()`, `/api/admin/recrutement-formateurs/**` sur le
  joker admin.

### 3.6 Réutilisations explicites

`FichierStockeService` (upload/lecture), `FormateurService` /
`FormateurRepository` (fiche + rôle), `GlobalExceptionHandler` (déjà
`MultipartException` / `DataIntegrityViolationException`), le patron checklist
requis/recommandés de l'accréditation.

---

## 4. Frontend

### 4.1 Vitrine publique — `features/recrutement-formateurs/public/`

- Page **« Rejoindre l'INPG comme formateur »** : liste des offres `PUBLIEE`
  (carte : intitulé, domaine, type de contrat, lieu, date limite) + page de
  détail (missions, profil). CTA **« Postuler »** → si non connecté, redirige
  vers `login`/`register` avec retour sur l'offre.
- Route publique (ajout dans les routes du site vitrine, à côté du catalogue des
  formations).

### 4.2 Espace candidat formateur — `features/espace-candidat-formateur/` (nouveau)

Mirroir de `features/accreditation` (espace école) :
- **Layout** (`candidat-formateur-layout`) : sidebar « Ma candidature » + cercle
  profil (chevron) → `mon-profil` (identité + mot de passe via
  `POST /api/auth/change-password`) + déconnexion. Logotype « INPG LMS ».
- **Formulaire de candidature** : choix de l'offre (préremplie si arrivée depuis
  une offre), infos (nom, prénom, téléphone, diplôme principal, années
  d'expérience), lettre de motivation, puis **checklist de pièces** (CV requis,
  diplôme requis, attestation d'expérience + pièce d'identité recommandées,
  autres) — réutiliser le composant checklist de
  `demande-accreditation.ts` (input fichier masqué + ligne par type + icône
  d'état + Voir/Remplacer/Supprimer).
- **Suivi** : barre de progression **3 étapes** (Dossier reçu → En cours de
  traitement → Terminé) + bandeau de décision :
  - `RETENU` → « Félicitations, votre espace formateur est activé. Connectez-vous
    avec les mêmes identifiants. »
  - `NON_RETENU` → motif affiché.

### 4.3 Espace admission — `features/recrutement/responsable-admission/` (extension)

- **Nav** : entrée « Formateurs » (`responsable-admission-layout.component.ts`),
  icône `badge` ou `co_present`.
- **`formateurs/offres`** — liste des offres + **page dédiée** création/édition
  (cohérent avec le patron « pages dédiées » introduit récemment pour
  sessions/tests : `sessions/nouvelle` avant `sessions/:id` dans les routes).
  Actions : Publier / Clôturer / Supprimer (si brouillon).
- **`formateurs/candidatures`** — liste filtrable (par offre, par statut) +
  panneau de détail : infos candidat, pièces (bouton Voir), **sélecteur de
  statut** (3 états) et bloc **Décision** (Retenu / Non retenu + zone motif
  obligatoire si non retenu). Bouton **« Créer le compte formateur »** actif
  quand `TERMINE` + `RETENU` et `formateur_cree = false`.
- Services : `recrutement-formateur.service.ts` (candidat),
  `admin-recrutement-formateur.service.ts` (admission).
- Modèles : `OffreRecrutementFormateur`, `CandidatureFormateur`,
  `PieceCandidatureFormateur`, enums + `LIBELLES_*`.

### 4.4 Charte

`CLAUDE.md §1` : `font-*` **et** `text-*` ensemble, cartes
`bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm`,
boutons plats `bg-secondary text-on-secondary`, conteneur
`p-margin-desktop space-y-stack-lg max-w-container-max mx-auto`.

---

## 5. Tests (backend, Mockito unitaire — patron des tests d'accréditation)

- **`OffreRecrutementFormateurControllerTest`** : `publier` sur brouillon → OK ;
  `supprimer` une offre avec candidatures → 409.
- **`CandidatFormateurControllerTest`** : `POST /candidatures` sur compte neuf →
  crée `ROLE_CANDIDAT_FORMATEUR` + 201 ; compte ouvrant déjà un espace → 403 ;
  candidature active existante → 409 ; upload sur candidature `TERMINE` → 409 ;
  candidature d'autrui → 404.
- **`AdminRecrutementFormateurControllerTest`** : transition de statut → notif
  appelée ; `decision NON_RETENU` sans motif → 400 ; `decision RETENU` → bascule
  `ROLE_FORMATEUR` + fiche `Formateur` créée + `formateur_cree = true` ; second
  appel `RETENU` → pas de doublon ; téléphone déjà pris → 400.

⚠️ `mvn test` complet touche la base de recette (CLAUDE.md §2) → lancer ciblé
`-Dtest='...Formateur...'` (tests unitaires purs, sans contexte Spring).

---

## 6. Ordre de réalisation (ce lot)

| # | Bloc | Contenu | Vérif |
|---|---|---|---|
| **P1** | Back — socle | Migration `V38` (renommée **V42** lors de la fusion avec `develop`, cf. §6bis) ; `ROLE_CANDIDAT_FORMATEUR` dans `DataInitializer` ; enums + entités + repos + DTOs ; `RecrutementFormateurNotificationService` | `mvnw -o compile` |
| **P2** | Back — API | 3 contrôleurs (public / candidat / admin) + règle `SecurityConfig` + réutilisation `FichierStockeService`/`FormateurService` | `mvnw -o test` ciblé, `curl` |
| **P3** | Front — candidat | Vitrine offres + espace candidat formateur (layout, formulaire + checklist pièces, suivi 3 étapes) | `ng build` |
| **P4** | Front — admission | Onglet « Formateurs » : pages offres (CRUD + publier/clôturer) + page candidatures (statut + décision + création compte) | `ng build` |
| **P5** | Recette + doc | Parcours complet sur Docker ; entrée `LOG_BLOQUANT.md` ; `docs/daily_logs/` | end-to-end |
| **P6** | Dossier enrichi | Migration `V39` (renommée **V43**, même fusion) : état civil, adresse, formation/expérience/langues/certifications répétables, domaines d'expertise ; formulaire à sections rétractables ; panneau admission étendu — inspiré d'un portail SAP SuccessFactors, cf. `LOG_BLOQUANT.md` §68 | `mvnw -o test` ciblé, `ng build`, end-to-end |

### 6bis. Renumérotation V38/V39 → V42/V43 (fusion avec `develop`, 2026-09-15)

Ce lot a été développé sur une branche restée isolée pendant qu'un autre lot
prenait sur `develop` les numéros **V38 à V41** (annulation de décisions,
prix par session, relevé, description des pièces justificatives — module
recrutement étudiant, sans lien avec le recrutement formateur). À la fusion,
les deux migrations de ce lot (`V38__recrutement_formateurs.sql` et
`V39__profil_candidature_formateur.sql`) ont donc été renommées
`V42__recrutement_formateurs.sql` et `V43__profil_candidature_formateur.sql`
(contenu SQL inchangé — seul le préfixe de version change, `out-of-order=true`
étant déjà actif dans `application.properties`). Toute base déjà migrée avec
les anciens numéros doit voir ses deux lignes `flyway_schema_history`
corrigées à la main (`version`/`script`, le `checksum` reste identique car il
porte sur le contenu du fichier, pas sur son nom).

**Rebelote à la fusion du 17/09** : `develop` avait entre-temps posé ses propres
`V42` à `V47` (statut de vérification des pièces, sessions et rôles initialisés,
notes sur 100, activités de cours). Même collision, cette fois définitivement
évitée en renommant en horodatage plutôt qu'en entier :
`V202609171200__recrutement_formateurs.sql` et
`V202609171201__profil_candidature_formateur.sql`. Ces deux migrations n'avaient
jamais été appliquées à une base partagée sous leurs anciens noms `V42`/`V43` —
aucune correction de `flyway_schema_history` n'était donc nécessaire cette fois.

---

## 7. Vérification end-to-end (stack Docker locale, `:4200`)

1. `docker compose up -d --build backend` → log Flyway **`V202609171200`** (`V38` à
   l'origine, renommée en V42 puis en horodatage — cf. §6bis) ; amorçage du rôle
   `ROLE_CANDIDAT_FORMATEUR`. Puis `--build frontend`.
2. **Admission** (`test.admission@inpg.sn` / `Test@2026`) : onglet Formateurs →
   créer une offre → **Publier**. `GET /api/recrutement-formateurs/public/offres`
   la renvoie.
3. **Candidat formateur** : créer un compte neuf (`/auth/register`) → depuis la
   vitrine, **Postuler** à l'offre → remplir + déposer un CV et un diplôme →
   soumettre. Compte porte `ROLE_CANDIDAT_FORMATEUR` ; e-mail « dossier reçu »
   parti (log SMTP best-effort). Postuler une 2ᵉ fois à la même offre → 409.
4. **Admission** : la candidature apparaît en `DOSSIER_RECU` → passer
   `EN_COURS_DE_TRAITEMENT` (candidat notifié) → `TERMINE` →
   **Décision : Retenu** → « Créer le compte formateur ».
   - Vérifier : le compte du candidat porte maintenant `ROLE_FORMATEUR`, une
     fiche `formateurs` existe, e-mail « retenu » parti.
   - Sur un autre dossier : **Décision : Non retenu** sans motif → 400 ; avec
     motif → candidat notifié, motif visible dans son suivi.
5. **Formateur** : le candidat retenu se connecte (mêmes identifiants) → arrive
   dans l'espace formateur (vide tant qu'aucun cours ne lui est affecté via
   `PUT /api/pedagogie/cours/{id}/formateurs` — cf. CLAUDE.md §3).
6. `mvnw -o test -Dtest='*RecrutementFormateur*,*CandidatFormateur*'` verts ;
   `ng build` OK. Restaurer les données d'essai (CLAUDE.md §5).

---

## 8. Explicitement reporté (à consigner dans `LOG_BLOQUANT.md`)

- **Phase 2 recrutement formateur** : épreuves/tests d'aptitude, entretiens,
  grille d'évaluation du **comité de recrutement** (QMS-007 §4), génération du
  **contrat PDF** + suivi paiements (QMS-007 §5), export Excel des candidatures,
  journal d'étapes fin (réutiliser `EtapeDossier` / `EnumEtapeQms` /
  `JournalEtapeService` du module recrutement étudiant).
- **Phase 3 — certification INPG des formateurs** (condition **1.3** du
  Référentiel DRCQ + Cadre §III.2 / §V.3 / §V.5) : parcours « formation des
  formateurs » → **certificat horodaté** (QR + référence, comme les certificats
  étudiants — `CertificatPdfService`), **valable 3 ans**, relance de
  renouvellement (`@Scheduled`, `@EnableScheduling` déjà en place), et **registre
  des modules** qu'un formateur est habilité à enseigner — à croiser avec
  `cours_formateur` et avec la grille d'accréditation (un module de programme
  accrédité n'est enseignable que par un formateur certifié).
- **Rôle DAF/RH distinct** : non créé (décision produit) — `RESPONSABLE_ADMISSION`
  porte les deux recrutements. À rouvrir si la séparation des responsabilités
  QMS-007 devient un besoin réel.
