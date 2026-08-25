# Journal des points bloquants et décisions autonomes

Ce fichier consigne les doutes majeurs, conflits techniques ou ambiguïtés de conception
rencontrés par l'agent de développement autonome, avec la décision prise pour continuer
sans attendre d'arbitrage humain. Nouvelles entrées ajoutées en fin de fichier, section
datée — les entrées précédentes ne sont jamais réécrites.

---

## 2026-08-24 — Session d'amorçage (Epic 3, Certificats sécurisés)

### 1. Fichiers de pilotage absents du dépôt

Le prompt de cette session fait référence à `docs/uml/release-2-enrichissement-pedagogique.drawio`
(« modèle de classes cible déjà fait pour ces 3 epics ») et à `docs/daily_logs/` comme s'ils
existaient déjà. Vérification faite : ni l'un ni l'autre n'étaient présents dans le dépôt, et
aucune branche `agent/dev-autonome` n'existait — c'est la première exécution de cette routine
autonome sur ce dépôt.

**Décision** : créer les deux (`docs/uml/release-2-enrichissement-pedagogique.drawio` à partir
du modèle déjà en base pour Epic 3/4/5, et `docs/daily_logs/2026-08-24.md`) plutôt que d'attendre.
Le fichier UML ne couvre pour l'instant en détail qu'Epic 3 (implémenté cette session) ; Epic 4 et
Epic 5 y figurent à l'état de squelette (classes marquées « à construire ») pour amorcer le travail
d'une prochaine session, mais n'ont pas été validés avec un humain.

### 2. Base de développement (`develop`) ne compilait pas avant cette session

Avant toute modification liée à Epic 3, `mvn compile` et `npx tsc --noEmit` échouaient tous les
deux sur `develop` (commit `1389a7a`, dernier commit humain avant cette session) :

- Backend : `CandidatController.java` appelait `TestAcademique.getLieu()` (méthode inexistante —
  aucun champ `lieu` sur l'entité ni colonne en base) et `CandidatureRepository.existsByNumeroDossier(String)`
  (méthode inexistante — seule `findByNumeroDossier` existait).
- Frontend : `RecrutementService` n'exposait pas `validerPreselection(id)`, appelée par
  `responsable-admission-candidatures.component.ts` ; et l'interface TypeScript
  `ConvocationTestGroupeeRequest` déclarait un champ `lieu: string` obligatoire jamais fourni au
  point d'appel — et jamais lu côté backend (le DTO Java `ConvocationTestGroupeeRequest` n'a pas
  ce champ).

Vérifié par `git stash -u` puis nouvelle compilation sur l'arbre propre : les deux échecs sont bien
antérieurs à cette session, introduits par le commit `1389a7a` (« fix(recrutement): cabler les
elements cliquables sans action »), sans lien avec les certificats.

**Décision** : un dépôt qui ne compile pas bloque la vérification de tout travail, y compris celui
de cette story. Correction minimale, sans redesign ni nouvelle fonctionnalité :
- `existsByNumeroDossier` et `validerPreselection` : ajoutés en suivant exactement le patron des
  méthodes sœurs déjà présentes (`findByNumeroDossier`, `admettreTestAcademique`/`admettreEntretien`/
  `admettreFinal`), et l'endpoint backend `PUT /candidatures/{id}/valider-preselection` existait déjà
  côté `AdminRecrutementController` — seule la méthode de service frontend manquait.
- Le champ `lieu` (test académique et convocation groupée) a été **retiré** plutôt qu'ajouté en
  base : il n'existe dans aucune migration, dans aucun formulaire, ni dans le DTO backend de la
  convocation groupée. L'ajouter aurait supposé une décision produit (le lieu d'une épreuve
  est-il pertinent ? saisi où ? obligatoire ?) qui n'appartient pas à cette session. Si le besoin
  est réel, il reste à instruire : ajouter une colonne `lieu` à `tests_academiques` (migration),
  le champ sur `TestAcademique`, et le relier au formulaire de convocation — actuellement rien de
  tout cela n'existe malgré la référence isolée dans le code.

Ces quatre correctifs sont commités séparément de la story Certificats (voir le journal du jour),
par petites étapes atomiques, avant le travail sur Epic 3.

### 3. Choix d'implémentation pour la « signature électronique réelle » (Epic 3)

Le TDR demande une « signature et [un] cachet électronique préétablis » apposés sur le certificat.
Le cachet visuel existait déjà (dessin vectoriel dans `CertificatPdfService`, sans aucune valeur de
vérification). Une signature au sens PKI (certificat X.509, tiers de confiance, PAdES) est hors de
portée d'une story incrémentale et du besoin exprimé — aucune infrastructure de clés n'existe dans
ce dépôt.

