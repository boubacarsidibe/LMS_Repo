# PASSATION — Alignement LMS INPG sur les référentiels officiels

> Document de reprise. Rédigé au milieu d'un lot de travail, pour qu'un autre
> poste (pull de la branche `develop`) puisse **continuer exactement** là où je
> me suis arrêté. Le travail est **commité** (commit `e2fc4dd` « version 1 apres
> reception des doc ») puis **mergé avec `origin/develop`** (8 commits recrutement/
> admission de l'autre équipe). Migrations renumérotées (voir §2). Lis ce fichier
> en entier avant de toucher au code.
>
> Dernière mise à jour : 2026-09-03, branche `develop`.

---

## 0. Où en est-on, en une phrase

Trois lots successifs autour de l'**accréditation**, de l'**espace entreprise**
et du **catalogue**, déclenchés par des documents officiels de l'INPG déposés à
la racine du dépôt.
**Lot 1 (accréditation) : terminé, déployé, vérifié.**
**Lot 2 (fiche technique de module + catalogue §3) : back terminé (compile + tests
compilent, étape 0 déjà faite pendant le merge), tout le FRONT reste à faire.**

### État de compilation à l'instant de la passation

| Cible | État |
|---|---|
| `backend` — `mvnw -o compile` | ✅ **OK** |
| `backend` — `mvnw -o test-compile` | ✅ **OK** (étape 0 faite pendant le merge) |
| `frontend` — `ng build` | ✅ OK (aucun code front du lot 2 n'a été écrit) |
| Conteneurs Docker | Lot 1 déployé et vérifié. Lot 2 **pas déployé**. |

---

## 1. Documents de référence déposés à la racine (non versionnés, `??`)

| Fichier | Contenu | Traité ? |
|---|---|---|
| `Cadre de référence DRCQ.pdf` | Procédure d'accréditation des programmes Oil & Gas (structures éligibles, 9 étapes, décision pleine/conditionnelle/refus, validité 3 ans, label). | ✅ Lot 1 |
| `Référentiel Accréditation Programme Oil  Gas DRCQ- 10 02 2024_vHcéres.pdf` | Grille : **31 conditions** (codes 1.1→4.5) en **4 sections**, chacune avec ses lignes directrices « ASG ». | ✅ Lot 1 (seed `referentiel-accreditation.json`) |
| `PROCÉDURES QUALITÉ DE L'INPG V-09-2025_compressed.pdf` | SMQ : 14 procédures `QMS-001`→`QMS-014`, Responsable/Contributeur parmi les directions **DRCQ, DPI, DAF, DC, BDU, DG, CPI, PF, JURY, ALUMNI**. Surtout un référentiel d'organisation qui *valide* l'existant. | ✅ Lecture faite (voir §3), rien à construire hormis nommage DRCQ (déjà fait) |
| `FICHE TECHNIQUE  LUBRIFICATION DES MACHINES TOURNANTES.docx` | Gabarit en **10 sections** d'un module de formation d'entreprise sur mesure (exemple SAR). C'est le livrable de la DPI en réponse à une demande entreprise « devis / sur mesure ». | 🚧 Lot 2 — **en cours** |
| `Calendrier Formations INPG2026.pdf` | **Déposé juste avant la passation. PAS ENCORE LU / ANALYSÉ.** À traiter par le repreneur. | ❌ Non traité |

---

## 2. LOT 1 — Accréditation alignée sur la DRCQ ✅ TERMINÉ

Déployé sur la stack Docker locale et vérifié end-to-end (curl + 37 tests
backend verts). **Ne pas y retoucher**, sauf bug. Résumé (détail complet :
entrée **64** de `LOG_BLOQUANT.md`) :

- **La demande d'accréditation porte sur un PROGRAMME** (Cadre §II), pas
  l'établissement : `demandes_accreditation` gagne `intitule_programme`,
  `niveau_programme` (enum `EnumNiveauProgramme`), `type_structure` (enum
  `EnumTypeStructure`), `effectif_cohorte`, `date_debut_cohorte`. Migration
  **`V33`**.
- **Décision conditionnelle** : `EnumStatutDemandeAccreditation` +
  `ACCREDITE_SOUS_RESERVE`, colonne `plan_actions`. Endpoint
  `PUT /api/admin/accreditation/demandes/{id}/accrediter-sous-reserve`.
- **Validité 3 ans** : `date_expiration` = décision + 3 ans, servie école /
  évaluateur / liste publique.
- **Référentiel réel** : `criteres_accreditation` + `code`/`section`/`ordre`,
  `libelle` élargi à `VARCHAR(600)` (migration **`V34`** — piège : la condition
  2.1 dépasse 200 car., le `CommandLineRunner` plantait après « Started »).
  Amorçage : `AccreditationReferentielInitializer` +
  `resources/data/referentiel-accreditation.json` (31 critères), idempotent par
  `code`.
- **Types de pièces** alignés Cadre §IV.1/IV.4 (+7 valeurs sur
  `EnumTypeDocumentAccreditation`).
- **Page « Référentiel » de l'évaluateur** refondue : regroupée par section,
  lignes directrices dépliables, **interrupteur activer/désactiver** (nouvel
  endpoint `PUT /api/admin/accreditation/criteres/{id}/reactiver`).
- **Fix cache** : `frontend/nginx.local.conf` envoie `Cache-Control: no-store`
  (le build de dev ne versionne pas `main.js` → le proxy Codespaces resservait
  du périmé).

Fichiers du lot 1 (dans le commit `e2fc4dd`) :
`backend/.../accreditation/**`, `backend/.../config/AccreditationReferentielInitializer.java`,
`backend/.../config/SecurityConfig.java`, `resources/data/referentiel-accreditation.json`,
`resources/db/migration/V32..V34`, `frontend/.../features/accreditation/**`,
`frontend/.../features/evaluateur-accreditation/**`, `frontend/nginx.local.conf`.

> ⚠️ **Renumérotation après merge avec `origin/develop`.** L'autre équipe avait
> déjà des migrations `V30` / `V31` (critères d'admission, lieu de test). Mes
> migrations d'accréditation/entreprise ont donc été décalées :
> `V30→V32` (grille éval), `V31→V33` (programme + décision), `V32→V34` (libellé),
> `V33→V35` (fiche technique), `V34→V36` (catalogue §3).
> Sur une **base neuve**, Flyway applique V1→V36 dans l'ordre, aucun souci.
> Sur **ma base Docker locale**, les anciens V30/V31/V32 (mes numéros d'avant)
> sont déjà dans `flyway_schema_history` : après ce merge il faut **réinitialiser
> la base locale** (`docker compose down` + supprimer le volume postgres, ou
> `DROP SCHEMA public CASCADE;` puis laisser Flyway tout rejouer). Le collègue
> qui pull sur une autre machine avec sa propre base n'est pas concerné.

---

## 3. Lecture acteur par acteur des 2 nouveaux documents (QMS + Fiche technique)

| Acteur LMS (≈ direction INPG) | Ce que disent les docs | Décision |
|---|---|---|
| **Évaluateur accréditation ≈ DRCQ** | QMS-001..004 = mécanique interne du SMQ, non exposée dans le LMS. Nommage DRCQ. | Fait au lot 1. Rien de plus. |
| **Responsable pédagogique ≈ DPI** | Porte QMS-005/006/009/010/011/012/013. « Responsable du module à l'INPG », rédige la Fiche Technique, validée par le CPI. Aujourd'hui **aucun écran** pour traiter les demandes entreprise (le back `PedagogieEntrepriseController` existe, rien ne le consomme). | **Lot 2** : écran « Entreprises » + éditeur de Fiche Technique. |
| **Entreprise** | QMS-008 (offre technique **et** financière), QMS-010. Aujourd'hui : dépôt texte libre → réponse texte libre. | **Lot 2** : reçoit la Fiche Technique structurée, l'accepte ou demande des ajustements. |
| **Responsable admission ≈ DPI/DAF** | QMS-009 ; Fiche §3 « test de prérequis ». Le module recrutement fait déjà tests + entretiens. | Aucun changement, alignement déjà en place. |
| **Formateur ≈ PF** | QMS-007/010/011 ; Fiche §9 profil formateur. | Marginal — champ « profil formateur » porté par la Fiche. Pas d'écran nouveau. |
| **Alumni** | QMS-013 : base alumni, besoins entreprises, partage, **mises en relation**, suivi insertion, analyse. L'espace alumni couvre base + enquêtes de suivi. | « Mises en relation alumni ↔ entreprises » = **hors périmètre**, à consigner (LOG_BLOQUANT, nouvelle entrée 65). |
| **Étudiant** | QMS-010/012 — couverts. | Rien. |

**Périmètre validé par le propriétaire pour le lot 2 :** fonctionnalité *complète*
Fiche Technique de Module **+** champs du §3 du Référentiel sur le catalogue.
**Explicitement écartés (à consigner, ne pas construire) :** renommage des rôles
LMS en DPI/DAF/… (churn sans valeur) ; mises en relation alumni ↔ entreprises.

---

## 4. LOT 2 — ROADMAP EXACTE À SUIVRE

### Ce qui est DÉJÀ sur le disque (fait, à ne pas refaire)

**Phase 3 — back, fichiers CRÉÉS (`??`) :**
- `backend/src/main/resources/db/migration/V35__fiche_technique_module.sql`
  (tables `fiche_technique_module` [1:1 avec `demandes_entreprise`, `demande_id`
  UNIQUE] et `fiche_module_jour`).
- `backend/.../entreprise/enums/EnumStatutFicheModule.java`
  (`BROUILLON`, `TRANSMISE`, `ACCEPTEE`, `AJUSTEMENTS_DEMANDES`).
- `backend/.../entreprise/entity/FicheTechniqueModule.java` (≈30 champs = les 10
  sections du gabarit ; `@OneToMany cascade=ALL orphanRemoval=true` vers
  `FicheModuleJour` ; `@OneToOne` LAZY vers `DemandeEntreprise`).
- `backend/.../entreprise/entity/FicheModuleJour.java`.
- `backend/.../entreprise/repository/FicheTechniqueModuleRepository.java`
  (`findByDemandeId`).
- `backend/.../entreprise/dto/FicheTechniqueModuleDto.java`,
  `FicheModuleJourDto.java`, `FicheTechniqueModuleRequest.java`
  (+ `FicheTechniqueModuleRequest.JourRequest` interne).
- `backend/.../entreprise/service/FicheTechniqueModuleService.java`
  (`appliquer(fiche, request)` recopie tous les champs + reconstruit `jours` ;
  `estTransmissible(fiche)` = intitulé + objectif général + ≥ 1 jour ;
  `toDto(fiche)`).

**Phase 3 — back, fichiers MODIFIÉS (`M`) :**
- `entreprise/entity/DemandeEntreprise.java` : ajout
  `@OneToOne(mappedBy="demande", fetch=LAZY) @JsonIgnore FicheTechniqueModule fiche;`
  + import `JsonIgnore` + import `OneToOne`.
- `entreprise/dto/DemandeEntrepriseDto.java` : ajout dernier champ
  `String ficheStatut` (statut de la fiche, ou `null`).
- `entreprise/controller/EntrepriseController.java` : constructeur
  **+`FicheTechniqueModuleRepository ficheRepository`, +`FicheTechniqueModuleService ficheService`** ;
  `toDto` renvoie `ficheStatut` ;
  nouveaux endpoints (contrôle de propriété via `utilisateurCourant.entreprise`) :
  - `GET  /api/entreprise/demandes/{id}/fiche` (jamais un `BROUILLON`)
  - `PUT  /api/entreprise/demandes/{id}/fiche/accepter` → `ACCEPTEE`, demande `CLOTUREE`
  - `PUT  /api/entreprise/demandes/{id}/fiche/ajustements` corps `{commentaire}` obligatoire → `AJUSTEMENTS_DEMANDES`
  - helper privé `ficheVisible(demandeId, auth)`.
- `entreprise/controller/PedagogieEntrepriseController.java` : constructeur
  **+`FicheTechniqueModuleRepository`, +`FicheTechniqueModuleService`, +`EntrepriseNotificationService notificationService`** ;
  nouveaux endpoints (déjà `hasRole("RESPONSABLE_PEDAGOGIQUE")` via
  `/api/pedagogie/**` dans `SecurityConfig`) :
  - `GET  /api/pedagogie/entreprises/demandes/{id}/fiche` (404 si aucune)
  - `PUT  /api/pedagogie/entreprises/demandes/{id}/fiche` — **upsert** : crée en
    `BROUILLON` si absente ; 409 si déjà `ACCEPTEE`
  - `PUT  /api/pedagogie/entreprises/demandes/{id}/fiche/transmettre` — 400 si
    `!estTransmissible` ; sinon `TRANSMISE` + `date_transmission` + notifie
    l'entreprise + fait passer la demande de `SOUMISE` à `EN_COURS`.
- `entreprise/service/EntrepriseNotificationService.java` : +
  `notifierFicheTransmise(emailDest, raisonSociale, intituleModule)`,
  `notifierDecisionFiche(raisonSociale, acceptee, commentaire)`.

**Phase 2 — back, fichiers déjà touchés :**
- `backend/src/main/resources/db/migration/V36__formation_caracteristiques_referentiel.sql`
  (CRÉÉ — `ALTER TABLE formations ADD COLUMN heures_simulation_labo INT,
  anglais_professionnel BOOLEAN DEFAULT false, soft_skills BOOLEAN DEFAULT false,
  credits_totaux INT, volume_horaire_total INT`).
- `espace_etudiant/entity/Formation.java` : +5 champs + getters/setters
  explicites (`heuresSimulationLabo`, `anglaisProfessionnel`, `softSkills`,
  `creditsTotaux`, `volumeHoraireTotal`).
- `config/CatalogueDataInitializer.java` : lit les 5 champs du JSON à la création
  ET, pour une fiche existante, ne les complète que s'ils sont vides (même garde
  que `ficheDetaillee`).
- `resources/data/catalogue-formations.json` : renseigné pour **2 fiches
  seulement** — `ING-PET-GAZ-02` (70 h simu, anglais, soft skills, 120 crédits,
  900 h) et `GEO-ING-RES-06` (70 h simu, anglais, 120 crédits, 900 h). Les autres
  n'ont pas les clés (⇒ « non renseigné », masqué sur la fiche). *Un script
  Python pour poser les drapeaux `anglais`/`soft` sur plus de fiches a été refusé
  par le propriétaire — laisser tel quel, ou lui redemander.*

---

### Étape 0 — ~~DÉBLOQUER LA COMPILATION DES TESTS~~ ✅ DÉJÀ FAIT (pendant le merge `origin/develop`)

Les 2 constructeurs de `EntrepriseControllerTest` et
`PedagogieEntrepriseControllerTest` ont été mis à jour (mocks `ficheRepository`,
`ficheService`, `notificationService`). `mvnw -o test-compile` = BUILD SUCCESS,
61 tests unitaires ciblés verts. Description conservée ci-dessous pour mémoire :

**a) `backend/src/test/java/inpg/sn/lms/entreprise/controller/EntrepriseControllerTest.java`, ~ligne 44-70 :**
Ajouter deux `@Mock` et les passer au constructeur :
```java
@Mock private inpg.sn.lms.entreprise.repository.FicheTechniqueModuleRepository ficheRepository;
@Mock private inpg.sn.lms.entreprise.service.FicheTechniqueModuleService ficheService;
...
controller = new EntrepriseController(entrepriseRepository, demandeRepository, messageRepository,
        formationRepository, ficheRepository, ficheService, roleRepository, utilisateurCourant, notificationService);
```
(ordre EXACT des args : `entrepriseRepository, demandeRepository, messageRepository,
formationRepository, ficheRepository, ficheService, roleRepository,
utilisateurCourant, notificationService`).

**b) `backend/src/test/java/inpg/sn/lms/entreprise/controller/PedagogieEntrepriseControllerTest.java`, ~ligne 40-58 :**
```java
@Mock private inpg.sn.lms.entreprise.repository.FicheTechniqueModuleRepository ficheRepository;
@Mock private inpg.sn.lms.entreprise.service.FicheTechniqueModuleService ficheService;
@Mock private inpg.sn.lms.entreprise.service.EntrepriseNotificationService notificationService;
...
controller = new PedagogieEntrepriseController(demandeRepository, messageRepository, entrepriseRepository,
        ficheRepository, ficheService, notificationService, utilisateurCourant);
```
(ordre EXACT : `demandeRepository, messageRepository, entrepriseRepository,
ficheRepository, ficheService, notificationService, utilisateurCourant`).

Vérifier : `cd backend && JAVA_HOME=... sh mvnw -o test-compile` → BUILD SUCCESS.

### Étape 1 — Tests backend du lot 2

Nouveau `FicheTechniqueModuleControllerTest` (ou étendre les 2 tests entreprise) :
- upsert crée une fiche en `BROUILLON` ;
- `transmettre` sur fiche vide → 400 ; sur fiche instruisable → `TRANSMISE` +
  `notifierFicheTransmise` appelé ;
- côté entreprise : `GET .../fiche` sur un `BROUILLON` → 404 ; sur `TRANSMISE` → 200 ;
- `accepter` → `ACCEPTEE` + `DemandeEntreprise.statut == CLOTUREE` ;
- `ajustements` sans commentaire → 400 ; avec → `AJUSTEMENTS_DEMANDES` ;
- **cloisonnement** : entreprise B ne voit pas la fiche d'une demande de A (→ 404).

Puis lancer (JAVA_HOME 17, voir §6) :
`sh mvnw -o test -Dtest='FicheTechniqueModuleControllerTest,EntrepriseControllerTest,PedagogieEntrepriseControllerTest,EspaceEtudiantGardeFousTest,AccreditationReferentielInitializerTest'`

### Étape 2 — Phase 2 back : exposer les 5 champs du catalogue

- `recrutement/dto/FormationDetailPubliqueDto.java` : ajouter
  `Integer heuresSimulationLabo, boolean anglaisProfessionnel, boolean softSkills,
  Integer creditsTotaux, Integer volumeHoraireTotal`.
- `recrutement/controller/PublicController.java`, méthode `consulterFormation`
  (le `new FormationDetailPubliqueDto(...)`) : passer les 5 valeurs depuis
  l'entité.
- (Optionnel, si le temps) idem sur `FormationPubliqueDto` + `toFormationDto`
  pour la liste — pas indispensable, l'encart est sur la fiche détail.
- Il n'existe **aucun** endpoint de création/édition de formation côté pédagogie
  (`GET /pedagogie/formations` renvoie juste `id/code/intitule`). Donc **pas de
  formulaire à câbler** — l'édition des 5 champs se fait via le JSON d'amorçage.
  Consigner ce constat (l'ancien plan disait « éditable en pédagogie » : faux).

### Étape 3 — Phase 3 front : écran pédagogie « Entreprises » (NOUVEAU)

Rien n'existe côté front pour les demandes entreprise du responsable
pédagogique. À créer, sur le patron de
`frontend/src/app/features/pedagogie/pages/formateurs/pedagogie-formateurs.component.*`
(liste + panneau de détail) :

- `frontend/src/app/features/pedagogie/pages/entreprises/pedagogie-entreprises.component.ts`
  (+ `.html`).
- Route dans `frontend/src/app/features/pedagogie/pedagogie.routes.ts` :
  `{ path: 'entreprises', loadComponent: () => import('./pages/entreprises/pedagogie-entreprises.component').then(m => m.PedagogieEntreprisesComponent) }`.
- Entrée de nav dans
  `frontend/src/app/features/pedagogie/layout/pedagogie-layout.component.ts`
  (tableau `navigation`, après « Suivi qualité ») :
  `{ libelle: 'Entreprises', lien: 'entreprises', icone: 'domain' }`.
- Service : soit étendre
  `frontend/src/app/features/pedagogie/services/pedagogie.service.ts`, soit un
  `pedagogie-entreprise.service.ts` dédié. Méthodes :
  - `getDemandesEntreprise()` → `GET /api/pedagogie/entreprises/demandes`
  - `repondreDemande(id, statut, reponse)` → `PUT /api/pedagogie/entreprises/demandes/{id}` (existe déjà côté back : `ReponseDemandeRequest{statut, reponse}`)
  - `getFiche(id)` → `GET /api/pedagogie/entreprises/demandes/{id}/fiche`
  - `enregistrerFiche(id, payload)` → `PUT /api/pedagogie/entreprises/demandes/{id}/fiche`
  - `transmettreFiche(id)` → `PUT /api/pedagogie/entreprises/demandes/{id}/fiche/transmettre`
- UI : liste des demandes (badge type `BESOIN_FORMATION`/`DEVIS_SESSION`, statut,
  `ficheStatut` s'il y en a un). Panneau détail : coordonnées + description +
  réponse texte libre + statut. Pour un `DEVIS_SESSION` (ou tout type, au choix),
  un bloc **Fiche Technique** = formulaire des 10 sections du gabarit (voir la
  liste de champs dans `FicheTechniqueModuleRequest`) + un mini-tableau **jour
  par jour** (numéro, contenus, durée h, type d'enseignement, modalités) avec
  « + ajouter une journée » / « retirer ». Boutons « Enregistrer le brouillon »
  et « Transmettre à l'entreprise » (désactivé tant que intitulé + objectif
  général + ≥ 1 jour manquent — reproduire `estTransmissible`). Afficher
  `commentaireEntreprise` quand `statut === 'AJUSTEMENTS_DEMANDES'`.
- Charte : suivre `CLAUDE.md §1` (jetons Tailwind, `font-*` **et** `text-*`
  ensemble, cartes `bg-surface-container-lowest rounded-xl border
  border-outline-variant shadow-sm`, boutons plats `bg-secondary
  text-on-secondary`).

### Étape 4 — Phase 3 front : espace entreprise, consultation + décision

- `frontend/src/app/features/espace-entreprise/models/entreprise.models.ts` :
  ajouter `StatutFicheModule`, interfaces `FicheModuleJour`,
  `FicheTechniqueModule`, et `ficheStatut: string | null` sur `DemandeEntreprise`.
- `frontend/src/app/features/espace-entreprise/services/entreprise.service.ts` :
  `getFiche(id)` → `GET /api/entreprise/demandes/{id}/fiche` ;
  `accepterFiche(id)` → `PUT .../fiche/accepter` ;
  `demanderAjustements(id, commentaire)` → `PUT .../fiche/ajustements`.
- Page de suivi des demandes de l'entreprise : identifier le composant qui
  affiche `getMesDemandes()` — regarder
  `frontend/src/app/features/espace-entreprise/pages/` (il y a `catalogue`,
  `messagerie`, `nouvelle-demande`, `mon-profil` ; **le suivi des demandes est
  probablement dans `nouvelle-demande.ts`** ou à créer — vérifier
  `espace-entreprise.routes.ts` + `entreprise-layout`). Quand
  `ficheStatut ∈ {TRANSMISE, ACCEPTEE, AJUSTEMENTS_DEMANDES}`, afficher un bloc
  **lecture seule joliment mis en forme** : les 10 sections + le planning en
  tableau. Actions « Accepter la fiche » / « Demander des ajustements » (zone de
  commentaire) tant que `!== ACCEPTEE`.

### Étape 5 — Phase 2 front : encart §3 sur la fiche catalogue

- `frontend/src/app/features/recrutement/catalogue/formation-detail/formation-detail.component.{ts,html}` :
  consommer les 5 nouveaux champs du `FormationDetailPubliqueDto` et afficher un
  encart « Conforme au référentiel DRCQ » (heures de simulation labo, anglais
  professionnel oui/non, soft skills oui/non, crédits, volume horaire) —
  **uniquement si au moins un champ est renseigné**.

### Étape 6 — Vérification (stack Docker locale, front nginx `:4200`)

1. `docker compose up -d --build backend` → vérifier le log Flyway
   `V35` **et** `V36` (`now at version v36`) et l'absence d'erreur au démarrage
   (le `CommandLineRunner` du catalogue tourne après « Started »).
2. `rm -rf frontend/.angular/cache && docker compose up -d --build frontend`.
3. **Pédagogie** (`test.pedago@inpg.sn` / `Test@2026`) : onglet « Entreprises »
   → une demande `DEVIS_SESSION` → Fiche Technique : en-tête + objectif général +
   3 jours → « Enregistrer » (`BROUILLON`) → « Transmettre » (`TRANSMISE`).
   Transmettre une fiche vide → 400.
4. **Entreprise** : il n'existe **pas** de compte `test.entreprise@…` dans
   `backend/db/seed-comptes-test.sql`. En créer un : se connecter avec un compte
   neuf (ou en créer un via `/auth/register`) et **soumettre une demande** via
   `POST /api/entreprise/demandes` — le rôle `ROLE_ENTREPRISE` est accordé à la
   première soumission (`EntrepriseController.soumettreDemande` →
   `garantirRoleEntreprise`). Puis : voir la fiche transmise, « Demander des
   ajustements » sans commentaire → 400 ; avec → repasse `AJUSTEMENTS_DEMANDES` ;
   « Accepter » → `ACCEPTEE` + demande `CLOTUREE`. Autre entreprise → `curl` sur
   la fiche → 404.
5. **Catalogue** : `GET /api/recrutement/public/formations/{id}` pour
   `ING-PET-GAZ-02` renvoie les 5 champs ; la fiche publique affiche l'encart.
6. `ng build` OK ; `mvnw -o test -Dtest='...'` verts (cf. étape 1).
7. Restaurer toute donnée de démo modifiée par les essais (CLAUDE.md §5 —
   cibler les `UPDATE`/`DELETE` par un `WHERE`).

### Étape 7 — Journalisation

- `LOG_BLOQUANT.md` : ajouter l'**entrée 65** — Fiche Technique de Module
  (résumé du modèle + workflow `BROUILLON→TRANSMISE→ACCEPTEE/AJUSTEMENTS`), le
  §3 catalogue (5 champs seed-only, pas d'écran d'édition), et les 2 points
  **consignés non construits** : renommage rôles DPI/DAF, mises en relation
  alumni↔entreprises.
- `CLAUDE.md` : rien d'obligatoire ; éventuellement une ligne §3 sur « la Fiche
  Technique est le seul livrable structuré de l'espace entreprise ».
- Traiter `Calendrier Formations INPG2026.pdf` (non lu) : voir ce qu'il implique
  (calendrier académique ? sessions ouvertes ? périodes des programmes) et
  proposer un plan au propriétaire avant de coder.

---

## 5. Plan complet d'origine (pour mémoire — le fichier `.claude/plans/…` ne suit pas le dépôt)

**Phase 3 — Fiche Technique de Module** : 1:1 avec `demandes_entreprise` ; table
`fiche_module_jour` pour le §4 ; enum `EnumStatutFicheModule` ; endpoints upsert
+ transmettre côté pédagogie, get + accepter + ajustements côté entreprise ;
`DemandeEntrepriseDto.ficheStatut` pour les listes ; notifications ; écran
pédagogie « Entreprises » (liste + éditeur) ; bloc lecture seule + décision côté
entreprise ; tests ciblés.

**Phase 2 — catalogue §3** : migration `V36` (5 colonnes) ; entité `Formation` ;
`CatalogueDataInitializer` + `catalogue-formations.json` ; exposer sur le
`FormationDetailPubliqueDto` + `PublicController` ; encart « Conforme au
référentiel DRCQ » sur `formation-detail`. Pas d'écran d'édition (aucun endpoint
CRUD formation n'existe).

**Écarté, à consigner** : renommage rôles LMS → DPI/DAF/… ; mises en relation
alumni ↔ entreprises (QMS-013).

---

## 6. Pièges d'environnement (déjà rencontrés cette session)

- **Java** : le défaut est Java 11, le projet exige 17.
  ```bash
  export JAVA_HOME=/usr/local/sdkman/candidates/java/17.0.15-ms
  export PATH=$JAVA_HOME/bin:$PATH
  ```
  `./mvnw` n'est pas exécutable → utiliser `sh mvnw`.
- **Maven hors-ligne** : `sh mvnw -o …` marche pour compile/test. `mvnw clean`
  **échoue** hors-ligne (`maven-clean-plugin` non caché) — pour forcer un
  rebuild, `rm -rf backend/target/classes backend/target/test-classes` à la main.
- **Piège shell** : `sh mvnw … | tail` ⇒ `$?` est celui de `tail`, pas de Maven.
  Utiliser `set -o pipefail` ou `${PIPESTATUS[0]}` pour un vrai code retour.
- **`mvn test` complet touche la base de recette** (`localhost:5432`, Flyway
  s'applique) — CLAUDE.md §2. Toujours cibler `-Dtest=…`.
- **Front** : après un rebuild, purger `frontend/.angular/cache` (monté dans le
  conteneur, sert des modules périmés). `nginx.local.conf` envoie déjà
  `Cache-Control: no-store` ; côté navigateur, un `Ctrl+Shift+R` une fois.
- **CRLF** : beaucoup de fichiers sont en CRLF sur le disque ; un remplacement
  multi-lignes écrit en `\n` échoue en silence. Détecter le séparateur avant
  d'écrire. Ne pas passer par `sed -i` / `Path.write_text()` sur ces fichiers.
- **Docker** : `docker compose up -d --build <svc>` ; `--build` ne suffit pas
  pour `.env` ou `.angular/cache` (vivent dans le montage) → `--force-recreate`
  ou `down`+`up` si besoin.
- **Après le merge** : migrations d'accréditation/entreprise renumérotées `V30→V32` … `V34→V36` (les V30/V31 de l'autre équipe passent avant). La base Docker **locale** doit être réinitialisée (voir l'encadré ⚠️ du §2). Entrée `LOG_BLOQUANT.md` : mon bloc accréditation est l'**entrée 64** (les 60–63 sont le travail recrutement/admission de l'autre équipe).

## 7. Comptes de test (mot de passe commun `Test@2026`)

| Rôle | Email |
|---|---|
| Responsable pédagogique | `test.pedago@inpg.sn` |
| Évaluateur accréditation | `test.evaluateur@inpg.sn` |
| École de formation | `test.ecole@inpg.sn` |
| Étudiant | `test.etudiant@inpg.sn` |
| Formateur (sans cours) | `test.formateur@inpg.sn` |
| Responsable admission | `test.admission@inpg.sn` |
| Alumni | `test.alumni@inpg.sn` |
| **Entreprise** | **aucun** — à créer (cf. §4 étape 6-4) |

Démo : `prof.demo@inpg.sn` / `Demo@2026`. Login = `POST /api/auth/login` avec
`{"email": "...", "password": "..."}` → renvoie `{ token, user, message }`.

---

## 8. Résumé pour le repreneur — checklist courte

- [ ] Lire ce fichier + entrée 60 de `LOG_BLOQUANT.md`.
- [x] ~~Étape 0~~ : constructeurs de test réparés pendant le merge. `test-compile` vert.
- [ ] Étape 1 : `FicheTechniqueModuleControllerTest` + lancer la suite ciblée.
- [ ] Étape 2 : exposer les 5 champs catalogue sur le DTO public.
- [ ] Étape 3 : écran pédagogie « Entreprises » (liste + éditeur de fiche + nav + route + service).
- [ ] Étape 4 : espace entreprise — vue fiche + accepter / ajustements.
- [ ] Étape 5 : encart §3 sur `formation-detail`.
- [ ] Étape 6 : rebuild Docker, vérifier Flyway V35/V36, tester les 3 rôles, `ng build`.
- [ ] Étape 7 : `LOG_BLOQUANT.md` entrée 61 ; analyser `Calendrier Formations INPG2026.pdf`.


---

## 9. Fichiers locaux NON versionnés à recréer (gitignore, ne suivent pas le push)

Pour monter la stack Docker locale (front statique nginx + proxy `/api`). Le
correctif `Cache-Control: no-store` du lot 1 est DANS ces fichiers, donc à
recréer à l'identique.

### `backend/.env`  (secrets — GÉNÈRE LES TIENS)
```
POSTGRES_DB=myappdb
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin
JWT_SECRET=<génère : openssl rand -base64 64>
JWT_EXPIRATION=6000000
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
APP_MAIL_FROM=no-reply@inpg.sn
APP_ADMISSION_EMAIL=admission@inpg.sn
APP_ACCREDITATION_EMAIL=accreditation@inpg.sn
APP_FRONTEND_URL=http://localhost:4200
CERTIFICAT_SIGNATURE_SECRET=<clé propre : openssl rand -base64 48>
CERTIFICAT_SIGNATURE_SECRETS_ANCIENS=<valeur précédente, si des certificats ont déjà été délivrés avec elle>
# Base neuve de production : comptes du personnel créés au premier démarrage (puis jamais modifiés)
APP_INIT_PASSWORD=<mot de passe initial robuste, à changer à la première connexion>
APP_INIT_ADMISSION_EMAIL=
APP_INIT_PEDAGOGIE_EMAIL=
APP_INIT_EVALUATEUR_EMAIL=
```
> Depuis le 2026-10-14, `CERTIFICAT_SIGNATURE_SECRET` est **obligatoire** et n'a plus de repli sur `JWT_SECRET`
> (le serveur refuse de démarrer sans elle). Un environnement où elle recopiait `JWT_SECRET` (consigne d'avant)
> garde la même valeur pour l'instant ; pour la rendre propre sans invalider les certificats imprimés : nouvelle
> valeur dans `CERTIFICAT_SIGNATURE_SECRET`, ancienne dans `CERTIFICAT_SIGNATURE_SECRETS_ANCIENS`.

### `docker-compose.override.yml`  (racine)
```yaml
services:
  frontend:
    build: !override
      context: ./frontend
      dockerfile: Dockerfile.local
    ports: !override
      - "4200:80"
    volumes: !reset []
    environment: !reset []
```

### `frontend/Dockerfile.local`
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration development

FROM nginx:1.27-alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.local.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### `frontend/nginx.local.conf`
```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-store" always;   # <-- correctif cache lot 1
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 20m;
    }

    location /uploads/ {
        proxy_pass http://backend:8080/uploads/;
        proxy_set_header Host $host;
    }
}
```

### Base de données
Après le merge (renumérotation V30→V32 … V34→V36), si tu réutilises une base
Docker qui avait déjà mes anciens V30–V32 : `docker compose down`, supprimer le
volume postgres (`docker volume rm <projet>_pgdata` ou équivalent), puis
`docker compose up -d` — Flyway rejoue V1→V36 proprement. Sur une base neuve :
rien à faire.
