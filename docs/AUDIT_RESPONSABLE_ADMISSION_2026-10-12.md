# Audit des responsabilités — interfaces du responsable admission

Date : 2026-10-12 · Périmètre : tout l'espace `/responsable-admission` et les routes serveur qui le servent.
Méthode : lecture du code, puis **exécution réelle** sur les conteneurs (dossier de recette `INPG-TEST0002` passé
par chaque statut, appels directs aux routes, captures navigateur). Chaque constat marqué ✔ a été reproduit ;
les autres sont issus de la lecture du code seule.

## 1. Ce que ce rôle est censé porter

Le responsable admission conduit le **recrutement des candidats** : ouvrir des sessions et fixer leurs critères,
examiner les dossiers, organiser les tests et entretiens, décider de l'admission, informer les candidats, suivre
les règlements. Il est protégé côté serveur par `hasRole('RESPONSABLE_ADMISSION')` à deux niveaux (règle d'URL
`SecurityConfig` **et** annotation de méthode) — cloisonnement correct, aucun écran n'est accessible à un autre rôle.

## 2. Écran par écran

| Écran | Responsabilité réelle | État constaté |
|---|---|---|
| Vue d'ensemble | Compteurs et sessions actives | ⚠ « Taux d'admission » reste à 0 % même avec un dossier `ADMIS` ✔ |
| Sessions (+ critères) | Créer/modifier une session, ses critères de présélection, son seuil de test | ✔ Sain. Retiré aujourd'hui : frais de dossier et « prochaines étapes » (3 champs) |
| Candidatures (liste) | Examiner, présélectionner, admettre, rejeter, reculer, annuler | 🔴 Statuts affichés en nom technique, actions absentes après la présélection ✔ |
| Candidatures (détail) | Pièces, frise, historique, rejet, « reculer vers » | ⚠ Fonctionne ; une copie de la fenêtre de confirmation était collée dans la frise (corrigé) |
| Tests académiques | Créer des épreuves, convoquer (groupe), saisir les notes | 🔴 La saisie d'une note **ne change pas le statut du dossier** ✔ |
| Entretiens | Planifier, convoquer, noter | 🔴 Idem : aucun statut « entretien effectué » n'est jamais posé ✔ |
| Délibération | Décision finale : admettre, liste complémentaire, rejeter | 🔴 Le candidat n'apparaît plus après l'entretien ✔ |
| Paiements | Suivre et **enregistrer à la main** un règlement fait hors plateforme, relancer les impayés | ✔ Conservé volontairement (décision du 2026-10-12) |
| Notifications | **Éditeur des modèles d'e-mails** envoyés aux candidats | ⚠ Nom trompeur : ce n'est pas la cloche |
| Formateurs | Recrutement des formateurs (offres, candidatures — procédure QMS-007) | ⚠ Autre métier que l'admission d'étudiants (§4, I1) |
| Rendez-vous | Prise de rendez-vous avec le personnel (TDR §J) | ✔ Écran partagé, sans enjeu |
| Statistiques | Entonnoir et profil des candidats | 🔴 Entonnoir à 0 : lit les anciens statuts ✔ · accessible seulement par un lien de la Vue d'ensemble |
| Paramètres | Profil, mot de passe, préférences du tableau de bord | ✔ Sain (préférences stockées dans le navigateur) |
| Utilisateurs | Liste des comptes et **changement de rôle** | 🔴 Écran routé mais sans aucun lien ; la route serveur, elle, est active (§3, C3) |
| Aide | Mode d'emploi | ⚠ Décrit des statuts et boutons qui n'existent plus (corrigé pour le paiement seulement) |

## 3. Constats critiques

### C1 — Le parcours est bloqué après la convocation au test ✔
La table de transitions (`CandidatureService#estTransitionValide`) impose `CONVOQUE_TEST → TEST_REUSSI | TEST_ECHOUE |
ABSENT_TEST`, puis `CONVOQUE_ENTRETIEN → ENTRETIEN_EFFECTUE | ABSENT_ENTRETIEN`. **Aucune route ne pose jamais ces cinq
statuts** : ils sont lus (filtres, statistiques) mais jamais écrits. Conséquences mesurées :

| Depuis | Action | Résultat |
|---|---|---|
| `CONVOQUE_TEST` | Admettre à l'entretien | 422 « Transition non autorisée » |
| `CONVOQUE_TEST` | Rejeter | 422 « Transition non autorisée : → NON_ADMIS » |
| `CONVOQUE_ENTRETIEN` | Admettre (final) | 422 « Transition non autorisée : → ADMIS » |
| `CONVOQUE_ENTRETIEN` | Rejeter | 422 « Transition non autorisée : → NON_ADMIS » |

Même constat côté notes : saisir une note de 18 sur un dossier `CONVOQUE_TEST` répond 200 et **laisse le statut
inchangé** ✔. Un dossier convoqué ne peut donc **ni avancer ni être refusé**. Aucune erreur de code n'est en cause : le workflow du
22/09 a défini les étapes sans écrire ce qui les franchit. À décider : le statut se pose-t-il **automatiquement** à la
saisie de la note (seuil de la session) ou **par un geste** du responsable ?