**Décision** : implémenter une signature HMAC-SHA256 du contenu figé du certificat (référence,
nom de l'étudiant, intitulé, date, émetteur), calculée avec une clé connue uniquement du serveur
(`certificat.signature.secret`, par défaut = `jwt.secret` pour ne pas exiger de variable
d'environnement supplémentaire en développement). C'est un scellement d'intégrité vérifiable
publiquement (`GET /api/certificats/public/verifier/{reference}`), pas une signature électronique
qualifiée au sens juridique. À documenter clairement si l'établissement a besoin d'une valeur
légale opposable (PAdES avec un certificat qualifié) — ce serait alors une story distincte, avec
un fournisseur de confiance à choisir.

Corollaire : les certificats émis **avant** cette migration (V11) n'ont pas de signature
rétroactive — ils ne seraient pas reconstructibles à l'identique sans en modifier le contenu. La
vérification publique les traite comme « non vérifiables », jamais comme « invalides » : ce n'est
pas la même chose qu'une falsification.

### 4. Contenu exposé par la vérification publique

Le QR code d'un certificat est destiné à un tiers sans compte sur la plateforme (employeur,
établissement). La réponse de `GET /api/certificats/public/verifier/{reference}` inclut donc le nom
complet de l'étudiant et l'intitulé de la formation — c'est l'objet même d'une vérification de
diplôme, au même titre que ce qui est déjà imprimé en clair sur le document physique. Elle exclut
en revanche le matricule, l'identifiant technique de l'étudiant et tout ce qui permettrait de
rebondir vers d'autres données (promotion complète, email, téléphone). Choix documenté dans le
Javadoc de `CertificatVerificationDto` et vérifié par test (`CertificatPublicControllerTest`).

## 2026-08-24 — Passage 09:53 UTC (PR #25 fusionnée, amorçage de l'Epic 4)

### 5. `RoadMap_LMS_V1.pdf` absent du dépôt (jamais commité, pas seulement illisible)

Avant de choisir la story suivante, vérification faite à la racine et dans tout l'historique git
(`git ls-tree -r origin/develop`, `git log --all --diff-filter=A --name-only`) : aucun fichier
nommé `RoadMap_LMS_V1.pdf`, ni aucun `.pdf` de feuille de route, n'a jamais existé dans ce dépôt.
Ce n'est pas un problème de lecture (le TDR, lui, a été décompressé et lu sans peine) : le fichier
n'a simplement jamais été fourni. La session précédente (celle de la PR #25) ne l'a pas non plus
constaté explicitement dans son journal — elle a travaillé uniquement à partir du TDR.

**Décision** : conformément à la consigne « si le PDF est illisible, note-le et utilise
uniquement les autres sources », je détermine l'ORDRE des parties à partir de : (a) le TDR, qui ne
contient d'ailleurs aucune notion d'« epic » ou de « release » — cette structuration est une
organisation interne introduite par la session précédente pour regrouper les besoins du TDR
(section C « Espace Alumni », section B sur les certificats, section E « E-learning ») ; (b)
`docs/daily_logs/` et les PR déjà fusionnées, qui montrent qu'Epic 3 (Certificats) vient d'être
livré et que la session précédente avait explicitement prévu Epic 4 (messagerie
apprenant-formateur + réunions virtuelles, TDR section E) ensuite, avant Epic 5 (Alumni, TDR
section C) ; (c) `docs/uml/release-2-enrichissement-pedagogique.drawio`, qui porte déjà le
squelette « à construire » d'Epic 4 et Epic 5. Je poursuis donc cet ordre plutôt que d'en inventer
un autre : TDR section E (E-learning : « outil de communication entre les apprenants et le
formateur » + « réunions virtuelles ») est la partie non commencée qui suit directement les
certificats. Si un `RoadMap_LMS_V1.pdf` existe réellement côté client et donne un ordre différent,
il faudra le déposer dans le dépôt pour qu'une session future puisse le lire et corriger la
priorisation en conséquence.

## 2026-08-24 — Échange direct avec l'utilisateur, 13:51–14:09 UTC : changement de périmètre et confirmation sur la Roadmap

### 11. `RoadMap_LMS_V1.pdf` : confirmation directe de l'utilisateur, ne plus attendre ce fichier

Interrogé directement (hors du cycle horaire automatique) sur l'absence répétée de
`RoadMap_LMS_V1.pdf` (déjà constatée aux points 5 et suivants), l'utilisateur a choisi
explicitement : dériver l'ordre des epics du TDR seul, sans plus attendre ce fichier. Ce n'est donc
plus une hypothèse de session isolée mais une décision actée — la routine (prompt de
`trig_018TJeHoZWdiPfF3aF6jiGrT`) a été mise à jour en conséquence. Si ce fichier apparaît un jour
réellement dans le dépôt, il redevient prioritaire sur le TDR pour l'ORDRE (le TDR reste la source
du CONTENU).

