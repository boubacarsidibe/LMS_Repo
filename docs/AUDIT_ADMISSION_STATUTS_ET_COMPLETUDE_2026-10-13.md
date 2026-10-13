# Admission : dossier incomplet et statuts périmés — 2026-10-13

Suite de [`AUDIT_PARCOURS_CANDIDAT_2026-10-13.md`](AUDIT_PARCOURS_CANDIDAT_2026-10-13.md) (côté candidat) et de
[`AUDIT_RESPONSABLE_ADMISSION_2026-10-12.md`](AUDIT_RESPONSABLE_ADMISSION_2026-10-12.md) (constats C1 et C2, résolus ici).
Deux questions : que faire d'un dossier **incomplet** que l'admission peut présélectionner, et que valent les écrans de
l'admission qui parlent encore l'ancien vocabulaire de statuts.

## 1. Décision métier : un dossier incomplet ne se présélectionne pas, il se met en attente du candidat

### Ce que faisait le code
- `changerStatut` ne refusait que le dossier **totalement vide** : un dossier sans diplôme, ou dont une pièce était refusée, se présélectionnait.
- Le filtre automatique de critères présélectionnait **ou rejetait** un dossier partiellement fourni, alors que le candidat pouvait encore le compléter.
- Le statut `INCOMPLET` existait (transitions `SOUMIS → INCOMPLET → SOUMIS`) mais **aucune route ne le posait** et rien ne ramenait le dossier à l'examen : le texte du suivi (« votre dossier sera de nouveau examiné ») promettait un mécanisme absent.
- Une pièce refusée puis remplacée restait « à remplacer » **pour toujours** (la pièce refusée reste en base comme preuve) : défaut introduit par la première version de `CompletudeDossier`, trouvé en préparant ce chantier.

### Ce que font les établissements (recherches)
Pas de règle unique, mais une pratique dominante : **on ne statue pas sur un dossier incomplet ; on le retient, on dit exactement ce qui manque, on donne un délai.**