### C2 — Les écrans parlent encore l'ancien vocabulaire de statuts ✔
~120 références à `ADMIS1/2/3`, `EN_COURS_DE_PRESELECTION`, `REJETE` dans 11 fichiers. Mesuré : statuts bruts dans la liste
(`PRESELECTIONNE`, `CONVOQUE_TEST`…), délibération vide, entonnoir à zéro, taux d'admission à 0 %. La page de suivi
**du candidat** avait le même défaut (corrigée le 2026-10-09). La correspondance partagée existe désormais
(`recrutement/parcours-candidature.ts`) : il reste à la brancher sur ces écrans.

### C3 — Le responsable admission peut changer le rôle de n'importe quel compte ✔
`PUT /api/admin/users/{id}/roles` accepte **tout** code de rôle existant, sans liste blanche, y compris
`ROLE_RESPONSABLE_ADMISSION`, `ROLE_RESPONSABLE_PEDAGOGIQUE`, `ROLE_EVALUATEUR_ACCREDITATION`. Aucune protection contre
l'auto-rétrogradation (un responsable peut s'enlever son propre rôle et verrouiller l'espace) ni contre la
rétrogradation du responsable pédagogique. `GET /api/admin/users` expose les 20 comptes de tous les rôles
(le mot de passe n'est pas exposé ✔, mais `sessionVersion`, champ interne, l'est).
Le rôle **admission** concentre donc l'administration des comptes, alors que son métier est le recrutement.
À décider : un rôle d'administrateur dédié, ou au minimum une liste blanche (candidat, étudiant, formateur) et
l'interdiction de se modifier soi-même.

## 4. Constats importants

- **I1 — Frontières de métier.** (a) `finaliser-inscription` crée la fiche étudiant : décision documentée
  (CLAUDE.md §3), avec la file « inscriptions à finaliser » comme garde-fou ; à garder, mais la frontière avec la
  pédagogie (promotion, matricule) doit rester explicite à l'écran. (b) Le **recrutement des formateurs** vit dans
  l'espace admission, alors que la pédagogie gère déjà les formateurs : une même population, deux espaces.
- **I2 — Plusieurs endroits pour décider.** Rejeter se fait depuis la liste (✕), le détail et la délibération ;
  admettre depuis la liste et la délibération. Seuls « annuler » et « reculer » exigent le mot de passe. Un même acte
  a donc plusieurs portes, avec des garde-fous différents.
- **I3 — Paiement : règle maintenue, mode d'emploi absent.** L'admission ne peut convoquer au test qu'après un
  règlement confirmé, mais le candidat n'a plus de moyen de payer en ligne ni aucune instruction sur le règlement hors
  plateforme. À décider : où et comment le candidat est-il informé ?
- **I4 — Éléments décoratifs ou orphelins.** Barre de recherche de l'en-tête sans aucun comportement ; composant
  `settings/` jamais routé (interrupteurs qui ne changent rien) ; page « Utilisateurs » sans lien.
- **I5 — Libellés trompeurs.** « Notifications » désigne un éditeur de modèles d'e-mails, pas la cloche d'alertes.

## 5. Ce qui est bien placé

Double barrière de rôle côté serveur ; confirmation par mot de passe pour annuler ou reculer un dossier ;
journal d'étapes QMS-009 horodaté et attribué ; refus de présélectionner un dossier sans pièce ; file d'attente
d'inscription ; la page de suivi du candidat et celle de l'admission partagent désormais la même frise.

## 6. Recommandations, par ordre

1. **Débloquer le parcours (C1)** : décider de la règle de passage test/entretien, puis l'écrire côté serveur. C'est
   le seul point qui empêche aujourd'hui un candidat d'aller au bout.
2. **Brancher la liste, le détail, la délibération, la vue d'ensemble et les statistiques sur la correspondance de
   statuts partagée (C2)**, avec une action « suivante » par statut du nouveau workflow.
3. **Encadrer le changement de rôle (C3)** : liste blanche, interdiction de se modifier soi-même, et trancher
   l'administrateur dédié.
4. Écrire le mode d'emploi du paiement hors plateforme côté candidat (I3).
5. Retirer ou brancher les éléments décoratifs (I4), renommer « Notifications » en « Modèles d'e-mails » (I5).
6. Trancher la place du recrutement des formateurs (I1b).

## 7. Modifications faites le 2026-10-12 en marge de cet audit

- Formulaire de session : champ « Frais de dossier » et bloc « Prochaines étapes » (frais de scolarité, carte
  étudiante, rentrée) retirés, ainsi que leur affichage côté admission et côté admis.
- Paiement en ligne retiré **côté candidat** (page de paiement, bouton « Payer », étape de frise). Conservé côté
  admission pour l'enregistrement d'un règlement hors plateforme (page Paiements, modèles d'e-mails, alerte, frise à
  5 étapes). Les routes serveur de paiement sont intactes.
- Textes d'e-mails « validation du dossier » et « rappel de paiement » : le règlement se fait désormais « auprès du
  service des admissions » et non plus « depuis votre espace candidat ».