### 12. Changement de périmètre : l'unité de travail devient l'EPIC, pas la story

L'utilisateur a demandé qu'à chaque activation de la routine horaire, un epic **entier** (toutes
ses stories) soit livré dans la même session, sans s'arrêter avant la fin, avec une seule PR à la
fin de l'epic — au lieu d'une story à la fois avec une PR par story comme jusqu'ici. La porte
d'approbation humaine (ÉTAPE 0) reste en place mais à l'échelle de l'epic : elle bloque le démarrage
d'un nouvel epic tant que la PR de l'epic précédent n'est pas fusionnée, mais ne s'applique plus
entre deux stories d'un même epic en cours. Prompt de la routine mis à jour en conséquence
(section « CHANGEMENT DE PÉRIMÈTRE » ajoutée en tête du prompt).

### 13. Doublon de routine détecté et partiellement nettoyé

Deux triggers identiques (« Dev autonome LMS INPG », cron `51 * * * *`) existaient et se
déclenchaient à la même minute, causant du travail en double (constaté à 13:51 UTC ce jour, deux
sessions ayant consigné la même attente de fusion de la PR #27 à une minute d'écart : commits
`c59d745` — perdu lors du nettoyage local, jamais poussé — et `b6d9b23`). L'ancien trigger
(`trig_01NNagahY1JTirjvMdsxaWCu`, créé le 2026-08-24 à 00:14 UTC via l'API HTTP, donc hors du
contrôle d'un agent) n'a **pas** pu être supprimé ni désactivé par l'agent — permission refusée
(« this routine was created via "http_api", not by an agent »). Seul l'utilisateur peut le
supprimer via le tableau de bord. Tant que ce n'est pas fait, deux exécutions peuvent continuer à
se déclencher à la même minute chaque heure ; seule celle liée à `trig_018TJeHoZWdiPfF3aF6jiGrT`
porte désormais le nouveau prompt « epic complet ».

### 6. Story choisie dans Epic 4 : messagerie apprenant-formateur (pas les réunions virtuelles)

La section E du TDR regroupe deux besoins sous « outil de communication » : une messagerie
apprenant-formateur, et des réunions virtuelles. Les réunions virtuelles supposent une intégration
avec un service tiers de visioconférence (Zoom, Jitsi, Meet…) — décision de fournisseur hors de
portée d'une session autonome, et rien dans le dépôt ne l'amorce. La messagerie, elle, peut
réutiliser des patrons déjà éprouvés dans ce dépôt (garde-fous de `MentionForumService`, cloche
`notification` adossée à l'utilisateur, cloisonnement `PerimetreFormateurService` /
`cours_formateur`) sans dépendance externe. Choisie comme la plus petite et la plus indépendante
des deux, conformément à la consigne de préférer une story incrémentale bien vérifiée.

### 7. Correction du point 6, avant tout code écrit : la messagerie existe déjà (le forum)

En préparant l'implémentation (lecture du code existant du forum avant d'écrire la moindre ligne),
javadoc de `ForumMessage.java` (`backend/src/main/java/inpg/sn/lms/espace_etudiant/entity/`) :
« C'est aussi le canal de communication apprenant ↔ formateur demandé au §3.5 du cahier des
charges : l'auteur est un `User`, ce qui couvre indifféremment un étudiant ou un formateur. » —
c'est-à-dire qu'un développeur (humain ou session précédente) a déjà tranché explicitement que le
forum, dès lors qu'un sujet est rattaché à un `Cours`, remplit le besoin TDR §3.5 « outil de
communication entre les apprenants et le formateur ».

Construire malgré tout `Conversation`/`MessagePrivé` (comme le squelette du `.drawio` le
suggérait) aurait recréé exactement le défaut que la section 3 de `CLAUDE.md` documente à
plusieurs reprises comme la cause de bugs déjà survenus trois fois dans ce dépôt : deux chaînes
concurrentes répondant à la même question métier, qui finissent par diverger (`cours_promotion` vs
la dérivation promotion→formation→cours, `evaluations_cours`/`POST /retours` vs
`retour_evaluation`). Le point 6 ci-dessus est donc **corrigé, pas effacé** (conformément à la
consigne de ne jamais réécrire une entrée précédente) : je n'implémente pas la messagerie.