- Arizona State (Barrett) : les dossiers sont évalués « selon l'arrivée des pièces requises, pas la date de dépôt ». [Source](https://asu.secure.force.com/kb/articles/FAQ/What-constitutes-a-complete-Barrett-application)
- Southern Illinois University Edwardsville : « votre dossier sera examiné une fois tous les documents reçus ». [Source](https://www.siue.edu/newsletters/undergraduate-admissions/july/undergraduate-incomplete-app.html)
- Indiana University (3ᵉ cycle) : le dossier n'est pas transmis aux départements avant que toutes les pièces soient réunies. [Source](https://bulletins.iu.edu/iu/educ-grad/2010-2012/admissions/admission-process/admission-decisions.shtml)
- Campus France Sénégal : un dossier incomplet est **renvoyé avec le détail des corrections**, un seul passage en correction. [Source](https://www.senegal.campusfrance.org/system/files/medias/documents/2023-09/Calendrier-De-Proce%CC%81dures-2024-8_0.pdf)
- Universités françaises : tantôt « dossier incomplet non examiné » ([Cergy](https://gci.u-cergy.fr/pdf/candidature/Inscription-MasterR.pdf)), tantôt l'université réclame les pièces manquantes après coup ([Lille](https://ged.univ-lille.fr/nuxeo/nxfile/default/8b6a5af4-48d9-40b6-aa70-1738c27a9463/blobholder:0/guide_candidature.pdf)).
- Université d'Amsterdam : un e-mail dit **quels documents manquent**, avec **10 jours ouvrés** avant clôture du dossier. [Source](https://www.uva.nl/en/programmes/bachelors/business-analytics/application-and-admission/international-prior-education/wait-for-the-admission-results.html)
- Admission conditionnelle (Seattle U, Boğaziçi) : réservée aux pièces **hors de la volonté du candidat** (relevé qu'une autre institution doit envoyer), avec date butoir. [Seattle U](https://archive.seattleu.edu/media/redhawk-service-center/registrar/registrar-policies/Admissions-Records-Incomplete-Documentation-76-07.pdf)

Le cahier des charges du projet va dans le même sens : *« dossier incomplet peut revenir plus tard pour le finaliser »* (TDR 3.1.A) et, pour la
checklist candidat, *« le dossier ne peut être soumis que lorsqu'il est cohérent »* (`AUDIT_RECRUTEMENT_TDR_2026-09-13.md`).

### Décision (alternatives écartées)
| Option | Verdict |
|---|---|
| Laisser présélectionner un dossier incomplet | ✗ le jury examine ce qu'il ne peut pas lire en entier ; contraire à toutes les pratiques relevées |
| Avertir seulement | ✗ un avertissement se contourne ; le dossier incomplet finit en test, puis en délibération |
| Rejeter automatiquement | ✗ le candidat peut encore compléter ; perte de candidats valables, et la raison n'est pas une décision de jury |
| **Bloquer la décision favorable, mettre en attente, relancer, reprendre seul** | ✓ retenu |

### Règles écrites
1. **Présélection et validation (`PRESELECTIONNE`, `DOSSIER_VALIDE`) refusées** tant qu'un document exigé manque ou est à remplacer : 422 qui nomme les pièces. Le **rejet** reste possible à tout moment (candidat inéligible, dossier abandonné) : seule la décision *favorable* est gardée.
2. **« Demander un complément »** (`SOUMIS → INCOMPLET`) : e-mail + cloche qui listent précisément les pièces, entrée au journal QMS-009. Refusé pour un dossier complet (une pièce illisible se traite pièce par pièce, « demander le remplacement »).
3. **Retour automatique** `INCOMPLET → SOUMIS` dès que le dernier document est reçu (journalisé « Complément reçu »).
4. **Le filtre automatique ne juge que des dossiers complets** ; les autres sont comptés à part (`dossiersIncomplets`) et restent `SOUMIS`.
5. **Les dossiers « à compléter » sont les premiers relancés** (`reminder-incomplets`).
6. **Une pièce refusée est « remplacée »** quand une pièce du même type, non refusée, a été déposée **après** le refus ; chaque remplaçant ne couvre qu'un refus (trois relevés refusés réclament trois dépôts).

Non retenu pour l'instant : **un délai butoir** (Amsterdam : 10 jours ouvrés, clôture ensuite). Il demande une date par session et une tâche planifiée ; la plateforme n'a que la date de fermeture de session. À décider avec l'admission.

## 2. Audit des statuts périmés côté admission (constats C1 et C2 du 12/10)

### Mesure
`EnumStatutCandidature` compte **20** statuts depuis la refonte du 22/09. Neuf fichiers de l'espace admission en raisonnaient encore sur **cinq** anciens
(`EN_COURS_DE_PRESELECTION`, `ADMIS1`, `ADMIS2`, `ADMIS3`, `REJETE`), chacun avec sa copie des libellés, couleurs, boutons, règles de rejet et de recul :
liste, fiche, vue d'ensemble, statistiques, délibération, paiements, entretiens, test, aide. (`REJETE` subsiste légitimement dans l'**accréditation**, autre domaine.)

| Écran | Défaut constaté avant correction |
|---|---|
| Liste | onglets vides, statuts en nom technique, **aucun bouton après la présélection**, menu « Reculer vers » inutilisable (4 statuts disparus) |
| Fiche | idem ; compteur « dossiers incomplets » faux (comptait toute pièce non encore contrôlée) |
| Délibération | **vide** : cherchait `ADMIS2` ; un candidat n'y apparaissait jamais après son entretien |
| Vue d'ensemble, statistiques | entonnoir et taux d'admission **à 0 %** |
| Paiements, entretiens, tests | listes de candidats éligibles **vides** (`ADMIS1/ADMIS2`) |
| Export Excel | graphique à 6 libellés pour 17 valeurs (chaque barre mal nommée) ; colonne « Statut » en nom technique |
| Aide | décrivait le parcours avec les anciens statuts |
| **C1** | aucune route ne posait `TEST_REUSSI/ECHOUE`, `ABSENT_TEST`, `ENTRETIEN_EFFECTUE`, `ABSENT_ENTRETIEN` : **un dossier convoqué ne pouvait ni avancer ni être refusé** |
| Modèles d'e-mails | la contrainte `notification_templates_type_check` n'autorisait que 11 des 14 types : `LISTE_ATTENTE`, `RAPPEL_DOSSIER_INCOMPLET`, `RAPPEL_PAIEMENT` ne pouvaient pas être enregistrés |

### Correction : une définition, décidée par le serveur
- **Serveur** : `CandidatureService#actionsPossibles` dit, dossier par dossier, ce que l'admission peut faire (`PRESELECTIONNER`, `DEMANDER_COMPLEMENT`, `ENREGISTRER_REGLEMENT`, `CONVOQUER_TEST`, `ENREGISTRER_RESULTAT_TEST`, `ADMETTRE_ENTRETIEN`, `ENREGISTRER_RESULTAT_ENTRETIEN`, `ADMETTRE_FINAL`, `PLACER_LISTE_ATTENTE`, `FINALISER_INSCRIPTION`, `REJETER`) et `reculsPossibles` les étapes de retour. Un test garantit que **toute action proposée correspond à une transition acceptée** (c'est l'écart entre ces deux listes qui laissait des dossiers sans bouton).
- **Frontend** : `recrutement/statuts-admission.ts` — libellés, tons de couleur, **groupes** (onglets : à examiner, à compléter, présélection, test, entretien, liste complémentaire, admis, écartés), indicateurs (admis, refusé, décidé, engagé dans la sélection). Un statut inconnu s'affiche tel quel au lieu de casser l'écran.
- **C1 résolu** : routes `resultat-test` (`REUSSI`/`ECHOUE`/`ABSENT`) et `resultat-entretien` (`EFFECTUE`/`ABSENT`). C'est un **geste du responsable**, pas un effet de la saisie des notes : la moyenne aide, le jury tranche. Un absent se reconvoque ; un échoué se rejette (`NON_ADMIS`).
- Migration `V202610131500` : contrainte des modèles d'e-mails complète, + type `DEMANDE_COMPLEMENT`.
- Export Excel : un onglet = plusieurs statuts (`statut=SOUMIS,INCOMPLET`) ; un statut inconnu donne 422 au lieu de 500.

## 3. Vérification

- **Backend** : 581 tests, 0 échec (suite complète hors `LmsApplicationTests`) — dont : garde de complétude (dossier sans diplôme, pièce refusée, pièce remplacée), `demanderComplement`, résultats de test/entretien, `actionsPossibles` pour chaque statut, cohérence actions/transitions, règle du recul, remplacement des pièces refusées (6 cas), retour automatique à l'examen.
- **Frontend** : 285 tests à froid, 0 échec ; `ngc --noEmit` propre.
- **Exécution réelle** sur les conteneurs (3 candidats jetables, API + navigateur) — **41 contrôles sur 41** : présélection d'un dossier incomplet refusée avec les documents nommés ; complément demandé, journalisé ; complément reçu → retour à l'examen ; parcours complet présélection → règlement → test (clic « Réussi » dans la fiche) → entretien → délibération (le candidat y figure, « En attente ») → admis ; échec au test → rejet → non admis ; absent → reconvoqué ; liste sans nom technique ; taux d'admission non nul ; export multi-statuts ; quatre modèles d'e-mails enregistrables.

## 4. Reste à décider ou à surveiller
- **Délai butoir** d'un complément (voir §1).
- **Résultats de test/entretien : automatiser ?** Aujourd'hui un geste du responsable. Une règle « moyenne ≥ seuil de la session → réussi » est possible (`seuilReussiteTest` existe) ; elle suppose de décider quoi faire si les notes changent après coup.
- `rejeterDossier` côté serveur garde une liste « après évaluation » qui inclut des statuts d'où le rejet est impossible (`CONVOQUE_*`) : inoffensif (le serveur ne le propose plus), à nettoyer.
- Le candidat en `DESISTEMENT` ne peut pas redéposer (le serveur le verrouille) alors que le suivi lui dit qu'il le pourra « lors d'une prochaine session » : à trancher.
