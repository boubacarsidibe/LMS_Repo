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