**Nouvelle story retenue** : l'autre moitié du besoin de communication de la section E du TDR,
« la possibilité de réunions virtuelles afin de faciliter l'apprentissage des étudiants », qui
elle n'est *pas* déjà couverte. Vérification faite avant de coder (pas seulement en lecture, en
suivant la chaîne bout en bout) : `Seance` (entité `espace_etudiant/entity/Seance.java`) a déjà un
champ `lieuOuLien`, déjà exposé par `SeanceRequest`/`SeanceDto`/`SeanceEtudiantDto` et déjà
correctement cloisonné côté lecture étudiante (`ScolariteController.monCalendrier` ne renvoie que
les séances des cours autorisés via `HabilitationCoursService`) — mais :
- côté formateur, aucune page ne permet de le renseigner : le seul point de création rapide
  (`SeanceService.creer()` dans `mes-cours.ts`, utilisé par la prise de présence) envoie
  systématiquement `lieuOuLien: null` ;
- côté étudiant, `calendrier-etudiant.ts` et `tableau-bord.ts` affichent `lieuOuLien` en texte
  brut (`{{ s.lieuOuLien || 'Lieu à préciser' }}`), jamais comme un lien cliquable — même si sa
  valeur est une URL de visioconférence, l'étudiant ne pouvait pas la rejoindre en un clic.

C'est exactement le même type de défaut « champ présent en base mais chaîne rompue avant l'écran »
que celui déjà documenté dans `CLAUDE.md` (le piège du champ `lieu` retiré au point 2 de ce
journal, dans l'autre sens : ici le champ existe et doit être raccordé, pas retiré). Corrigé cette
session, en frontend uniquement (aucune migration, aucune entité, aucun endpoint nouveaux — tout
le nécessaire backend existait déjà et est resté inchangé) :
- `mes-cours.ts`/`mes-cours.html` : un bouton d'édition par séance dans la liste du modal de
  présence, qui appelle `PUT /formateur/seances/{id}` (endpoint déjà existant et déjà cloisonné
  par `findOwnedSession`/`findOwnedCourse`) pour ne changer que `lieuOuLien`.
- `calendrier-etudiant.ts` et `tableau-bord.ts` : un lien « Rejoindre » (`target="_blank"`,
  `rel="noopener"`) affiché uniquement quand `lieuOuLien` commence par `http://`/`https://`
  (`estLienVisio()`), sinon le texte brut est conservé (cas d'un lieu physique).
- Aucun fournisseur/API tierce (Zoom, Meet…) intégré : le formateur colle un lien déjà créé par
  ses propres moyens, exactement comme le fait déjà `Evenement.lienVisio` (calendrier
  administratif) ailleurs dans ce même dépôt — cohérent avec l'existant plutôt qu'une nouvelle
  approche.

`docs/uml/release-2-enrichissement-pedagogique.drawio` mis à jour en conséquence : la case
`Seance.lieuOuLien` passe en « modifié » (jaune), la case `Conversation`/`MessagePrivé` est
requalifiée « remis en question » avec ce raisonnement, jamais implémentée cette session.

## 2026-08-24 — Passage suivant (PR #26 fusionnée, Epic 5 — Espace Alumni)

### 8. Story choisie : fondations de l'Espace Alumni, forum et partage de documents exclus

TDR section C : « La plateforme doit comprendre une section dédiée aux Alumni afin de
regrouper les anciens étudiants de l'INPG avec la possibilité d'avoir un suivi de leur
situation professionnelle. Ces utilisateurs devront avoir accès à leurs relevés et
autres informations qui seront définies par l'INPG. Un forum de discussion et
d'échange leur sera ouvert, de même que la possibilité de partager des documents. »

Vérification faite avant de coder (audit du modèle existant) : ni rôle `ROLE_ALUMNI`,
ni notion de « diplômé »/« ancien étudiant », ni conversion Étudiant → Alumni
n'existaient. Construits cette session en suivant exactement le patron déjà en place
pour Candidat → Étudiant (`AdminRecrutementController#inscrireCommeEtudiant`) : un
compte ne détient qu'un seul rôle à la fois, la ligne `Etudiant` d'origine n'est pas
supprimée à la conversion (elle garde le matricule et l'accès au relevé délivré,
exactement comme la ligne `Candidat` reste après le passage à étudiant).

**Décision de périmètre** : seules les parties « suivi de la situation
professionnelle » et « accès aux relevés » sont livrées cette session (`AlumniProfil`,
`SituationProfessionnelle`, migration V12, `AlumniController`). Le forum et le
partage de documents demandés par la même phrase du TDR ne le sont pas :

- **Forum** : `ForumSujet.cours` est aujourd'hui soit rattaché à un cours (visible des
  étudiants/formateurs de ce cours), soit général (`cours == null`, visible de « tout
  l'espace étudiant »). Rien ne distingue aujourd'hui *quelle catégorie* d'utilisateurs
  voit un sujet général. Ouvrir le forum aux alumni sans introduire cette distinction
  exposerait par erreur les fils étudiants aux alumni (ou l'inverse) — la même famille
  de bug que `cours_promotion` documentée en §3 de `CLAUDE.md` : une question métier
  répondue par un modèle qui ne la représente pas encore. Ajouter une portée
  (`ForumSujet.portee` : ÉTUDIANT / ALUMNI / MIXTE, ou équivalent) est une décision de
  modèle à instruire à part, pas une extension mécanique.
- **Partage de documents** : le mécanisme de `Phototheque`/`PhotoStorageService`
  (upload, `ValidationFichier`, stockage) est directement réutilisable pour un futur
  `DocumentPartage` alumni — mais sans forum alumni fonctionnel, il n'y a pas encore
  d'écran naturel où l'accrocher (à qui un document est-il visible : à quels alumni,
  sur quelle portée ?). Cette story reste donc **la plus petite et la plus
  indépendante** en s'y limitant, conformément à la consigne « une story incrémentale
  bien vérifiée vaut mieux qu'une partie entière à moitié faite ».

### 9. `ROLE_ALUMNI` : un vrai rôle Spring Security, pas un flag sur `Etudiant`

Deux options étaient possibles pour marquer un compte comme alumni : un booléen sur
`Etudiant`, ou un rôle au sens plein (table `roles`, `SecurityConfig`,
`@PreAuthorize`). Choix du second : tout le système d'autorisation de ce dépôt (filtre
d'URL **et** `hasRole`/`hasAnyRole` en double protection, cf. commentaire de
`PhotothequeController`) est bâti autour des rôles Spring Security, et
`EspaceEtudiantGardeFousTest` (cf. `CLAUDE.md` §3) attend ce modèle sur les contrôleurs
qu'il couvre. Un flag isolé aurait fonctionné pour la question métier (« cet étudiant
est-il diplômé ? ») mais pas pour l'accès HTTP, qui doit rester un rôle pour bénéficier
des mêmes protections que les autres espaces.

Corollaire du modèle « un seul rôle à la fois » déjà en vigueur : passer un compte en
`ROLE_ALUMNI` lui retire `ROLE_ETUDIANT`, donc l'accès à l'espace étudiant. C'est le
comportement voulu — un alumni a *terminé* sa scolarité active — et conforme à la
manière dont `ROLE_CANDIDAT` est déjà retiré au profit de `ROLE_ETUDIANT` lors de
l'admission. Si un besoin réel de cumul apparaissait (ex. un alumni qui reprend une
formation), ce serait une story à part : le modèle actuel de rôle unique par compte
est une décision d'architecture antérieure à cette session, non remise en cause ici.

