# Vidéos de présentation de la plateforme

Chaîne de production reproductible : **un scénario = un fichier** (`src/scenarios/*.mjs`) qui joue un vrai parcours dans la
vraie application (Chromium piloté par Playwright), commenté par une voix neuronale française et sous-titré. Sortie : MP4 1080p,
chapitres, SRT. Rien n'est simulé : chaque geste fait réellement avancer un dossier en base (que l'on restaure avant chaque prise).

## Utilisation

```bash
./preparer-donnees.sh                  # jeux SQL + fichiers + objets signés créés par l'API réelle + instantané de référence
./produire.sh 05-etudiant --essai --captures   # répétition rapide : une capture par étape, signale les erreurs de l'application
./produire.sh 05-etudiant              # prise complète → sortie/05-etudiant/05-etudiant.mp4 (+ .srt, chapitres)
```

Prérequis : la pile Docker tourne (`docker compose up -d`), le serveur de dev du frontend a fini de recompiler.
`restaurer.sh` remet la base dans l'état de l'instantané (appelé par `produire.sh`).

## Scénarios (`src/scenarios/`)

| Fichier | Public | Parcours |
|---|---|---|
| `01-candidat` | Candidat | catalogue → compte → dossier en 3 étapes → suivi → paiement → convocation → admission |
| `02-admission` | Responsable admission (1/2) | sessions, instruction pièce par pièce, présélection, rejet motivé, paiements, statistiques |
| `03-admission-suite` | Responsable admission (2/2) | tests notés, entretiens, délibération, inscription, courriels |
| `03-pedagogie` | Responsable pédagogique | offre, scolarité, relevés, certificats, qualité, entreprises, activités |
| `04-formateur` | Formateur | tableau de bord, ressources, suivi, planning, forum, retours, annonces |
| `05-etudiant` | Étudiant | cours, évaluation, calendrier, forum, résultats, certificat, rendez-vous |
| `06-alumni` | Alumni | parcours, relevés, enquêtes, forum, documents |
| `07-ecole` | École de formation | inscription, demande d'accréditation, pièces, attestation, liste publique |
| `08-evaluateur` | Évaluateur | référentiel, grille de 31 critères, avis, décision |
| `09-entreprise` | Entreprise | catalogue, demande, messagerie, rendez-vous |
| `10-candidat-formateur` | Candidat formateur | offres, compte, dossier, pièces, suivi |

(La numérotation sera harmonisée à la livraison : `00-presentation-generale`, puis 01…11.)

## Contenu du dossier

- `src/lib/` : acteur (gestes lents et lisibles), surcouche (curseur, onde, puces de chapitre, cartes), enregistreur (CDP screencast),
  narration (edge-tts, voix placée au centième de seconde), montage (ffmpeg : H.264, sous-titres incrustés, chapitres, loudnorm).
- `src/outils/` : exploration par rôle, inspection d'écran, captures, diagnostic de gel, contrôle audio par transcription (Whisper).
- `donnees/` : pièces PDF de démonstration (fictives, signalées comme telles) ; `src/outils/generer-documents.mjs` les régénère.
- `cache/` et `sortie/` ne sont pas versionnés (reconstructibles).

## Points d'attention

- Voix : synthèse neuronale (service edge-tts, non officiel) — la **relire à l'oreille** avant diffusion ; `src/outils/verifier-audio.py`
  compare la voix à son texte par transcription.
- Les données de démonstration sont fictives ; les entreprises et écoles citées n'existent pas.
- Voir `CLAUDE.md` §6 pour les pièges rencontrés.
