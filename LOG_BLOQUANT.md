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

### 23. Pipeline GitHub Actions à l'arrêt depuis le 10 août — cause hors de portée de l'API

`.github/workflows/ci.yml` ne s'est plus exécuté sur aucun push depuis le run
#43 du 2026-08-10 (en échec), malgré des dizaines de fusions sur `develop` et
`main` depuis (dont toutes celles du 2026-08-25). Aucun outil `mcp__github__*`
disponible dans cette session n'expose les réglages « Actions permissions » du
dépôt (`GET/PUT /repos/{owner}/{repo}/actions/permissions`) pour vérifier ou
corriger un éventuel blocage à ce niveau.

Tentative de diagnostic : déclenchement manuel via `actions_run_trigger`
(`run_workflow`) sur `develop`. Résultat : erreur **« Workflow does not have
'workflow_dispatch' trigger »** — une erreur de configuration du workflow
(`ci.yml` ne déclare que `push`/`pull_request`, pas `workflow_dispatch`), pas
une erreur d'accès ou de quota. Ça exclut un blocage total (« Actions
désactivées sur le dépôt » aurait probablement échoué autrement, avant même
d'atteindre la validation du déclencheur) mais ne dit pas pourquoi les
événements `push` eux-mêmes ne déclenchent plus rien.

**Décision** : hors de portée des outils disponibles ici — nécessite une
vérification humaine directe sur `Settings → Actions → General` du dépôt
(état « Actions permissions », historique de facturation/minutes consommées),
et éventuellement d'ajouter un déclencheur `workflow_dispatch` à `ci.yml` pour
permettre un test manuel propre à l'avenir. Ne bloque pas le reste de cette
session ni des suivantes — la propagation vers `main` (ÉTAPE 0) et le travail
sur `develop` continuent sans dépendre de la CI.

### 24. Pipeline GitHub Actions à l'arrêt — cause réelle identifiée : workflow fantôme sur un chemin corrompu

Suite du point 23. `mcp__github__actions_list` expose deux `workflow_id` bien
distincts sur ce dépôt :

- **317742837** — le vrai `Spring Boot CI/CD Pipeline`, chemin
  `.github/workflows/ci.yml`, `state: active`. Son dernier run réel est le
  run #43 du 2026-08-10 (`conclusion: failure`) — rien depuis, malgré des
  dizaines de fusions sur `develop`.
- **331865476** — un workflow nommé `""`, de chemin littéral `BuildFailed`
  (pas un chemin de fichier valide), `state: deleted`, créé le 2026-08-11.
  **C'est lui qui reçoit tous les événements `push` sur `develop` depuis** :
  un nouveau run apparaît à chaque fusion (jusqu'au dernier en date,
  2026-08-25T14:43:48Z), toujours avec `conclusion: startup_failure` —
  GitHub tente de déclencher un workflow qu'il considère supprimé, échoue
  immédiatement avant même d'atteindre `ci.yml`.

Le fichier `.github/workflows/ci.yml` lui-même a été relu intégralement :
YAML valide, déclencheurs `push`/`pull_request` corrects sur
`develop`/`preprod`/`main`. Ce n'est donc ni un problème de contenu, ni un
quota épuisé, ni un réglage « Actions désactivées » — c'est une association
chemin → workflow corrompue côté GitHub, gelée sur l'ancien chemin
`.github/workflows/ci.yml` depuis un événement du 2026-08-11 (nature exacte
inconnue : renommage, ré-écriture d'historique, ou incident de
synchronisation de la plateforme). Ce que confirme `get_workflow` sur
l'identifiant fantôme : `path: "BuildFailed"` — littéralement le nom du
« check » que GitHub affiche pour un run qu'il ne sait plus rattacher à un
fichier réel.