### 10. Pas de déclenchement automatique à la fin de promotion

`Promotion.estEnCours()` existe déjà (dérivé de `dateDebut`/`dateFin`) mais n'est PAS
utilisé pour promouvoir automatiquement un étudiant en alumni à la fin de sa
promotion : une promotion peut se terminer sans que tous ses étudiants aient
effectivement validé leur formation (échec, abandon, redoublement), et rien dans le
modèle actuel ne distingue ces cas. La conversion reste donc un geste explicite du
responsable pédagogique (`PUT /api/pedagogie/etudiants/{id}/promouvoir-alumni`,
bouton « Marquer alumni » dans le répertoire des étudiants), à l'image de
l'affectation de promotion qui est elle aussi manuelle. Si un besoin de bascule
automatique à la date de fin de promotion se confirme, une story dédiée devra d'abord
définir comment distinguer un diplômé d'un abandon.

## 2026-08-24 — Passage 16:51 UTC (PR #27 fusionnée, epic « forum et documents alumni »)

### 14. Forum alumni : portée à deux valeurs, pas trois, et correction d'une fuite découverte au passage

Le point 8 proposait `ForumSujet.portee` : ÉTUDIANT / ALUMNI / MIXTE, « ou équivalent ».
Décision : seulement `ETUDIANT` et `ALUMNI`. Rien dans le TDR ne demande de fil partagé
entre étudiants et alumni, et ajouter une valeur MIXTE sans besoin exprimé aurait été
concevoir pour un besoin hypothétique — une valeur d'enum supplémentaire reste une
migration additive si le besoin se confirme réellement, pas une refonte.

