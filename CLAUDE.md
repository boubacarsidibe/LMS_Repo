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

**Poser `text-<échelle>` ne suffit pas si une troisième classe de taille traîne déjà
sur l'élément** (`text-[11px]`, `text-sm`…) — piège trouvé le 2026-09-18 en corrigeant
19 occurrences de ce même oubli. `text-<échelle>` et `text-[Npx]`/`text-sm` ciblent la
même propriété CSS avec la même spécificité (une seule classe) ; Tailwind les déclare
dans l'ordre où elles sont *rencontrées à la compilation*, pas dans l'ordre des classes
posées dans le HTML — et dans ce projet, les classes arbitraires (`text-[11px]`) et les
utilitaires par défaut (`text-sm`) sont émises **après** les échelles du thème dans
`styles.css`, donc elles l'emportent silencieusement. Un badge qui portait déjà
`text-[11px]` avant qu'on lui ajoute `font-label-sm text-label-sm` continuait de
s'afficher en 11 px : le correctif était posé mais sans le moindre effet visible tant
que `text-[11px]` restait sur l'élément. Vérifier en cas de doute dans le CSS compilé
(`grep`/byte offset des deux règles) — ou, plus simple, retirer la classe de taille
concurrente : c'est presque toujours elle l'ancien pis-aller que `text-<échelle>`
remplace.

**Titres de page (audit du 2026-10-08)** : tous les `<h1>` suivent la ligne du tableau ci-dessous, y
compris l'espace candidat et les pages publiques — 48 px / 700, `text-on-surface`, jamais `text-primary`,
jamais `text-2xl`/`text-3xl`. Un seul `<h1>` visible par page : les layouts qui portaient un
`<h1 class="sr-only">` (admission, pédagogie) en dupliquaient un, ils utilisent un `<span class="sr-only">`.
Exceptions voulues : le hero de l'accueil (échelle responsive propre) et le titre blanc du panneau de
connexion. Famille et taille vont ensemble : `font-<échelle>` sans `text-<échelle>`, ou deux échelles
différentes sur un élément, sont des erreurs — hors cas responsive (`text-2xl sm:text-headline-lg`).

**Aucune couleur ni taille de texte en dur** (audit du 2026-10-08, espace candidat migré) : jamais
`text-[#46464e]`, `border-[#c7c5d0]`, `text-slate-900`, `bg-white`… — le jeton existe. Équivalences :
`#30609a` = `primary`, `#46464e` = `on-surface-variant`, `#191c1e` (et le noir `#000002`, abandonné) =
`on-surface`, `#c7c5d0` = `outline-variant`, `#ba1a1a` = `error`, `bg-white` = `surface-container-lowest`.
Tailles : `text-sm` / `text-base` / `text-lg` ne s'écrivent plus que `font-body-sm text-body-sm` /
`-md` / `-lg` ; `text-xl font-semibold` = `headline-sm`, `text-2xl font-semibold` = `headline-md`.
Restent volontairement en dur : les couleurs de **statut** sans jeton (teintes vertes, jaunes, rouges
pâles des pastilles) et `text-xs`/`text-[Npx]` des icônes — à promouvoir en jetons plutôt que recopier.
**Méthode de vérification d'une refonte de classes** : photographier les styles calculés (`getComputedStyle`)
de chaque élément des pages concernées avant et après, puis comparer élément par élément — c'est ce qui a
montré que 800 remplacements ne changeaient aucun pixel, et trouvé le seul vrai écart (un
`font-semibold font-bold` contradictoire que le CSS arbitrait). Une connexion par compte et par série :
l'application verrouille 15 minutes après trop de tentatives.

