# Repères de travail — LMS INPG

Ce fichier est lu automatiquement au début de chaque session. Il consigne les règles
et les pièges découverts en cours de route, pour qu'ils n'aient pas à être
redécouverts.

---

## 1. Charte graphique

Tout le style passe par les jetons de `frontend/tailwind.config.js`. Les valeurs
codées en dur de l'espace candidat (`#30609a`, `#46464e`, `#c7c5d0`…) **sont** celles
des jetons : elles ne créent pas d'écart visuel, seulement une entorse au système.
Ne pas les recopier ailleurs.

### Le piège central : famille ≠ taille

`font-<échelle>` ne définit que la **famille** (Inter). La taille, l'interlignage et
la graisse viennent de `text-<échelle>`, une classe **distincte**.

```html
<!-- FAUX : le texte retombe en 16 px / graisse 400 -->
<h1 class="font-display text-on-surface">Titre</h1>

<!-- JUSTE -->
<h1 class="font-display text-display text-on-surface">Titre</h1>
```

252 endroits souffraient de cet oubli, dont **tous les titres de page**, qui
s'affichaient en 16 px au lieu de 48. Vérifier systématiquement que les deux classes
sont présentes.

### Règles à appliquer

| Élément | Classes |
|---|---|
| Titre de page (`<h1>`) | `font-display text-display text-on-surface leading-none -ml-1` |
| Sur-titre | `font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block` |
| Sous-titre | `font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl` |
| Intertitre (`<h2>`) | `font-headline-sm text-headline-sm text-primary flex items-center gap-2` + pictogramme en `text-secondary` |
| Carte | `bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm` |
| Bouton plein | `bg-secondary text-on-secondary rounded-lg font-label-md text-label-md` — **sans ombre ni `active:scale`** |
| Conteneur de page | `p-margin-desktop space-y-stack-lg max-w-container-max mx-auto` (1280 px, centré) |

**L'ombre des cartes n'est pas décorative** : sans elle, les cartes blanches se
confondent avec le fond clair et l'écran paraît plat.

**Les boutons sont plats.** L'espace étudiant leur ajoutait `shadow-md`,
`hover:shadow-xl` et `active:scale-95` — retirés pour suivre l'espace candidat.

### Logotype

Identique dans tous les espaces :

```html
<span class="text-2xl font-extrabold tracking-tight text-primary">INPG<span class="text-brand"> LMS</span></span>
```

Pas de badge ni d'icône. Le jeton `brand` (`#131741`) est le bleu nuit du suffixe.
Dans l'espace pédagogique, le suffixe est « Pédagogie » — même traitement.

### Contraste : le piège des jetons jumeaux

`primary-fixed` et `primary-container` valent **la même couleur** (`#d4e3ff`). Poser
l'un comme texte sur l'autre comme fond donne un contraste de 1:1 — du texte
littéralement invisible. C'était le cas de la date des séances, des initiales
d'avatar et de l'icône du logo.

> Texte sur `bg-primary-container` → **`text-on-primary-container`** (`#001b3d`).
> Jamais `text-primary-fixed`, jamais `text-on-primary`.

L'avatar de profil est plein : `bg-primary`, initiales `text-white`, `shadow-sm`.

### Limite connue

La règle `.material-symbols-outlined` de `frontend/src/styles.css` est déclarée
**hors de toute couche CSS**. Les styles hors couche l'emportent toujours sur ceux
d'une couche, et Tailwind range ses utilitaires dans `utilities` : **les 216 classes
de taille posées sur des icônes sont donc inertes**, toutes rendues en 24 px.

La corriger demande d'entourer la règle d'un `@layer base` — ce qui changerait la
taille de 216 icônes d'un coup. À traiter comme un chantier à part, pas comme une
retouche.

---

## 2. Pièges de l'environnement

### Le cache de build fige certains modules

Quand une modification du frontend semble ne pas s'appliquer, le suspect n'est pas le
navigateur mais **`frontend/.angular/cache`**. Le conteneur monte le dossier local
(`./frontend:/app`), ce cache est donc partagé, et il peut servir des modules périmés
**pour certains fichiers seulement** — symptôme observé : la barre latérale se mettait
à jour, les pages chargées à la demande non.

```bash
docker compose stop frontend
rm -rf frontend/.angular/cache frontend/dist
docker compose up -d frontend        # ~90 s de reconstruction à froid
```

`docker compose up -d --build` **ne suffit pas** : le cache vit dans le montage, pas
dans l'image. Avant d'accuser le cache, vérifier que le code est correct :