En cartographiant le forum avant de coder (comme la méthode l'exige), découverte d'un
défaut préexistant, sans rapport direct avec l'espace alumni mais touchant le même
modèle : `EspaceEtudiantController#listSujets` (un second contrôleur qui sert les
mêmes entités `ForumSujet`/`ForumMessage` sous un préfixe différent, `/api/espace-
etudiant/sujets` plutôt que `/api/espace-etudiant/forum/sujets`) appelait
`forumSujetRepository.findAll()` **sans aucun filtre** — un étudiant authentifié
pouvait lister tous les sujets de tous les cours, y compris ceux hors de son cursus,
en appelant directement cet endpoint. `ForumController`, lui, filtrait correctement
via une méthode privée `sujetsVisibles`. Corrigé en extrayant cette logique dans
`ForumVisibiliteService`, désormais partagée par les deux contrôleurs : la migration
V5 (« fusionner les deux forums ») avait déjà unifié le modèle de données, mais pas
les contrôleurs qui le servent — c'est fait maintenant, a minima pour la lecture et
les actions de modération (`repondre`, `toggle-urgence`, `resoudre`, `supprimer`), qui
reçoivent toutes un garde-fou de portée. Les actions de modération de ce second
contrôleur n'ont en revanche toujours aucune vérification d'appartenance à un cours
(un étudiant peut répondre à un sujet d'un cours qui n'est pas le sien via cet
endpoint) : défaut préexistant, plus large que cette story, non corrigé ici — à
instruire séparément si confirmé comme un vrai problème.

### 15. Partage de documents alumni : pas de stratification par promotion

L'esquisse initiale du `.drawio` prévoyait `DocumentPartage.visiblePar` (PROMOTION /
TOUS_ALUMNI). Non retenu : `AlumniProfil` ne porte aucun champ promotion (un alumni a
terminé sa scolarité active, cf. migration V12) — il n'y aurait rien à quoi rattacher
un filtre par promotion sans ajouter un champ que rien d'autre ne réclame. Le TDR ne
demande d'ailleurs aucune stratification (« la possibilité de partager des documents »,
sans plus de précision). Un seul espace de partage, plat, ouvert à tous les alumni —
migration V14. Réutilise le mécanisme de stockage de la photothèque
(`PhotoStorageService`/`ValidationFichier`) plutôt qu'un nouveau système de fichiers.

## 2026-08-24 — Passage 21:37 UTC (PR #28 fusionnée, epic suivant)

### 16. TDR section D (compatibilité SIGB) : bloquée, le système cible n'existe pas encore

Dans l'ordre du TDR, la partie suivante après l'Espace Alumni est la section D,
« Compatibilité avec le SIGB interne ». Relecture attentive du texte avant de
choisir cette partie : « L'Institut **ambitionne de disposer** d'un SIGB (Système
Intégré de Gestion de Bibliothèque), et par conséquent, la solution LMS devrait
pouvoir permettre l'accès fluide au SIGB **qui sera en place**. » Le temps du verbe
et la formulation sont sans ambiguïté : ce SIGB n'existe pas encore à l'INPG. Aucun
système, aucune API, aucun format d'échange, aucun protocole n'est nommé nulle part
dans le TDR ni ailleurs dans le dépôt.

**Décision** : ne pas deviner d'intégration technique avec un système qui n'existe
pas — construire quoi que ce soit ici (un connecteur, une API, un modèle
d'ouvrages) serait fabriquer une spécification que personne n'a écrite, avec un
risque élevé de devoir tout refaire une fois le vrai SIGB choisi (produit,
protocole d'échange — Z39.50, SIP2, API REST propriétaire — dépendent entièrement
du logiciel retenu). Conformément à la consigne « ne pas deviner, consigner le
blocage », cette partie est documentée ici comme bloquée et **non traitée** cette
session. Elle redeviendra actionnable dès qu'un humain communiquera : le nom du
logiciel de SIGB retenu (ou envisagé), et son protocole/API d'intégration.

**Choix pour continuer** : plutôt que d'immobiliser toute la routine sur une partie
qui restera bloquée tant que cette information n'existe pas, passage à la partie
suivante réellement actionnable dans l'ordre du TDR — section F, « Accréditation
des écoles de formation » — entièrement spécifiée (formulaire de soumission,
critères, suivi d'état, espace évaluateurs, notifications, base des écoles
accréditées). Section D reste en tête de la prochaine liste « suite prévue » tant
qu'elle n'est pas débloquée par l'utilisateur.

### 17. Accréditation des écoles : trois choix de modèle volontairement minimalistes

En construisant l'epic F (`EcoleFormation`/`DemandeAccreditation`/
`PieceAccreditation`/`CritereAccreditation`, migration V15), trois décisions
prises pour ne pas construire au-delà de ce que le TDR demande :