**Cadres des espaces (audit du 2026-10-08)** : tous les espaces à menu latéral partagent la même
charpente — menu `w-64` (256 px), en-tête `h-16` en `bg-surface-container-lowest/80 backdrop-blur-md`,
élément actif `bg-primary-container text-on-primary-container! font-medium` (sans ombre ; le `!` est
nécessaire : la classe de base `text-on-surface-variant` l'emporterait sinon), contenu décalé de
`md:pl-64`. Chaque page pose elle-même son `p-margin-desktop` : ne jamais en remettre sur le `<main>` du
layout, le double décalait les titres de 40 px chez le formateur. Un sur-titre (`label-sm`) au-dessus du
`<h1>` ajoute 24 px à l'écart sous l'en-tête : c'est normal, pas un défaut.
**Rayons** : 4 px (cases à cocher), 8 px (`rounded-lg` : boutons, champs), 12 px (`rounded-xl` : cartes),
`full`. Ni `rounded-md`, ni `rounded-2xl`/`3xl`. **Carte** = `shadow-sm` toujours, y compris sur les pages
publiques (le survol d'une carte cliquable peut l'élever). **Champ de saisie** : jamais `border-none`.
**Bouton secondaire** : `bg-surface-container-lowest border border-outline-variant text-on-surface
rounded-lg hover:bg-surface-container-low`. **Pas de relief** sur un bouton : ni `border-t` clair, ni
filet `bg-white/10`, hérités de l'ancien bouton bleu moyen.
**Jetons de statut** (au lieu de recopier des hexadécimaux) : `success-container` / `on-success-container` /
`success-outline`, `warning-container` / `on-warning-container` / `warning-outline`, `on-brand-variant`
(texte atténué sur le bleu nuit du pied de page).
**Pièges rencontrés** : une couleur peut se cacher dans une liaison `[class.bg-primary]="…"` ou un
`[ngClass]`, que les recherches par `class="…"` ne voient pas — chercher aussi `[class.`. Et
`rounded` → `rounded-lg` ne doit **jamais** toucher une case à cocher (16 px de côté : elle deviendrait
ronde).

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

**Un seul bouton principal pour toute la plateforme : bleu nuit** (`bg-secondary`, `#1E3A5F`, décision du
2026-10-08), y compris sur l'accueil, la navbar publique et le parcours candidat, qui étaient en
`bg-primary` / `bg-[#30609a]` / `bg-blue-700`. `bg-primary` reste réservé aux avatars et aux accents (points,
barres de progression). Survol : `hover:opacity-90`, jamais une seconde teinte codée en dur (`#254d7d`).

**Les boutons sont plats.** L'espace étudiant leur ajoutait `shadow-md`,
`hover:shadow-xl` et `active:scale-95` — retirés pour suivre l'espace candidat.

### Logotype

Depuis le 2026-09-25, c'est **l'image du logo officiel de l'INPG** — plus l'écriture « INPG LMS », qui
n'existe plus nulle part — identique dans tous les espaces :

```html
<app-logo-inpg hauteur="h-10"></app-logo-inpg>
```

`LogoInpgComponent` (`shared/components/logo-inpg/`) est l'unique source de l'image
(`frontend/public/images/logo-inpg.png`, 640 × 174) et de son texte alternatif : ne pas recopier un `<img>`
ailleurs, ni recomposer un logotype en texte. Le fichier est détouré (fond transparent) et le logo est bleu
nuit : sur un fond sombre (panneau de connexion, pied de page), le poser dans une pastille blanche arrondie,
sans quoi il disparaît. **Exception voulue** : le layout de connexion/inscription (`auth-layout`) garde le
texte « INPG LMS » (`text-2xl font-extrabold text-primary`, suffixe en `text-brand`) en haut de sa partie
blanche, et le panneau sombre n'a plus de logo — choix esthétique du 2026-09-25. Le nom de l'espace (« Espace formateur », « Portail Admissions », « Espace
pédagogique ») reste écrit **sous** le logo, en `font-label-sm`. Le même logo coiffe les PDF (voir §3, papier
à en-tête).

### Contraste : le piège des jetons jumeaux

`primary-fixed` et `primary-container` valent **la même couleur** (`#d4e3ff`). Poser
l'un comme texte sur l'autre comme fond donne un contraste de 1:1 — du texte
littéralement invisible. C'était le cas de la date des séances, des initiales
d'avatar et de l'icône du logo.

> Texte sur `bg-primary-container` → **`text-on-primary-container`** (`#001b3d`).
> Jamais `text-primary-fixed`, jamais `text-on-primary`.

L'avatar de profil est plein : `bg-primary`, initiales `text-white`, `shadow-sm`.

**Vert et jaune : un jeton pour le fond, un autre pour le texte** (audit du 2026-10-08).
`text-success` (3,3:1 sur blanc) et `text-warning` (1,9:1) sont sous le seuil de 4,5:1 ; ils
restent valables pour des fonds et pastilles. Pour du **texte** : `text-success-strong` et
`text-warning-strong`. Même logique pour `outline`, assombri à `#6b6a73` (4,07:1 → 4,84:1 sur
`surface-container-low`). Un bandeau en texte blanc (`text-white`, `bg-white/10`) se pose sur
`bg-secondary`, jamais sur `bg-primary-container` — ce jeton est pâle, le texte y disparaît.

### Chantier des icônes — résolu le 2026-08-25, deux causes superposées

La règle `.material-symbols-outlined` de `frontend/src/styles.css` était déclarée
**hors de toute couche CSS**. Les styles hors couche l'emportent toujours sur ceux
d'une couche, et Tailwind range ses utilitaires dans `utilities` : les 216 classes
de taille posées sur des icônes étaient donc inertes, toutes rendues en 24 px.
Corrigé en entourant la règle d'un `@layer base`.

**Ce correctif seul ne suffisait pas.** `angular.json` active par défaut
l'optimisation `fonts.inline` en production : Angular re-télécharge la feuille
Google Fonts au moment du build et l'injecte en `<style>` littéral dans
`index.html`, **hors de toute couche** — une seconde copie de la même règle,
indépendante de `styles.css` et de son `@layer base`. Un `@import … layer(base)`
sur l'import Google ne change rien : ce n'est pas un `@import` classique qui
charge cette copie, c'est l'optimiseur de build qui la génère séparément.
Vérifié en inspectant `dist/frontend/browser/index.html` après build : la classe
y apparaissait avant même que Tailwind ne déclare ses couches.

**Décision** : `angular.json` → `architect.build.configurations.production.optimization`
règle désormais `"fonts": false` (scripts et styles restent optimisés). Les
polices se chargent alors via de vrais `<link rel="stylesheet">`, servis par
Google avec la réponse moderne (`@font-face` seul, sans classe de secours) — la
seule règle `.material-symbols-outlined` restante dans le bundle est la nôtre,
correctement dans `@layer base`. Vérifié par inspection du CSS compilé (une
seule occurrence de la règle, dans `@layer base`, avant la couche `utilities`) —
non vérifié par rendu visuel réel à l'époque (voir §2, un navigateur réel est en
fait disponible via Docker).