```bash
grep -rl "<un marqueur de la modification>" frontend/dist/frontend/browser/*.js
```

### Fins de ligne CRLF

Beaucoup de fichiers sont en CRLF sur le disque. Un remplacement multi-lignes écrit
avec `\n` échoue **en silence**. Toujours détecter :

```python
raw = p.read_bytes().decode("utf-8")
NL = "\r\n" if "\r\n" in raw else "\n"
```

Et se méfier de `git checkout`, qui les réécrit en LF via `.gitattributes`.

Ne jamais passer par `sed -i` ni `Path.write_text()` sur ces fichiers : ils
normalisent les fins de ligne et transforment un correctif de 20 lignes en diff de
2000.

### `mvn test` touche la base de recette

`LmsApplicationTests` démarre Spring contre `localhost:5432`, donc **Flyway applique
les migrations** pendant les tests, avant tout déploiement. En tenir compte avant de
lancer la suite sur une base qui sert à une démonstration.

---

## 3. Décisions d'architecture à ne pas défaire

**Un cours se rattache à une formation, jamais à une promotion.** La table
`cours_promotion` a été supprimée (migration `V6`). Deux chaînes concurrentes
répondaient à « cet étudiant suit-il ce cours ? » et avaient divergé au point de ne
désigner aucun cours commun. La dérivation cours → formation → promotions vit dans
`PromotionService` ; `PerimetreFormateurService` y délègue.

**Un seul système d'avis : `retour_evaluation`** (migration `V7`). `evaluations_cours`
et `POST /retours` ont été supprimés : l'avis d'un étudiant n'atteignait jamais son
formateur. Unicité `(étudiant, cours)` — redéposer met à jour.

**`maNote` est l'appréciation donnée AU cours par l'étudiant**, pas sa note
académique. Les notes sont servies par `GET /scolarite/mes-notes`.

**Les deux photothèques ne sont pas des doublons** : `Phototheque` permet au formateur
de publier, `Photheque` est un visualiseur étudiant.

**Ce module traverse beaucoup d'associations paresseuses** (`open-in-view=false`).
`EspaceEtudiantGardeFousTest` échoue si un endpoint est ajouté sans `@Transactional`
ou sans `@PreAuthorize` — ne pas contourner ce test, il protège d'une famille de bugs
survenue trois fois.

**`cours_formateur` commande tout le cloisonnement enseignant.** `PerimetreFormateurService`
y lit « ce cours est-il le mien ? ». Un cours absent de cette table n'apparaît dans
**aucun** espace formateur : ni support, ni séance, ni note n'y sont possibles. Rien
ne le signale côté base — `ddl-auto=validate` ne compare jamais deux relations entre
elles. Six cours sur neuf étaient dans ce cas. L'affectation se fait par
`PUT /api/pedagogie/cours/{id}/formateurs`, qui refuse une liste vide.

**La chaîne d'accès a trois maillons, et chacun casse en silence :**

```
promotion → formation → cours → formateur
```

Un seul maillon rompu suffit à vider un écran, sans erreur ni journal. Les trois
sont désormais signalés en rouge dans l'espace pédagogique — c'est la raison d'être
de ces alertes, ne pas les retirer comme du bruit visuel. Piège rencontré : la
promotion `MPG-2026` pointait vers la formation « Rôles spécifiques » (0 cours)
tandis que les 9 cours étaient rattachés à « Master Pétrole & Gaz ». Les deux
maillons existaient, ils ne se rejoignaient pas.

**Sur `CoursCreateRequest`, `null` et `[]` ne veulent pas dire la même chose.** Champ
absent = ne pas toucher au programme ; liste vide = retirer le cours de tous les
programmes. Les confondre donne deux bugs muets et symétriques : soit modifier le
titre d'un cours le détache de tout, soit on ne peut plus jamais le détacher. Ni
erreur, ni journal — seul un écran vide le révèle plus tard.
`PedagogieCoursProgrammeTest` fige les trois cas.

**Une mention au forum suit l'accès, elle ne l'ouvre pas** (migration `V10`). Les
personnes citables sont celles qui participent déjà au fil — étudiants du cours,
formateurs du cours — jamais l'annuaire, qui révélerait les noms de toutes les
promotions dans une autocomplétion. La liste envoyée par le navigateur **ne fait pas
foi** : `MentionForumService` la recoupe côté serveur, sans quoi un identifiant glissé
à la main notifierait n'importe qui en lui montrant le titre d'un fil interdit.
`MentionForumServiceTest` fige ce recoupement.