- **Pas de `numeroDossier`** : contrairement à `Candidature` (`numeroDossier`
  généré, affiché à l'école), une demande d'accréditation s'identifie par son id
  technique. Le TDR ne demande aucune référence humainement lisible pour ce
  parcours plus simple (une seule demande active à la fois par école, cf. index
  partiel), et en fabriquer une aurait été une convention inventée sans usage
  identifié.
- **`CritereAccreditation` sans grille de notation structurée** : le TDR demande
  de « définir et gérer les critères d'accréditation », pas de barème ou de
  système de notation par critère. `CritereAccreditation` reste un référentiel
  admin-géré (libellé, description, actif) que l'évaluateur consulte en
  rédigeant son `commentaireEvaluation`/`motifRejet` en texte libre — construire
  une table de scores par critère aurait été une spécification inventée.
- **Rôle `ROLE_EVALUATEUR_ACCREDITATION` assigné via l'écran générique existant**
  (`AdminController#changeUserRoles`, `responsable-users.component.html`) plutôt
  qu'un nouvel écran dédié : ce rôle n'ouvre pas d'espace où l'utilisateur
  s'auto-inscrit (contrairement à `ROLE_ECOLE_FORMATION`, accordé à la première
  soumission) — c'est un rôle de staff, assigné par un responsable admission,
  exactement le patron déjà en place pour tous les autres rôles internes. Le
  dropdown de ce fichier n'avait pas non plus d'entrée pour `ROLE_ALUMNI` (rôle
  auto-attribué, cohérent avec cette distinction) — seule `ROLE_EVALUATEUR_
  ACCREDITATION` a été ajoutée ici.

## 2026-08-25 — Epic « nettoyage de l'existant » (à la demande directe de l'utilisateur)

### 18. `AcademicTestController` et une partie d'`InterviewController` : modules orphelins, non touchés

En cherchant où était réellement consommé `SettingsController.rules` (seuils de
décision), découverte que `AcademicTestController` (`/api/academic-tests/**`,
6 endpoints) n'est appelé par **aucun** composant frontend : les méthodes de
`RecrutementService` qui lui correspondent (`getAcademicTestKpis`,
`listAcademicExams`, `getExamScores`, `saveExamScores`…) sont bien définies
mais jamais invoquées nulle part. L'écran réel « Tests académiques »
(`responsable-admission-tests.component.ts`) implémente sa propre logique
côté client, avec son propre calcul de seuil (`seuilPour`, qui lit le vrai
`SessionCandidature.seuilReussiteTest`) — sans jamais appeler ce contrôleur.
`InterviewController#saveEvaluation` calcule de son côté une décision à partir
d'un seuil 60/40 codé en dur, sur une échelle /100, déconnecté à la fois du
seuil réel de la session (/20) et de `SettingsController`.

**Décision** : ne pas toucher à ces deux points. Corriger `AcademicTestController`
reviendrait à retaper du code mort ; lui donner un vrai seuil configurable, ou en
donner un à `InterviewController`, est une décision de modèle (quelle échelle,
portée par session ou globale, qui la définit) qui n'appartient pas à une story
de nettoyage — à instruire séparément si un besoin réel est confirmé. Seul
`SettingsController.getRules/updateRules` a été reconnecté, sur la seule donnée
qui a un vrai support en base (`SessionCandidature.seuilReussiteTest`).

### 19. Page « Paramètres » (responsable admission) : seul l'onglet « Règles » a été reconnecté

La page entière (`responsable-admission-settings.component`) était un
prototype non branché : onglets Général, Règles, Équipe et Modèles éditaient
tous des champs purement locaux, avec un bouton « Enregistrer » qui ne faisait
qu'afficher un toast de succès sans jamais appeler le backend — y compris
l'onglet Équipe, qui gérait une liste `users` fictive en mémoire. Seul
l'onglet Règles a été reconnecté (seul son besoin — le seuil de réussite —
avait un backend réel à joindre, voir point 18). Les onglets Général, Équipe
et Modèles restent des prototypes non fonctionnels : les reconnecter
suppose de construire les endpoints/tables qui leur correspondent (gestion des
comptes staff, paramètres d'établissement, modèles d'email persistés — ces
derniers existent déjà côté backend via `AdminRecrutementController`, mais
cette page ne les appelle pas) — hors du périmètre « corriger l'existant sans
changer le cœur » fixé pour cette session.