---

## 2. Pièges de l'environnement

### Un navigateur réel est disponible malgré les apparences

Aucun navigateur dans le shell de ce Codespace, mais **Playwright tourne très bien en
conteneur** et permet une vérification visuelle réelle plutôt qu'une simple lecture de
code :

```bash
docker run --rm --network host -v <dossier-scripts>:/scratchpad -w /scratchpad \
  mcr.microsoft.com/playwright:v1.48.0-jammy bash -c "node script.js"
```

`--network host` donne accès direct à `localhost:4200`/`:8080` — inutile de résoudre
`host.docker.internal` (il n'est pas résolvable avec ce réseau). Utilisé avec succès
pour capturer des captures d'écran et lire du `getComputedStyle` réel (par exemple
pour confirmer qu'un `text-<échelle>` l'emportait bien sur une classe de taille
concurrente après correctif — voir le piège de cascade en §1).

### Ce Codespace ne sert pas le frontend en mode dev — vérifier avant d'accuser le cache

Le paragraphe suivant décrit le montage `./frontend:/app` défini dans
`docker-compose.yml`. **Il ne s'applique pas forcément** : un `docker-compose.override.yml`
(présent dans ce Codespace, ignoré par git — donc à vérifier sur chaque nouvelle
machine plutôt que supposé absent) remplace le service `frontend` par une image de
production statique (`Dockerfile.local` : `ng build --configuration development` puis
Nginx, sans le moindre volume monté). Symptôme si on l'ignore : vider
`frontend/.angular/cache` et relancer `docker compose up -d frontend` ne change
strictement rien, parce que ce cache n'est même pas dans le conteneur. Vérifier lequel
des deux cas s'applique avant d'agir :

```bash
docker inspect demo-frontend --format '{{json .Mounts}}'   # [] → pas de montage, voir ci-dessous
```

Si `Mounts` est vide, la seule façon de pousser une modification est de reconstruire
l'image (même logique que pour le backend) :