**Correctif appliqué** (PR issue de cette investigation) : renommage de
`.github/workflows/ci.yml` en `.github/workflows/build.yml` (contenu
inchangé, `git mv` pour préserver l'historique), plus ajout d'un déclencheur
`workflow_dispatch: {}` pour permettre un test manuel à l'avenir sans
attendre un push. Le changement de chemin force GitHub à enregistrer une
troisième identité de workflow, fraîche, sans lien avec l'association
corrompue — hypothèse testable dès le prochain push sur `develop`.

**À vérifier au prochain passage de la routine** : si le premier run après
la fusion de cette PR porte un nouveau `workflow_id` (ni 317742837 ni
331865476) avec une vraie exécution (pas `startup_failure` immédiat), le
correctif est confirmé — mettre à jour ce point plutôt que d'en ouvrir un
nouveau. Si le fantôme `331865476` continue malgré le renommage, la cause
est plus profonde (réglage d'organisation ou de dépôt hors de portée de
l'API `mcp__github__*` disponible ici) et nécessite une vérification humaine
directe sur `Settings → Actions` du dépôt.

Le PR Copilot ouverte #21 (`copilot/fix-failing-github-actions-job`,
brouillon depuis le 2026-07-21) traite un problème différent et bien plus
ancien (chemin d'exécution Maven à la racine au lieu de `backend/`, déjà
corrigé depuis dans `ci.yml` via `defaults.run.working-directory`) — sans
rapport avec le fantôme `BuildFailed` découvert ici. Laissée telle quelle,
hors du périmètre de cette investigation.

**Vérification faite dans la foulée (même passage), hypothèse du chemin corrompu invalidée.**
Après fusion de la PR de renommage, `list_workflows` confirme l'enregistrement
d'une troisième identité fraîche (`workflow_id` **342182978**, chemin
`.github/workflows/build.yml`, `state: active`) — le renommage a bien eu
l'effet attendu sur l'enregistrement. Mais :

