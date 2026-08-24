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

### 6. Story choisie dans Epic 4 : messagerie apprenant-formateur (pas les réunions virtuelles)

La section E du TDR regroupe deux besoins sous « outil de communication » : une messagerie
apprenant-formateur, et des réunions virtuelles. Les réunions virtuelles supposent une intégration
avec un service tiers de visioconférence (Zoom, Jitsi, Meet…) — décision de fournisseur hors de
portée d'une session autonome, et rien dans le dépôt ne l'amorce. La messagerie, elle, peut
réutiliser des patrons déjà éprouvés dans ce dépôt (garde-fous de `MentionForumService`, cloche
`notification` adossée à l'utilisateur, cloisonnement `PerimetreFormateurService` /
`cours_formateur`) sans dépendance externe. Choisie comme la plus petite et la plus indépendante
des deux, conformément à la consigne de préférer une story incrémentale bien vérifiée.