```bash
docker compose up -d --build frontend        # ~10-30 s, PAS ~90 s : ce n'est pas le dev-server
```

### Le cache de build fige certains modules (setup dev-server avec volume monté)

Quand une modification du frontend semble ne pas s'appliquer **et que `docker-compose.override.yml`
ne détourne pas déjà le service vers une image statique** (voir ci-dessus), le suspect
n'est pas le navigateur mais **`frontend/.angular/cache`**. Le conteneur monte le
dossier local (`./frontend:/app`), ce cache est donc partagé, et il peut servir des
modules périmés **pour certains fichiers seulement** — symptôme observé : la barre
latérale se mettait à jour, les pages chargées à la demande non.

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

### Tests frontend : un test peut passer avec le cache et échouer à froid

`ng test` réutilise `frontend/.angular/cache`. Le 2026-10-09, trois tests de l'évaluateur échouaient à chaque
exécution à froid (et sur une intégration continue) mais passaient dès que le cache était chaud, ce qui les
a fait passer pour « flottants » alors qu'ils étaient reproductibles. Vérifier **à froid** avant de conclure :
`NG_BUILD_CACHE=0 npx ng test --watch=false`. Cause : un champ `x = CONSTANTE_IMPORTEE` arrivait `undefined`
quand deux fichiers de test partageaient un bloc ; un accesseur (`get x() { return CONSTANTE; }`) l'évite.
36 autres champs de ce motif existent (`libellesStatut = LIBELLES_…`) : à convertir si le défaut y réapparaît.

### `mvn test` touche la base de recette

`LmsApplicationTests` démarre Spring contre `localhost:5432`, donc **Flyway applique
les migrations** pendant les tests, avant tout déploiement. En tenir compte avant de
lancer la suite sur une base qui sert à une démonstration.

### Les e-mails partent, mais n'arrivent pas — SPF d'`inpg.sn` contre Brevo

Diagnostiqué le 2026-09-08 : le code d'envoi fonctionne (SMTP Brevo authentifié,
aucune exception), mais `inpg.sn` a de vrais MX Microsoft 365 et un SPF terminé par
`-all` qui **n'autorise pas Brevo**. Un message expédié via Brevo en se présentant
comme `@inpg.sn` est donc rejeté ou mis en quarantaine chez le destinataire, sans
que rien ne remonte côté serveur. Ne pas chercher le bug dans `NotificationService` :
il n'y en a pas.

Deux sorties, à trancher avant la production : autoriser Brevo dans le SPF/DKIM du
DNS d'`inpg.sn`, ou renoncer à Brevo et pointer `MAIL_HOST` vers le SMTP Microsoft
365 du domaine. En développement, `APP_MAIL_FROM` utilise une adresse d'un domaine
vérifié chez Brevo — contournement inacceptable en production, où les candidats
recevraient leurs convocations depuis une adresse personnelle.

### Numéroter les nouvelles migrations par horodatage, pas par entier

Les migrations `V1` à `V39` restent telles quelles — **ne jamais les renommer** : leur
somme de contrôle est enregistrée dans `flyway_schema_history`, et un fichier renommé
après application fait échouer la validation au démarrage.

Toute **nouvelle** migration prend en revanche la forme `V<AAAAMMJJHHMM>__sujet.sql`
(ex. `V202609081430__prix_par_session.sql`). Flyway compare les versions numériquement,
donc un horodatage passe toujours après `V39`.