**Les citations sont stockées, pas redéduites du texte** (`forum_mention`). Un nom
s'écrit de plusieurs façons, change, ou apparaît par hasard dans une phrase : relire le
message donnerait des résultats différents selon le moment.

**La cloche `notification` est adossée à l'utilisateur**, pas au candidat.
`notifications_candidat` reste en place pour l'espace recrutement, qui a son propre
parcours — ne pas les fusionner sans traiter les deux parcours. Le courriel n'a pas été
retenu pour les mentions : trop fréquentes, elles feraient couper la notification.

**L'admission ouvre l'accès, la pédagogie inscrit.** `AdminRecrutementController.
inscrireCommeEtudiant` bascule `ROLE_CANDIDAT` en `ROLE_ETUDIANT` et crée la fiche
étudiant — **sans promotion ni matricule**. Ne pas supprimer cette conversion au motif
que la scolarité appartient au responsable pédagogique : sans elle, un candidat admis
resterait sans accès et sans que personne ne sache qu'il attend. C'est l'affectation
d'une promotion, faite côté pédagogie, qui achève l'inscription et attribue le
matricule. Le compte porte `origine = "Admission"` tant qu'un dossier de candidature
existe derrière lui, pour distinguer un admis de la veille d'un compte oublié.

**Un relevé délivré fige ses valeurs, il ne les recalcule pas** (migration `V9`).
`releve_ligne` recopie code, titre, coefficient et moyenne du cours ; elle n'a
volontairement **aucune clé étrangère** vers `cours`. Sans cela, un document remis en
janvier se réécrirait en février parce qu'un formateur a corrigé une note — ce serait
un écran, pas un document. `ReleveDelivranceServiceTest` fige cette propriété ; ne pas
« optimiser » ces colonnes en jointures.

Deux notions à ne jamais confondre à l'écran comme au serveur :
`GET /scolarite/mes-notes` renvoie le relevé **courant**, recalculé ;
`GET /scolarite/mes-releves` renvoie les relevés **délivrés**, figés. Qu'ils diffèrent
est normal, et c'est l'intérêt.

**Le coefficient d'un cours ne se déduit de rien.** Ni du volume horaire, ni du
nombre d'évaluations : c'est une valeur de la maquette pédagogique, saisie à la main
(colonne `cours.coefficient`, migration `V8`, défaut 1). La moyenne générale portait
avant sur la liste à plat de toutes les notes — un cours évalué trois fois pesait donc
trois fois plus qu'un cours évalué une fois, pondération que personne n'avait choisie.
Elle se calcule désormais sur les moyennes **par cours**, chacune pesant son
coefficient. `ScolariteReleveNotesTest` fige les deux cas et le cas par défaut.

**Deux contrôleurs répondent sur `/api/pedagogie`** : `PedagogieController` (offre
d'enseignement) et `PedagogieScolariteController` (scolarité). Ne jamais déclarer la
même route dans les deux — Spring refuse de démarrer. `GET /promotions` appartient au
second, qui porte le `PromotionDetailDto` complet ; la version réduite du premier
faisait afficher « Aucun programme » partout et **effaçait le rattachement** à la
première édition.

---

## 4. Comptes et données

Mot de passe commun aux comptes de recette : `Test@2026`.

| Rôle | Email |
|---|---|
| Candidat (sans dossier) | `test.candidat@inpg.sn` |
| Candidat (avec dossier) | `test.candidat2@inpg.sn` |
| Étudiant | `test.etudiant@inpg.sn` |
| Formateur *(sans cours — cobaye de cloisonnement)* | `test.formateur@inpg.sn` |
| Responsable admission | `test.admission@inpg.sn` |
| Responsable pédagogique | `test.pedago@inpg.sn` |

Pour une démonstration, préférer **`prof.demo@inpg.sn` / `Demo@2026`** : trois cours,
six supports, seize notes. Les étudiants de démonstration utilisent `Etudiant@2026`.

Scripts hors Flyway, relançables sans risque (`backend/db/`) :

```bash
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-comptes-test.sql
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-client.sql
```

Le second remplit les écrans qui sonnent creux en démonstration : progressions
variées, photothèque, forum, tests et entretiens.

---

## 5. Méthode attendue

Reproduire un défaut avant de le déclarer, le corriger, puis le revérifier **en
exécution réelle** sur les conteneurs — pas seulement en lecture de code. Vérifier
qu'un test de non-régression échoue bien sur le code d'avant correction.

Restaurer les données après tout essai destructif, et cibler les `DELETE` par un
`WHERE` — la base sert aux démonstrations client.

Réponses en français, chaque terme technique expliqué brièvement.
