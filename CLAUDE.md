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