La raison : deux branches (ou deux agents) qui travaillent en parallèle choisissent
fatalement le même entier suivant. C'est arrivé le 2026-09-08 — `V32`/`V33` ont été
renumérotées en `V38`/`V39` alors que la base les avait déjà appliquées sous leur
ancien numéro. Résultat : validation Flyway en échec, backend qui refuse de démarrer,
et réparation manuelle des lignes de `flyway_schema_history` (correction du champ
`version`, checksums inchangés car le contenu des fichiers, lui, n'avait pas bougé)
puis un démarrage ponctuel avec `spring.flyway.out-of-order=true` pour rattraper les
migrations restées en attente. Un horodatage rend la collision impossible.

### `ng build` tué (SIGTERM) et démon Docker qui tombe : c'est la mémoire du Codespace

Le Codespace n'a que ~8 Go, sans swap, et l'IDE en occupe plus de 3. Quand la mémoire disponible
tombe sous ~1,5 Go, un `ng build` (local **ou** dans `docker compose build frontend`) est tué en
`SIGTERM`, sans la moindre erreur de compilation — `ngc --noEmit` et `ng test` passent pourtant. Le démon
Docker peut lui aussi disparaître (`Cannot connect to the Docker daemon`). Ne pas chercher un bug de code.

```bash
docker stop demo-backend                       # libère ~500 Mo
sync; echo 3 | sudo tee /proc/sys/vm/drop_caches   # vide le cache disque : le garde-fou de l'hôte le compte
cd frontend && NODE_OPTIONS=--max-old-space-size=1536 NG_BUILD_MAX_WORKERS=1 \
  npx ng build --configuration development --source-map=false --progress=false   # ~28 s
docker cp dist/frontend/browser/. demo-frontend:/usr/share/nginx/html/   # sans reconstruire l'image
docker start demo-backend
sudo /usr/local/share/docker-init.sh true &    # si le démon Docker est tombé
```

`docker compose up -d --build backend` recrée le conteneur : toute variable passée en ligne de commande
(ex. `APP_CORS_ORIGINS` pour un tunnel Cloudflare, dont l'URL change à chaque lancement) est perdue et doit
être repassée. Les redémarrages du Codespace arrêtent Postgres sans arrêt propre : les écritures récentes
peuvent être perdues.

---

## 3. Décisions d'architecture à ne pas défaire

**Un cours se rattache à une formation, jamais à une promotion.** La table
`cours_promotion` a été supprimée (migration `V6`). Deux chaînes concurrentes
répondaient à « cet étudiant suit-il ce cours ? » et avaient divergé au point de ne
désigner aucun cours commun. La dérivation cours → formation → promotions vit dans
`PromotionService` ; `PerimetreFormateurService` y délègue.

**Les notes sont sur 100, pas sur 20** (migration `V44__notes_sur_100.sql`) : le statut de suivi bascule à
60 (= 12/20), l'interface affiche `note / 5` pour le /20. Un test écrit avec des valeurs « sur 20 »
échoue donc sans que le code soit en cause (`MoyenneUniqueTest` l'était).

**Un dossier de candidature n'est « encore modifiable » que tant que l'admission ne s'est pas prononcée** :
`STATUTS_ENCORE_MODIFIABLES` = `BROUILLON`, `SOUMIS`, `INCOMPLET` — pour déposer une pièce **et** pour
re-soumettre. La refonte du workflow (22/09) l'avait remplacé par `PRESELECTIONNE` / `PAIEMENT_EN_ATTENTE`,
soit exactement l'inverse : un dossier tout juste soumis, ou renvoyé « à compléter », ne pouvait plus rien
ajouter. Les cinq tests de `CandidatControllerTeleverserPieceTest` le voyaient, mais on les tenait pour
périmés. Le paiement, lui, n'est ouvert qu'après la présélection (`PRESELECTIONNE` / `PAIEMENT_EN_ATTENTE`) :
là, c'étaient les tests qui étaient périmés.

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

**Le cercle de profil en en-tête doit toujours mener quelque part.** Il l'a été posé
sans lien ni gestionnaire de clic dans l'espace étudiant et l'espace pédagogique — un
élément qui ressemble à un bouton mais n'en est pas un. Un chevron (`expand_more`)
accompagne désormais chaque cercle pour signaler qu'il est cliquable. Le responsable
pédagogique n'avait même aucune page « Mon profil » ; comme il n'a pas de fiche dédiée
(pas de téléphone à tenir à jour), la sienne se limite à l'identité lue depuis
`Auth.currentUser()` et au changement de mot de passe via l'endpoint générique
`POST /api/auth/change-password`, qui sert les trois rôles.

**`@EnableScheduling` existe depuis `EvaluationRappelService`** (TDR §G, rappel
automatique d'évaluation) — c'était la première tâche planifiée de tout ce backend.
Posé sur `LmsApplication`, volontairement au seul endroit qui en a besoin plutôt que
dans une classe de configuration séparée créée par anticipation. Toute nouvelle tâche
`@Scheduled` peut désormais s'appuyer sur cette annotation déjà présente — ne pas la
reposer ailleurs ni la dupliquer. `Cours` n'a aucune notion de session/édition datée :
un rappel « à la fin de chaque session » au sens strict du TDR n'est donc pas
possible aujourd'hui ; le rappel actuel se déclenche tant qu'un cours autorisé reste
non évalué, avec un délai de repos (`COOLDOWN_JOURS`) plutôt qu'une vraie date de fin.
Si le besoin d'une session datée se confirme un jour, c'est une décision de modèle à
instruire à part (voir `LOG_BLOQUANT.md`), pas une extension mécanique de ce service.

**Le certificat suit le modèle officiel de l'INPG, il n'est pas redessiné.** `CertificatPdfService` charge
`backend/src/main/resources/certificats/certificat-formation-fond.pdf` (le modèle dont les six zones variables
ont été vidées) et n'y superpose que : civilité + nom, « né le », formation, période, « Fait à Dakar, le … »,
code de bas de page, plus un QR de vérification. Positions et polices sont relevées dans le README du même
dossier. Le fichier d'origine (`backend/Modèle Certificat INPG.pdf`) porte les données d'une personne réelle :
il est dans `.gitignore`, ne jamais le versionner. Les champs du modèle (`civilite`, `date_naissance`,
`date_debut`, `date_fin`, `code_certificat`) sont nullables et **scellés dans la signature seulement quand
ils existent** : un certificat délivré avant leur ajout garde son contenu canonique, donc sa signature reste
valide. `CertificatPdfServiceTest` relit le texte du PDF produit. Sa **date de délivrance est le jour où le
responsable pédagogique crée le certificat** (fuseau `Africa/Dakar`, posée par le serveur, jamais saisie) : le
formulaire n'a volontairement pas de champ « date de délivrance ». Le QR pointe vers
`APP_FRONTEND_URL/verification-certificat/<référence>` — en production, cette variable doit être le domaine
public et permanent de l'INPG (un QR imprimé ne se corrige pas), pas `localhost` ni un tunnel ; la référence
est aussi imprimée sous le QR pour une saisie manuelle. La page publique qu'il ouvre (`CertificatPublicController`,
sans connexion) restitue ce qu'un faussaire retoucherait sur un vrai document — nom, formation, **période**,
**code**, date d'émission, validité de la signature — mais jamais la date de naissance, pourtant imprimée :
c'est une donnée d'identité. Aucune limitation de débit n'y protège le balayage des références (8 caractères
hexadécimaux aléatoires) : à ajouter avant la mise en production.

**Tout PDF sort sur le papier à en-tête de l'INPG, par un moteur commun — ne pas réécrire de mini-moteur dans
un service.** `pdf/PapierEnTete` (PDFBox) pose sur chaque page le logo, le pied de page (adresse, téléphone,
site cliquable) et la pagination ; `pdf/RapportPdf` met en page les rapports (retour à la ligne, sauts de page,
tableaux, tuiles de chiffres clés, filigrane) ; `pdf/PapierEnTeteOpenPdf` fait de même pour les trois documents
encore écrits avec OpenPDF (lettre d'admission, convocation, reçu). Mesures, logo et icônes viennent du
document Word fourni par l'INPG — voir `resources/pdf/README.md`. Chaque rapport avait auparavant son propre
moteur, avec les mêmes défauts : titres « barrés » (le filet était tracé en travers du texte au lieu de dessous),
commentaires coupés par « ... », ni logo ni pied de page. Deux pièges d'OpenPDF : ne jamais imbriquer des
`Paragraph` dans un `Paragraph` conteneur — l'espacement et l'alignement se perdent, les titres se décentrent et
les blocs se recouvrent — mais ajouter les éléments un à un (`PapierEnTeteOpenPdf.ajouter`). Et un test de PDF
doit relire le fichier **produit** (texte extrait, position des glyphes par rapport aux traits), comme
`RapportPdfTest` : constater seulement « un fichier est généré » ne voyait aucun de ces défauts.

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
| Alumni | `test.alumni@inpg.sn` |
| École de formation *(candidate à l'accréditation)* | `test.ecole@inpg.sn` |
| Évaluateur accréditation | `test.evaluateur@inpg.sn` |

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