### 20. Audit font-`<échelle>`/text-`<échelle>` : 52 occurrences corrigées, 5 laissées de côté

Script de vérification exhaustif (grep structuré sur tous les `class="…"` du
frontend, `.html` et gabarits inline `.ts`) : 52 occurrences d'une classe
`font-<échelle>` sans aucune classe `text-<échelle>` correspondante — le même
piège que les 252 déjà corrigés (CLAUDE.md §1), retombant en 16 px/400 au lieu
de la taille prévue. Concentrées presque entièrement dans le module
`responsable-admission` (recrutement), le plus ancien et le moins repassé par
les sessions précédentes. Toutes corrigées en insérant la classe `text-<même
échelle>` juste après, à l'identique du patron déjà en place partout ailleurs.
Une occurrence responsive (`md:font-headline-md`) avait été mal corrigée par
le script (texte inséré sans le préfixe `md:`) — repérée en relisant le diff
et corrigée à la main.

**5 occurrences volontairement non touchées** : `font-label-sm`/`font-label-md`
combinées à une taille Tailwind brute (`text-sm`, `text-[11px]`, `text-[10px]`)
déjà choisie explicitement, différente de la taille canonique de l'échelle
(12px/14px). Les corriger aurait changé la taille visible d'un badge ou d'un
onglet déjà réglé à une valeur précise, sans pouvoir le vérifier à l'écran
dans cet environnement (pas de navigateur) — laissées en l'état plutôt que
risqué une régression visuelle non vérifiable : `mon-parcours.html:31`,
`retours-evaluations.html:103-105`, `forum.html:69`.

**Non touché, à part** : les 8 occurrences `font-display` associées à
`text-headline-lg/md/sm` (une échelle différente) ne sont pas un défaut — les
dix échelles de `tailwind.config.js` partagent toutes la même famille
(`Inter`), donc `font-display` et `font-headline-lg` produisent exactement le
même rendu de police ; seule la classe `text-*` détermine la taille réelle.

## 2026-08-25 — Epic « affichage global et parcours par rôle » (demande directe, échéance 10h)

### 21. Écoles accréditées : décision produit qui va à l'encontre du TDR §F

Le TDR §F demande explicitement une liste des écoles accréditées en accès
public (« processus transparent »), implémentée comme telle lors de l'epic
Accréditation (migration V15, `AccreditationPublicController`,
`permitAll()` dans `SecurityConfig`). L'utilisateur, interrogé directement
sur le périmètre de ce nouvel epic (« que ne doit-on pas voir »), a demandé
que cette liste soit réservée aux comptes connectés.

**Décision** : suivre l'instruction directe de l'utilisateur, qui prime sur
le TDR pour une décision produit explicite. `SecurityConfig` ne classe plus
`/api/accreditation/public/**` en `permitAll()` — la requête tombe
désormais sur le `.anyRequest().authenticated()` final (tout rôle confondu,
aucune restriction par rôle spécifique). Route frontend `/ecoles-accreditees`
protégée par `authGuard`. Le préfixe d'URL (`.../public/...`) et le nom de
la classe (`AccreditationPublicController`) restent inchangés pour ne pas
casser l'appel frontend existant ni élargir le diff — seule l'autorisation
change ; les commentaires du contrôleur et de la route sont mis à jour pour
refléter l'état réel plutôt que le nom.

### 22. Nettoyage navigation : rien à retirer, un orphelin repéré mais non supprimé

Audit systématique de tous les layouts/barres latérales de l'application
(étudiant, formateur, responsable admission, pédagogie, alumni, évaluateur,
école) : chaque entrée de navigation pointe vers une route réelle et
fonctionnelle (service HTTP réel, pas de données figées). Aucun lien mort à
retirer.

Un seul reliquat, déjà identifié lors d'un epic précédent : le composant
`responsable-admission/settings/responsable-admission-settings.component.*`
(onglets Général/Équipe/Modèles) n'est déclaré dans aucune route et n'est
référencé nulle part — c'est un prototype orphelin, distinct du vrai composant
`.../parametres/...` qui, lui, est routé et fonctionnel. Confirmé inoffensif
(aucun lien n'y mène, donc rien à masquer côté navigation) mais sa suppression
a été bloquée par le classeur de permissions de la session autonome sur des
commandes de suppression de fichiers ; laissé en l'état, à supprimer à la
prochaine intervention humaine ou lors d'un epic dédié au nettoyage du code
mort.
