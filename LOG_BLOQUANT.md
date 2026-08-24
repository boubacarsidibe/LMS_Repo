# Journal des points bloquants

Ce fichier consigne les blocages rencontrés par l'agent de développement autonome
(session cloud, branche `agent/dev-autonome`) : contradictions, ambiguïtés de
conception nécessitant un arbitrage humain, ou défauts pré-existants découverts en
cours de route. Chaque section est datée et ne réécrit jamais les précédentes.

---

## 2026-08-24

### 1. `docs/uml/release-2-enrichissement-pedagogique.drawio` n'existait pas

Le protocole de développement autonome (instructions de la tâche planifiée)
indique : « Modèle de classes cible déjà fait pour ces 3 epics :
`docs/uml/release-2-enrichissement-pedagogique.drawio` (XML texte, lisible
directement). Pars de ce modèle plutôt que d'en inventer un autre. »

Ni le dossier `docs/`, ni `docs/uml/`, ni ce fichier n'existaient sur `develop` au
moment de démarrer (aucune trace non plus de `docs/daily_logs/`). C'est
apparemment la première exécution de cet agent sur ce dépôt.

**Choix retenu** : plutôt que d'attendre un arbitrage humain pour un blocage qui
n'empêche pas d'avancer, j'ai créé le fichier moi-même en modélisant les classes
cibles des trois epics (Certificats sécurisés, E-learning, Espace Alumni) à partir
du TDR (`TDR - Mise en place du LMS - V02.docx`, sections B/E/C) et des entités déjà
présentes en base (`Certificat`, `ProgressionSupport`). Les classes déjà
implémentées sont marquées en vert, celles restant à créer en orange, avec une note
par entité expliquant les décisions de modélisation (notamment la réutilisation du
pattern `@MapsId` de `Candidat`/`Etudiant` pour `AlumniProfil`, et la séparation du
forum Alumni du forum de cours existant).

Ce modèle n'est **pas figé** : chaque story suivante doit le relire et, si
nécessaire, le corriger avant de coder — exactement comme le demande le protocole
pour un modèle qui existerait déjà.

### 2. `develop` ne compilait pas au moment de démarrer (backend)

`mvn compile` sur `origin/develop` (commit `1389a7a`) échouait avec deux erreurs,
strictement indépendantes de la story choisie (vérification publique d'un
certificat) :

- `CandidatController.java:604` — `TestAcademique.getLieu()` n'existe pas :
  l'entité `TestAcademique` n'a jamais eu de champ `lieu`.
- `CandidatController.java:724` — `CandidatureRepository.existsByNumeroDossier(String)`
  n'existe pas, alors que `findByNumeroDossier` existe déjà.

Reproduit sur un `git worktree` propre pointant sur `origin/develop`, en dehors de
toute modification de cette session — donc bien un défaut pré-existant, pas une
régression introduite ici.

**Choix retenu** (prudent, minimal, documenté) :
- `existsByNumeroDossier` : ajouté à `CandidatureRepository` — méthode dérivée
  Spring Data triviale, exactement du même moule que `findByNumeroDossier` déjà
  présente. Aucune ambiguïté de conception.
- `getLieu()` : la ligne `ligne.put("lieu", test.getLieu())` a été **supprimée**
  plutôt que le champ inventé. Comme le code ne compilait pas, cette ligne n'a
  jamais pu s'exécuter en production : la supprimer ne retire aucune
  fonctionnalité réellement disponible. Ajouter un champ `lieu` sur
  `TestAcademique` demanderait une migration Flyway et une décision produit (que
  représente ce lieu ? une salle ? un centre d'examen ? saisi par qui ?) qui
  dépasse le cadre de cette story et n'est pas dans mon mandat de trancher seul.
  **Suite à donner** : si le besoin est réel, le traiter comme une petite story à
  part dans le domaine recrutement (hors Release 2).

### 3. `develop` ne passait pas non plus le typecheck frontend

`npx tsc --noEmit -p tsconfig.app.json` sur la même base révélait deux erreurs
supplémentaires, également indépendantes de la story du jour :

- `responsable-admission-candidatures.component.ts:178` — appelle
  `this.recrutementService.validerPreselection(c.id)`, méthode absente du
  service alors que l'endpoint backend existe bel et bien
  (`PUT /api/admin/recrutement/candidatures/{id}/valider-preselection`,
  `AdminRecrutementController.java:388`). **Corrigé** : méthode ajoutée au
  service, sur le modèle exact de `admettreTestAcademique`/`admettreEntretien`/
  `admettreFinal` déjà présentes. Aucune ambiguïté : l'endpoint et son usage
  existaient déjà des deux côtés, seul le pont manquait.
- `responsable-admission-tests.component.ts:291` — construit un
  `ConvocationTestGroupeeRequest` sans le champ `lieu`, que l'interface TypeScript
  déclarait obligatoire. Vérification faite : ni le DTO backend
  `ConvocationTestGroupeeRequest.java`, ni le formulaire Angular
  (`convocationForm`), ni aucun autre appelant ne portent ce champ — c'est un
  reliquat d'un développement interrompu, exactement le même sujet que le point 2
  ci-dessus (« lieu de l'épreuve »), jamais raccordé côté backend. **Corrigé** en
  retirant `lieu` de l'interface TypeScript, pour la même raison qu'au point 2 :
  ce champ n'a jamais été fonctionnel, le retirer ne supprime rien qui marchait.

**Point de vigilance pour une future story** : le besoin « lieu de l'épreuve » a
donc été amorcé à trois endroits (entité, DTO de convocation groupée, formulaire)
sans jamais être branché de bout en bout. S'il est toujours souhaité, le
reconstruire proprement (migration + DTO + formulaire + affichage) en une seule
story plutôt que de raccommoder les bouts existants.

### 4. Pas de framework de tests unitaires frontend configuré

`tsconfig.json` référence `./tsconfig.spec.json`, qui n'existe pas, et
`package.json` ne déclare ni `jasmine`, ni `karma`, ni aucun autre framework de
test — alors que plusieurs fichiers `*.spec.ts` existent déjà dans le dépôt (ex.
`politique-confidentialite.spec.ts`) et ne peuvent donc pas s'exécuter tels quels.

**Choix retenu** : ne pas mettre en place toute une chaîne de test dans le cadre de
cette story (hors sujet, risque de conflit avec un choix d'outillage futur de
l'équipe). Le composant Angular ajouté aujourd'hui
(`VerificationCertificat`) n'a donc pas de test associé côté frontend ; la logique
qu'il expose est fine (appel HTTP + affichage) et est couverte côté backend par
`VerificationCertificatControllerTest` et `CertificatPdfServiceTest`. Point à
traiter par l'équipe : choisir et câbler un framework de test frontend
(vraisemblablement Jasmine/Karma vu les fichiers existants, ou migrer vers Jest).