- Le push de fusion (`2fd8231`) a quand même généré un nouveau run sur le
  fantôme `331865476` (run #137, `conclusion: startup_failure`) — le fantôme
  n'est pas lié au chemin `ci.yml` spécifiquement, il intercepte les push sur
  `develop` en général.
- Plus décisif : `list_workflow_runs` sur le nouvel identifiant **342182978**
  reste à `total_count: 0` plusieurs dizaines de secondes après le push — le
  vrai workflow, sur un chemin propre, ne se déclenche pas non plus sur
  `push`.
- Déclenché manuellement via `workflow_dispatch` (désormais disponible sur
  `build.yml`) : la requête est acceptée (`204 Workflow run has been
  queued`), un run apparaît bien (`id 32862597408`, `event:
  workflow_dispatch`) — mais avec `conclusion: startup_failure` **et 0 job
  créé** (`list_workflow_jobs` : `total_count: 0`, aucun log disponible).

Un run sans le moindre job créé, y compris pour un déclenchement manuel
explicite sur un workflow tout juste enregistré et parfaitement valide,
exclut définitivement un problème de contenu, de chemin, ou d'association
fantôme : GitHub accepte la demande d'exécution mais ne parvient jamais à
provisionner le moindre runner. C'est la signature typique d'un blocage au
niveau du compte/dépôt lui-même — le plus probable étant une limite de
dépenses (« spending limit ») atteinte ou nulle sur les minutes Actions, ou
un blocage explicite des Actions au niveau du dépôt/organisation
(`Settings → Actions → General`, ou `Settings → Billing → Plans and usage`)
— aucun des deux n'est vérifiable ni corrigible par les outils
`mcp__github__*` disponibles ici (pas d'endpoint exposant
`GET /repos/{owner}/{repo}/actions/permissions` ni la facturation du compte).

**Décision finale de cette investigation** : le renommage reste en place
(changement neutre à légèrement positif — chemin propre, `workflow_dispatch`
disponible pour un nouveau test dès que la cause réelle sera levée), mais
n'est **pas** le correctif. La cause racine est confirmée hors de portée de
l'API : **vérification humaine indispensable** sur `Settings → Actions →
General` (Actions activées ? autorisées pour ce dépôt ?) et sur
`Settings → Billing → Plans and usage` (minutes Actions épuisées, limite de
dépenses à 0). Ne pas retenter de nouveau correctif automatique sur ce point
tant que cette vérification n'a pas eu lieu — le prochain passage de la
routine peut se contenter d'un nouveau `workflow_dispatch` ponctuel pour
sonder si la situation a changé, sans reproduire l'investigation complète.

## 2026-08-26 — Epic « Processus qualité » (TDR §G)

### 25. Gap analysis phrase par phrase avant tout code : trois manques réels, pas un chantier vide

Le squelette `.drawio` marquait encore la section G « à construire », mais
une revue informelle antérieure (pendant la dérivation de l'epic H) avait
suggéré qu'elle était en grande partie déjà couverte. Plutôt que de trancher
sur ce doute, la section a été relue phrase par phrase et confrontée au code
existant :

- **Déjà fait** : le tableau de bord qualité par cours (`GET /pedagogie/qualite`,
  `PedagogieQualiteComponent`) existait déjà — avis étudiants agrégés par
  cours, cours à surveiller signalés en tête.
- **Manque 1 (S3/B2, TDR)** : « recueillir des retours sur l'application des
  compétences » et « mesurer l'impact à long terme de la formation sur la
  carrière des diplômés » — rien n'existait pour ça, ni modèle ni écran.
  `maNote`/`retour_evaluation` mesurent la satisfaction envers un cours, pas
  l'impact carrière après le diplôme.
- **Manque 2 (B4)** : « notifications automatiques pour rappeler aux
  étudiants de remplir leurs évaluations » — aucun rappel n'existait, ni
  manuel ni automatique.
- **Manque 3 (B5)** : « rapport qualité global consolidé » exportable —
  l'écran existait, mais rien à imprimer ou à archiver hors de l'application.

Ces trois manques ont défini le périmètre exact de l'epic : modèle
`EnqueteSuivi`/`ReponseEnqueteSuivi` (migration V18), dépôt de réponse côté
alumni avec upsert `(enquête, alumni)` (même patron que `retour_evaluation`,
V7), synthèse et lancement/clôture de campagne côté pédagogie, rappel
planifié (`EvaluationRappelService`, première tâche `@Scheduled` de ce
backend — précédent documenté dans `CLAUDE.md` §3), et rapport PDF
consolidant les deux écrans qualité sans rien recalculer
(`RapportQualitePdfService`, PDFBox, même patron que `CertificatPdfService`).

### 26. Gap non traité : S2, verrouillage de fin de session

Le TDR (S2) mentionne aussi le verrouillage d'une évaluation à la fin d'une
session de cours — hors périmètre de cette story, pour la même raison que le
rappel « à la fin de chaque session » (voir doc de `EvaluationRappelService`) :
`Cours` n'a aucune notion de session/édition datée. Une évaluation reste donc
modifiable indéfiniment aujourd'hui (upsert par `(étudiant, cours)`, sans
date de clôture). Introduire une session datée est une décision de modèle à
part entière, qui toucherait aussi le rappel automatique et l'affichage des
cours — à instruire séparément, pas une extension mécanique de cette story.

## 2026-08-26 — Epic « Processus de recrutement » (TDR §A)

### 27. Gap analysis complet du TDR pour choisir l'epic suivant, et écarts trouvés dans §A

Après la fusion de la PR #48 (epic « Processus qualité »), une analyse de gap a relu le
TDR paragraphe par paragraphe (pas seulement les titres de section) pour situer, dans
l'ordre réel du document, une liste de sept chantiers dictés le 2026-08-25 (parcours
d'admission détaillé, création de comptes par la pédagogie, forum/roster par promotion,
message groupé, accréditation à plusieurs évaluateurs, écrans de paramétrage par rôle,
profil alumni enrichi) plus deux sections du TDR jamais touchées (I « Suivi du personnel
INPG », J « Gestion des rendez-vous », confirmé par recherche exhaustive dans le
backend — aucune entité, contrôleur ni route frontend pour l'un ou l'autre).

**Résultat** : la section A (« Processus de recrutement »), déjà largement construite
lors de sessions précédentes, contient encore le chantier le plus en amont dans l'ordre
du document parmi tous les écarts identifiés — les sept chantiers dictés se répartissent
en réalité sur plusieurs sections (B, C, F), toutes postérieures à A. Deux manques réels
dans A, tous deux à propos du suivi du parcours candidat après soumission :

- `ApplicationController.sendReminder()` ne faisait que compter les dossiers incomplets
  ("Simulation d'envoi", aucun email ni notification réellement expédié) — endpoint
  d'ailleurs jamais appelé par le frontend (wrapper Angular présent, jamais invoqué par
  un composant).
- Aucune relance de paiement n'existait. `PaiementService.getAllPaiements()` ne liste que
  les `Paiement` déjà créés : un candidat n'ayant jamais initié de paiement (aucune ligne)
  restait invisible de tout suivi, alors que c'est justement le cas le plus fréquent d'un
  dossier impayé.

**Corrigés** : relance réelle des dossiers incomplets (email + notification, nommant les
pièces manquantes/non conformes — même patron que
`AdminRecrutementController.modifierConformitePiece`), et nouvelle relance des paiements
impayés (`PaiementService.relancerImpayes()`, recalculée côté serveur à partir du statut
du dossier et non d'une liste transmise par le client) — les deux branchées sur l'écran
du responsable admission (`responsable-admission-candidatures` et
`responsable-admission-paiements`).

**Décisions de périmètre, pour ne pas dupliquer ce qui existe déjà** :
- L'export CSV de `ApplicationController.exportApplications()` reste **non branché** : un
  export Excel complet et déjà fonctionnel existe (`AdminRecrutementController
  .exporterCandidatures`, appelé par `responsable-admission-candidatures.component`) —
  le brancher aurait créé un second export parallèle du même besoin, contraire au principe
  « réutiliser plutôt qu'inventer une modélisation parallèle ». L'endpoint CSV reste en
  place, inutilisé, sans risque : il n'écrit rien.
- « Nombre de tests configurable (par défaut 2) », dicté le 2026-08-25, n'a aucun ancrage
  dans le texte du TDR (seul un concept singulier de « test académique » y figure) et le
  modèle actuel (`TestAcademiqueService.creerTest`) permet déjà de créer autant de tests
  qu'on veut par candidat, sans plafond artificiel — le besoin semble déjà satisfait par
  construction. Non implémenté : ajouter un champ non exploité aurait été une extension
  spéculative, pas une correction d'un écart réel.
- Le rôle propriétaire du paiement (dicté comme potentiellement « pédagogie ») est confirmé
  par le TDR lui-même comme relevant de l'admission : les paragraphes sur le paiement
  vivent dans la section A (« Processus de recrutement »), pas dans une section pédagogie.
  `SecurityConfig` (`/api/admin/recrutement/**` → `RESPONSABLE_ADMISSION`) n'a pas été
  changé.
- Unifier les deux calculs de KPI candidatures existants (un côté serveur dans
  `ApplicationController.getKpis`, jamais appelé ; un côté client dans
  `responsable-admission.component.ts`, à partir de la liste brute) n'a pas été fait : le
  tableau de bord actuel fonctionne et est déjà exercé par les écrans ; le refaire sans
  besoin fonctionnel nouveau aurait été un risque de régression pour un gain cosmétique
  seulement (un seul point de calcul plutôt que deux qui, aujourd'hui, s'accordent).

### 28. Faille de configuration trouvée au passage : `/api/applications/**` sans règle d'URL

`SecurityConfig` protège son voisin direct `/api/admin/recrutement/**` par une règle
d'URL (`hasRole("RESPONSABLE_ADMISSION")`), en plus de l'annotation de méthode sur
chaque endpoint — la double protection déjà documentée pour `PhotothequeController`
("le filtrage d'URL seul est trop implicite"). `/api/applications/**`
(`ApplicationController`) n'avait **aucune** règle d'URL : chaque méthode porte bien
`@PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")`, mais rien ne la secourrait en cas
d'oubli sur un futur endpoint, et la requête tombait sur le filet générique
`.anyRequest().authenticated()` — accessible à n'importe quel rôle connecté si
l'annotation de méthode venait à manquer. Le risque réel était resté faible tant que
`sendReminder()` était un stub sans effet ; il cesse de l'être maintenant qu'il envoie de
vrais emails. Corrigé en ajoutant `/api/applications/**` → `RESPONSABLE_ADMISSION` dans
`SecurityConfig`, au même endroit que `/api/admin/recrutement/**`.

### 29. Pas de nouvelle entité : aucun diagramme UML mis à jour

Cette story n'ajoute aucune entité ni migration — seulement deux valeurs
d'`EnumTypeNotification`, deux méthodes de service, et deux endpoints réutilisant le
modèle `Candidature`/`Paiement`/`NotificationTemplate` déjà en place. Rien à documenter
dans `docs/uml/` : la charte du dépôt n'y consigne que les classes nouvelles ou
significativement modifiées, pas les ajouts de comportement sur un modèle inchangé.

## 2026-08-26 — Correctif signalé par l'utilisateur : confirmation de paiement impossible côté responsable admission

### 30. Le frontend appelait un endpoint backend qui n'a jamais existé

Signalement direct : « le resp admin peut pas confirmer les paiements sur le UX ».
Investigation : `RecrutementService.enregistrerPaiementHorsLigne()` (frontend) postait
déjà sur `POST /admin/recrutement/candidatures/{id}/paiement/enregistrer` — mais aucune
méthode `@PostMapping` ne répondait sur ce chemin dans `AdminRecrutementController`,
et **rien n'appelait cette méthode du service** depuis un composant. Un 404 silencieux
si jamais quelqu'un l'avait branchée. Le seul chemin réellement câblé côté serveur,
`PaiementService.confirmerPaiement()` (via `PUT /paiements/{id}/simuler-succes`),
ne peut agir que sur un `Paiement` **déjà créé** — donc rien pour les candidats n'ayant
jamais initié de tentative en ligne, le cas le plus fréquent d'un règlement en espèces
au guichet.

**Corrigé** : implémentation de `PaiementService.enregistrerPaiementHorsLigne()` — crée
le `Paiement` s'il n'en existe encore aucun, ou solde directement une tentative
`EN_ATTENTE`/`ECHEC` existante, sans passer par `Paiement.effectuerPaiement()` (qui
n'autorise que `EN_ATTENTE → SUCCES` : ici c'est un constat humain, pas la confirmation
d'une tentative en ligne). Endpoint ajouté au chemin déjà attendu par le frontend.
Bouton « Confirmer le paiement » ajouté sur `responsable-admission-paiements.component`,
sur chaque dossier de la table « Dossiers validés en attente de paiement » — couvre les
trois cas (jamais initié, en attente, en échec) avec une seule action.

Rien de similaire n'a été retrouvé ailleurs par grep (`enregistrerPaiementHorsLigne`
était le seul appel de service frontend sans méthode de contrôleur correspondante),
mais aucun audit systématique de toutes les paires service-frontend / endpoint-backend
n'a été fait à cette occasion — un futur passage pourrait le faire si un doute
similaire se présente ailleurs.

**Correctif concurrent découvert au moment d'ouvrir la PR** : pendant cette
investigation, un commit humain direct sur `develop` (`8e862b7`, `63ea0be`) a
câblé indépendamment la partie « valider/rejeter une tentative `EN_ATTENTE` »
de ce même signalement (boutons « Valider »/« Rejeter », méthodes
`valider()`/`rejeter()`, endpoints `simuler-succes`/`echec` déjà fonctionnels)
— avec une protection utile non anticipée ici : l'email de confirmation au
candidat, envoyé dans une méthode `@Transactional`, faisait échouer **et
annuler** la confirmation elle-même en cas de panne SMTP (Brevo injoignable
depuis Railway, constatée en production). Les deux travaux touchaient le même
fichier en parallèle ; fusion de `develop` dans la branche de ce correctif,
conflit résolu en gardant le câblage humain déjà en production pour
`EN_ATTENTE` (`idEnCours`, `valider`/`rejeter`) et en n'ajoutant que la pièce
manquante (`enregistrerPaiementHorsLigne`, bouton « Confirmer le paiement »
sur les dossiers sans aucune tentative). La protection SMTP a été reportée
dans `notifierCandidatPaiementConfirme()` (méthode partagée extraite pendant
ce correctif), pour couvrir aussi le nouveau chemin hors ligne.

## 2026-08-26 — Epic : comptes pédagogie (mot de passe généré) + forum/roster de promotion

Cadrage avant tout code : les deux points traités ici viennent d'une liste dictée
par l'utilisateur le 2026-08-25 (voir le rapport du même jour dans
`docs/daily_logs/2026-08-25.md`), pas d'une lecture littérale d'une phrase du TDR.
Un passage d'analyse d'écart dédié (méthode renforcée de l'ÉTAPE 3) a confirmé
qu'il s'agit d'un vrai manque du code, pas d'un doublon d'une fonctionnalité déjà
livrée — présenté ici honnêtement comme un souhait utilisateur adjacent au TDR §B,
et non comme un défaut du TDR lui-même.

### 31. Aucun compte (étudiant ou formateur) ne pouvait recevoir un mot de passe généré

`EtudiantService.createEtudiant` et `FormateurService.createFormateur` exigeaient
tous deux un `password` non vide, sans jamais en générer un — `send-otp`/
`verify-otp` (`OtpService`) est un mécanisme de vérification d'email pour
l'inscription et la réinitialisation, entièrement distinct, à ne pas réutiliser ici
(il produit un code à 6 chiffres en mémoire, pas un mot de passe). Pire : l'import
CSV en masse (`PedagogieScolariteController.importerEtudiants`) attribuait à
**tous** les étudiants importés le même mot de passe codé en dur
(`"INPG@" + annee`), partagé entre toute une promotion importée le même jour —
un étudiant connaissant ce format pouvait se connecter au compte d'un camarade.

**Corrigé** : `PasswordGeneratorService` (12 caractères, quatre classes de
caractères mélangées, `SecureRandom`). Les deux services de création génèrent
désormais un mot de passe si celui reçu est vide, et le réécrivent dans le DTO —
c'est ce qui permet à l'appelant (qui connaît l'email) de le transmettre ensuite.
La saisie manuelle existante n'est pas touchée : un mot de passe fourni n'est
jamais écrasé ni envoyé par courriel (seul un mot de passe généré l'est). L'import
CSV route désormais chaque ligne par ce même chemin, un mot de passe par étudiant.

### 32. Création de formateur totalement injoignable depuis la pédagogie

`POST /api/auth/formateurs` existe mais est réservé à `RESPONSABLE_ADMISSION`
(décision documentée dans le javadoc du contrôleur, point 18 de ce journal) — et
confirmé par `grep` **inutilisé par aucun écran frontend**, ni pour l'admission ni
pour la pédagogie. Un responsable pédagogique n'avait donc aucun moyen d'ouvrir un
compte formateur, alors que c'est lui qui les affecte ensuite aux cours.

**Corrigé** : `POST /api/pedagogie/formateurs` (nouveau, dans `PedagogieController`,
sous la règle `RESPONSABLE_PEDAGOGIQUE` déjà en place), qui réutilise
`FormateurService.createFormateur` sans dupliquer la logique d'unicité déjà
présente. Écran de création ajouté (`pedagogie-formateurs`), sur le même patron que
la page Étudiants existante. Le compte créé n'apparaît toujours dans **aucun**
espace formateur tant qu'il n'est pas rattaché à un cours (`cours_formateur`,
CLAUDE.md §3) — non automatisé ici (rattacher un formateur neuf à un cours est une
décision pédagogique, pas mécanique), mais explicitement rappelé par le message de
succès de l'écran de création.

### 33. Un mot de passe généré n'était jamais communiqué à son titulaire

Une fois généré, encore fallait-il le transmettre. `NotificationService.envoyerEmail`
(déjà utilisé côté recrutement) est réutilisé tel quel, sans étendre le système de
templates `NotificationTemplate`/`EnumTypeNotification` — ce système répond à « quel
modèle activable pour quel événement de candidature », une question différente
d'un envoi ponctuel d'identifiants. Le mot de passe en clair n'apparaît **que**
dans le corps de l'email : jamais dans la réponse HTTP de création (vérifié sur les
trois endpoints : `EtudiantSuiviDto`, la réponse `{id, message}` de la création
formateur, et le compte-rendu d'import CSV), ni dans les journaux applicatifs (le
`catch` autour de l'envoi ne logue que `exception.getMessage()`). Une panne SMTP
n'annule jamais la création du compte : elle est seulement journalisée, le compte
reste utilisable — c'est la même protection que celle apportée au point 30
ci-dessus pour la confirmation de paiement.

### 34. Forum et roster scopés à la promotion : absents, jamais construits sur `cours_promotion`

Le forum ne connaissait que deux granularités : par cours (`ForumSujet.cours`) et
par portée globale (`ForumSujet.portee`, ETUDIANT/ALUMNI, migration V13). Aucune
notion de promotion. Migration `V19` ajoute une FK `promotion_id` **additive**, à
côté de `cours_id` et `portee` — jamais en remplacement, et les deux rattachements
restent mutuellement exclusifs sur un même sujet (appliqué côté serveur dans
`ForumController#ouvrirSujet`, qui refuse un `coursId` et `pourMaPromotion` en même
temps). Choix déterminant : la promotion utilisée est **toujours** celle de
l'auteur, lue côté serveur depuis `Etudiant.getPromotion()` — jamais un identifiant
envoyé par le client (le DTO ne porte d'ailleurs qu'un booléen `pourMaPromotion`,
pas un `promotionId`), pour ne pas reproduire le précédent `cours_promotion`
(CLAUDE.md §3, deux chaînes concurrentes ayant fini par diverger).

`ForumVisibiliteService` (déjà le point de passage unique après le correctif du
point 14 de ce journal) a été étendu plutôt que contourné : la résolution reste
`cours != null` (inchangé) → sinon `promotion != null` (nouveau : membre de la
promotion) → sinon `portee` décide (inchangé). Le même correctif s'applique aux
deux contrôleurs qui servent `ForumSujet` (`ForumController` et
`EspaceEtudiantController`), reproduisant le principe déjà posé au point 14 : ne
jamais laisser un second contrôleur relire `ForumSujet` sans passer par ce service.

Suppression d'une promotion : `forum_sujets.promotion_id` n'a **aucun**
`ON DELETE CASCADE` (choix identique à `cours_id`) — sans nettoyage, supprimer une
promotion vidée de ses étudiants mais ayant encore un fil de forum aurait échoué
sur une violation de clé étrangère brute, une erreur SQL non traduite en message
utilisateur. `PedagogieScolariteController#supprimerPromotion` détache désormais
messages, mentions puis sujets avant de supprimer la promotion — même geste que
`PedagogieController#supprimerCours` fait déjà pour un cours.

### 35. Roster de promotion : nouvel endpoint, minimisation des données appliquée

`GET /api/espace-etudiant/scolarite/ma-promotion/etudiants`, adossé à
`EtudiantRepository.findByPromotionId` déjà existant (utilisé par la délivrance de
relevés en masse). Ne renvoie que nom et matricule (`CamaradeDto`) — jamais l'email
ni le téléphone, même précédent que la vérification de certificat (point 4 de ce
journal). Aucun paramètre d'entrée : la promotion vient uniquement de
`Etudiant.getPromotion()` de l'appelant, comme `GET /ma-promotion` déjà en place —
il n'existe donc aucune façon de demander le roster d'une autre promotion que la
sienne.

### 36. Écarts relevés mais non traités dans ce passage

- Le rattachement d'un formateur nouvellement créé à un ou plusieurs cours n'est
  pas chaîné dans l'écran de création (`PUT /cours/{id}/formateurs` reste une
  étape séparée, sur l'écran Cours). Un enchaînement guidé (« créer puis affecter
  en une seule visite ») améliorerait l'ergonomie mais élargirait la story
  au-delà de ce qui a été demandé.
- Le badge `SujetDto.cours` renvoyé par `EspaceEtudiantController` (vue
  formateur/personnel) affiche désormais « Promotion <libellé> » au lieu de
  « Général » pour un sujet scopé promotion, mais le champ `module` reste vide
  dans ce cas (il ne porte que le code d'un cours) — cohérent avec le comportement
  déjà en place pour un sujet général, non traité comme une lacune propre à ce
  correctif.
- Aucun diagramme n'existait déjà pour `ForumSujet` dans `docs/uml/` ; la
  documentation de la nouvelle FK `promotion_id` a été ajoutée directement dans
  le javadoc de l'entité plutôt que dans un nouveau diagramme, ce module n'ayant
  pas jusqu'ici de représentation UML dédiée à créer par cette seule story.

## 2026-08-26 — Demande directe de l'utilisateur : fiche détaillée et suppression étudiant/formateur

### 37. Suppression d'un compte : trois cas refusés plutôt que traités par cascade

L'utilisateur a demandé que le responsable pédagogique voie toutes les informations
sur les étudiants et formateurs (fiche cliquable) et puisse supprimer un compte
étudiant ou formateur. Aucune des tables qui référencent `etudiants(id_user)` ou
`formateurs(id_user)` n'a de suppression en cascade côté base (`V1__baseline.sql` ne
déclare pas `ON DELETE CASCADE` sur ces FK, à l'exception de `notification`/
`forum_mention` sur `users(id)`, ajoutées par la migration V10) : une suppression
naïve échouerait avec une violation de clé étrangère brute, exactement le piège déjà
rencontré deux fois sur ce module (suppression de cours, de promotion — CLAUDE.md §3).

`EtudiantSuppressionService`/`FormateurSuppressionService` (nouveaux, package
`espace_etudiant.service`, suivant le patron déjà établi par `EtudiantImportService`
d'un service `espace_etudiant` dépendant librement des entités/repositories `auth`)
vident donc chaque table dépendante avant de retirer la ligne satellite puis la ligne
`users`. Mais trois situations sont volontairement **refusées** (409, message clair)
plutôt que cascadées jusqu'au bout, parce que la suppression y déborderait du mandat
de cet écran ou effacerait un contenu dont d'autres personnes dépendent :

- **Un dossier de candidature existe encore** (`candidats.id_user`) : son cycle de
  vie appartient à l'espace admission (« l'admission ouvre l'accès, la pédagogie
  inscrit », CLAUDE.md §3) — cascader jusqu'aux candidatures, paiements, pièces
  justificatives, tests académiques et entretiens aurait fait disparaître un
  historique qui n'appartient pas à cet écran.
- **Le compte est passé côté alumni** (`alumni_profils.id_user`) : sa fiche, son
  historique professionnel et ses réponses aux enquêtes de suivi relèvent de
  l'espace alumni, pas du répertoire de scolarité.
- **La personne a publié dans le forum** (`forum_sujets.auteur_id` /
  `forum_messages.auteur_id`) : supprimer ses messages les effacerait aussi pour
  les autres participants du fil, qui n'ont rien demandé — à la différence des
  notes, présences ou certificats, qui n'appartiennent qu'à cette seule personne.

En pratique, un compte tout juste créé à la main (le cas le plus fréquent d'un
nettoyage de test ou d'une erreur de saisie) n'a aucun de ces trois obstacles et se
supprime sans friction ; un compte avec un vrai parcours (admission, promotion
alumni, participation au forum) est protégé plutôt que silencieusement vidé de tout
ce qui n'est pas strictement pédagogique.

Refus propre au formateur, en plus des trois ci-dessus : s'il est l'unique titulaire
d'au moins un cours, la suppression est refusée — `cours_formateur` commande tout le
cloisonnement enseignant (CLAUDE.md §3), et un cours sans formateur n'apparaît plus
dans aucun espace enseignant sans qu'aucune alerte ne le signale. S'il partage un
cours avec d'autres formateurs, il en est simplement détaché.

### 38. Fiches détaillées : DTO dédiés, jamais une extension des DTO existants

`EtudiantSuiviDto` (répertoire pédagogie) est le même DTO que consomme l'écran de
suivi du formateur, et `FormateurBrefDto` (`GET /pedagogie/formateurs`) alimente le
sélecteur d'équipe pédagogique d'un cours — son commentaire précise déjà
explicitement « le téléphone porté par l'entité n'a pas à circuler pour composer une
équipe pédagogique ». Y ajouter email/téléphone aurait fait fuiter ces coordonnées
vers des écrans qui n'en ont pas besoin. Deux nouveaux DTO (`EtudiantDetailDto`,
`FormateurDetailDto`) et deux nouveaux endpoints (`GET /pedagogie/etudiants/{id}`,
`GET /pedagogie/formateurs/{id}`) portent ces informations, réservés à la fiche
ouverte au clic sur une ligne du répertoire.

## 2026-08-26 — Reprise de l'epic « accréditation multi-évaluateurs » (TDR §F)

### 39. Décision collégiale à plusieurs évaluateurs : consolidation minimale, pas de vote

Le TDR §F demande que « le processus d'accréditation reste cohérent quand plusieurs
évaluateurs interviennent sur une même demande », sans préciser de règle de
consolidation (majorité, unanimité, veto…). Plutôt qu'inventer une règle de vote non
demandée, choix du patron déjà standard pour ce type de processus d'évaluation
collégiale (relecture par les pairs d'un article, comité d'admission) : chaque
évaluateur dépose un avis indépendant (`FAVORABLE`/`DEFAVORABLE`, upsertable — un
second dépôt du même évaluateur met à jour le premier, jamais n'en crée un second,
même contrainte d'unicité `(demande, evaluateur)` que `retour_evaluation`
`(étudiant, cours)`, V7), mais la décision finale (`accrediter`/`rejeter`) reste un
acte explicite d'un seul évaluateur — désormais **subordonné** à l'existence d'au
moins un avis déposé (`avisRepository.findByDemandeIdOrderByDateAvisAsc(id)` non
vide, sinon 400).

Délibérément **pas** de majorité automatique ni de blocage tant que tous les
évaluateurs assignés n'ont pas répondu : rien dans le modèle actuel ne définit
« l'ensemble des évaluateurs assignés » à une demande (n'importe quel compte
`ROLE_EVALUATEUR_ACCREDITATION` peut prendre en charge n'importe quelle demande,
patron déjà en place avant cette story) — une règle de quorum aurait donc dû
inventer aussi cette notion d'assignation, hors du périmètre demandé. Si un besoin
réel de consolidation automatique (majorité, quorum, veto d'un rôle senior) se
confirme, c'est une décision de modèle à instruire à part, qui suppose d'abord de
définir qui est « les » évaluateurs d'une demande donnée.

### 40. DTO évaluateur séparé du DTO partagé avec l'école : avis jamais divulgués avant décision

`EcoleAccreditationController` et `EvaluateurAccreditationController` construisaient
jusqu'ici le même DTO partagé (`DemandeAccreditationDto`). Y ajouter la liste des
avis aurait fait fuiter les délibérations individuelles des évaluateurs vers l'école
candidate, avant même la décision finale — un évaluateur pourrait ainsi voir son
nom et son avis défavorable exposés à l'établissement qu'il évalue, avant que
quiconque n'ait tranché. Nouveau DTO dédié `DemandeAccreditationEvaluateurDto`
(mêmes champs que le DTO partagé, plus `List<AvisAccreditationDto> avis`), retourné
uniquement par `EvaluateurAccreditationController` ; le DTO partagé et l'écran école
restent strictement inchangés. Même principe côté frontend : `DemandeAccreditation`
(modèle partagé) n'est pas modifié, un nouveau type `DemandeAccreditationEvaluateur`
l'étend avec `avis`, utilisé uniquement par `EvaluateurAccreditationService`.
