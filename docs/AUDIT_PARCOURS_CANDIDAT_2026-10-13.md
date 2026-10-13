# Audit du parcours complet du candidat étudiant — 2026-10-13

**Question posée** : « un étudiant qui candidate, après la soumission on lui redemande de fournir les documents alors
qu'il l'a déjà fait » ; vérifier le processus complet et s'assurer que toutes les soumissions sont enregistrées.

**Méthode** : lecture du code de bout en bout (formulaire → serveur → suivi → fiche d'admission), puis essais en
exécution réelle sur les conteneurs (Playwright, comptes jetables), puis correctifs, puis mêmes essais après correctif.

## 1. Le parcours, étape par étape

| # | Étape | Où | Ce qui est enregistré |
|---|---|---|---|
| 1 | Profil, formation & parcours, documents (3 écrans) | `formulaire.component` | brouillon local + brouillon serveur (PUT différé d'1 s) |
| 2 | Dépôt du dossier | `POST /api/recrutement/candidat/candidature/soumettre` | candidat, candidature `SOUMIS`, réponses aux critères, **parcours, adresse, certification** |
| 3 | Envoi des pièces, une requête par fichier | `POST …/candidature/{id}/pieces` | `fichiers_stockes` + `pieces_justificatives` |
| 4 | Suivi | `suivi.component` | lecture seule + ajout de pièce |
| 5 | Examen par l'admission | fiche `candidature-detail` | lecture + décisions |

Le dépôt (2) et l'envoi des pièces (3) sont **deux appels séparés** : un dossier peut exister sans l'une de ses
pièces. C'est le point de fragilité central ; tout le reste en découle.

## 2. Constats et correctifs

### Ce qui explique le symptôme rapporté

1. **Garde d'ajout de pièce inversée** (22/09 → 09/10). Le serveur refusait une pièce pour un dossier tout juste
   soumis ou « à compléter » — l'inverse de la règle. Les pièces partaient donc toutes en 409, le formulaire
   l'annonçait par une alerte brève puis redirigeait, et le suivi réclamait les documents. **Corrigé par `9e28fa0`,
   présent uniquement sur `develop` — ni `main` ni `preprod` ne l'ont : à déployer.**
2. **Un échec d'envoi était invisible.** L'alerte disparaissait avant d'être lue, le formulaire redirigeait aussitôt
   (200 ms), et le suivi redemandait les documents sans explication. Aucune reprise en cas de coupure.
3. **Le suivi ne connaissait que trois types de documents** (liste en dur) : le diplôme, pourtant exigé et réclamé
   par la relance par e-mail, n'y figurait pas. La frise affirmait en dur « Tous les documents requis téléchargés ».
   La liste des pièces exigées existait en **trois copies divergentes** (formulaire, suivi, relance).
4. **Un diplôme ajouté depuis le suivi partait sans description** : le serveur le refusait, sans champ pour la saisir.

### Données saisies mais jamais enregistrées (la question « toutes les soumissions sont-elles enregistrées ? »)

5. **Parcours académique et situation professionnelle** : envoyés par le formulaire, jamais copiés dans l'entité
   (les colonnes existaient, la réponse les renvoyait — toujours vides). Perdus à chaque dépôt.
6. **Adresse postale** : exigée par le formulaire, jetée en silence (`@JsonIgnoreProperties(ignoreUnknown)` sur le DTO,
   aucune colonne).
7. **Certification sur l'honneur** : case obligatoire, jamais transmise ni datée.

### Défauts de robustesse trouvés en exécution réelle

8. **Brouillon recréé après le dépôt** : la dernière sauvegarde différée partait 0,6 s après le dépôt et recréait,
   côté serveur, le brouillon que le dépôt venait de supprimer (constaté en base sur deux comptes).
9. **Pièces en double** si un envoi réussissait mais que sa réponse se perdait, puis était relancé.
10. **Double clic** sur « Soumettre » : deux dépôts possibles.
11. **Téléphone déjà porté par un autre compte** : violation d'unicité remontée en fin de transaction, message
    générique, après écritures partielles.
12. **Redirection d'ouverture** du formulaire testait `'REJETE'`, statut qui n'existe plus : un dossier refusé
    renvoyait vers le suivi au lieu de permettre un nouveau dépôt, contrairement à la règle du serveur.
13. **Ordre des candidatures non déterministe** : le suivi lit la dernière entrée comme « dossier actuel » sans tri.
14. Fiche d'admission : ni téléphone, ni adresse, ni parcours, ni certification ; types de documents affichés en
    clair technique (`PIECE_IDENTITE`), sans nom de fichier ni description.
15. Textes : « votre lettre de motivation » (plus collectée), en-tête « Félicitations… examen en cours » affiché pour
    un dossier auquel il manque des pièces, bouton de soumission vert avec relief (hors charte).

### Correctifs

| Constat | Correctif |
|---|---|
| 5, 6, 7 | migration `V202610131100` (`candidats.adresse`, `candidatures.consentement_le`) ; `CandidatController` enregistre parcours (JSON validé, ≤ 20 000 car., mis à jour à la re-soumission, jamais effacé par une requête sans parcours), adresse (≤ 500) et date de certification |
| 3 | `CompletudeDossier` : **définition unique** des pièces exigées ; le serveur calcule `documentsAttendus` (`FOURNI` / `A_FOURNIR` / `A_REMPLACER`) que lisent le suivi, la fiche d'admission et la relance |
| 2 | `envoi-pieces.ts` : 2 envois en parallèle, 3 essais avec attente croissante sur les pannes passagères (réseau, 408/425/429/5xx), aucun sur les refus définitifs ; panneau persistant (raison par pièce, « Renvoyer », « Aller à mon suivi ») ; **contrôle final auprès du serveur** avant de dire « tout est transmis » |
| 9 | le serveur reconnaît une pièce déjà reçue (même type, nom, taille, description, moyenne, **mêmes octets**) et répond `doublon` ; un fichier disparu du disque n'est pas un doublon |
| 8 | brouillon purgé dès le dépôt, `saveDraft()` neutralisé côté client ; le serveur ignore un brouillon quand un dossier est déposé |
| 10 | garde `loading \|\| candidature` ; le formulaire disparaît une fois le dossier déposé |
| 11 | contrôle `existsByTelephoneAndIdNot` **avant toute écriture**, 409 avec message explicite |
| 12 | `depotPossibleApres()` : brouillon, `DOSSIER_REJETE`, `NON_ADMIS` (règle du serveur) |
| 13 | tri par identifiant dans `getCandidaturesByCandidatId` |
| 4, 14, 15 | description du diplôme dans le suivi ; fiche d'admission enrichie ; textes et bouton corrigés |

## 3. Preuves

- **Tests backend** : `CandidatControllerSoumissionTest` (25), `CandidatControllerTeleverserPieceTest` (14),
  `CompletudeDossierTest` (9), `ApplicationControllerTest` (13). Contrôle de mutation : en neutralisant
  successivement l'enregistrement du parcours, la détection de doublon et la garde de brouillon, les tests visés
  échouent (2, 3 et 3 échecs).
  Suite backend complète (hors `LmsApplicationTests`) : 554 tests, 0 échec.
- **Tests frontend** (à froid, `NG_BUILD_CACHE=0`) : `envoi-pieces`, `formulaire-depot` (dont double clic et brouillon,
  vérifiés par mutation : 2 échecs sans les gardes), `suivi`, `parcours-candidature`, `parcours-declare`,
  `candidature-detail`. Suite complète à froid : 245 tests, 0 échec ; `ngc --noEmit` propre.
- **Exécution réelle** (Playwright sur les conteneurs, comptes `zz.audit.*`) :

| Scénario | Résultat |
|---|---|
| A. Parcours nominal | 4 pièces reçues (200), parcours / adresse nettoyée / certification datée enregistrés, 0 brouillon résiduel, suivi sans « À fournir » |
| B. Réponse perdue deux fois sur le CV (le serveur l'a pourtant reçu) | arrivée au suivi ; **1 seul CV en base** |
| C. Panne durable sur le CV, puis « Renvoyer » | panneau persistant avec la raison ; 1 seul dépôt de dossier ; après renvoi, 4 pièces |
| D. Panne durable, « Aller à mon suivi », ajout depuis la ligne du CV | le suivi réclame le CV et lui seul, frise « 1 document à fournir » puis « Tous les documents… » |
| E. Téléphone déjà pris | message clair, formulaire conservé, aucun dossier ni pièce créés |
| F. Fiche d'admission | téléphone, adresse, certification, parcours, libellés, fichiers et description visibles |

## 4. Ce qui reste à décider ou à surveiller

- **Déployer `9e28fa0` sur `main`/`preprod`** : tant que ce n'est pas fait, les environnements concernés ont la garde inversée.
- ~~**L'admission peut présélectionner un dossier incomplet**~~ — **tranché et corrigé le même jour** : un dossier incomplet ne se
  présélectionne plus ; il se met en attente du candidat (« demander un complément ») et revient seul à l'examen. Voir
  [`AUDIT_ADMISSION_STATUTS_ET_COMPLETUDE_2026-10-13.md`](AUDIT_ADMISSION_STATUTS_ET_COMPLETUDE_2026-10-13.md), qui trace aussi les recherches.
- ~~**Statuts hérités côté admission**~~ — **audités et corrigés** dans le même document (neuf écrans, une définition unique décidée par le serveur).
- **Fichier orphelin possible** : `FichierStockeService` écrit le fichier avant la pièce ; une erreur entre les deux
  laisse un fichier sans pièce. Le script d'audit (section 5) les liste ; aucune purge automatique.
- **Pas de limitation de débit** sur l'envoi de pièces ni sur le dépôt.
- Les dossiers déposés **avant** le correctif n'ont ni parcours, ni adresse, ni certification : non récupérables
  (jamais transmis au serveur). Le script les compte.

## 5. Contrôler une base : `backend/db/audit-candidatures.sql`

Lecture seule, 13 requêtes (dossiers sans pièce, pièces manquantes par type, pièces fantômes, fichiers orphelins,
doublons, brouillons survivants, téléphones partagés…). Sur une base saine, chacune rend zéro ligne.

```bash
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/audit-candidatures.sql
```

Sur la base de recette, le script a relevé, avant nettoyage, les défauts attendus des dossiers de test **et** deux
anomalies plus anciennes sans rapport avec ce correctif : le dossier de démonstration `INPG-TEST0002` n'a aucune pièce,
et un fichier stocké (`cv.pdf`, 2026-09-08) n'est référencé par aucune pièce.
