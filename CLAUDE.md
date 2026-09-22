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
<span class="text-2xl font-extrabold tracking-tight text-primary">INPG<span class="text-brand"> Connect</span></span>
```

Pas de badge ni d'icône. Le jeton `brand` (`#131741`) est le bleu nuit du suffixe.
Dans l'espace pédagogique, le suffixe est « Pédagogie » — même traitement.

**Renommé « INPG LMS » → « INPG Connect » le 2026-09-22** : le sigle LMS (Learning
Management System) restreignait un produit qui dépasse largement la formation en ligne
(admission, accréditation d'écoles partenaires, espace entreprise, alumni, évaluation
d'accréditation). Le suffixe « Pédagogie » de l'espace pédagogique n'est **pas** concerné
— ce n'était déjà pas « LMS » avant, c'est une variante délibérée, pas un oubli. Si un
nouvel espace ajoute son propre logo, chercher `INPG<span class="text-brand">` pour
retrouver tous les endroits à traiter ensemble — neuf occurrences y sont passées en une
fois (navbar public, six espaces à barre latérale, bandeau de la liste des écoles
accréditées, pied de page, panneau de la page de connexion), plus un intitulé de sujet de
formulaire de contact (« Support Technique LMS » → « Support Technique »).

### Contraste : le piège des jetons jumeaux

`primary-fixed` et `primary-container` valent **la même couleur** (`#d4e3ff`). Poser
l'un comme texte sur l'autre comme fond donne un contraste de 1:1 — du texte
littéralement invisible. C'était le cas de la date des séances, des initiales
d'avatar et de l'icône du logo.

> Texte sur `bg-primary-container` → **`text-on-primary-container`** (`#001b3d`).
> Jamais `text-primary-fixed`, jamais `text-on-primary`.

L'avatar de profil est plein : `bg-primary`, initiales `text-white`, `shadow-sm`.

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

**Le type d'inscription oriente, il n'autorise pas.** Les rôles métier
(`ROLE_ECOLE_FORMATION`, `ROLE_ENTREPRISE`, `ROLE_CANDIDAT_FORMATEUR`, `ROLE_CANDIDAT`)
ne sont accordés qu'à la **première soumission réelle** ; une école ou une entreprise
qui s'est inscrite sans rien déposer n'a donc **aucun rôle**. Seul `typeInscription`
(`CANDIDAT`/`ECOLE`/`ENTREPRISE`/`FORMATEUR`, colonne `users.type_inscription`) dit ce
qu'elle est. Il doit voyager jusqu'au navigateur : `AuthController.toUserMap` en est
l'**unique** fabrique (connexion, `/current-user`, inscription) — ces trois réponses
avaient divergé, seule l'inscription le renvoyait, et une école qui se reconnectait le
lendemain retombait sur l'accueil des candidats étudiants, sans le moindre lien vers son
espace, ni erreur ni journal. Côté Angular, `Auth.getProfilCompte()` en tire le profil :
**le rôle fait foi dès qu'il existe**, le type ne parle que quand la liste des rôles est
vide, et un compte qui porte un autre rôle (étudiant, personnel) n'est jamais reclassé
d'après son type. `getHomeUrl()` s'appuie dessus en dernier recours (école →
`/accreditation`, entreprise → `/espace-entreprise`, formateur → `/offres-formateurs`,
comme l'inscription). Le navbar public partagé (onglet propre, libellé, menu du cercle de
profil) et les boutons du héros de `/accueil` suivent ce profil : **le compte prime sur la
page** — `ContexteNavbarService` ne pilote plus que les visiteurs non connectés.
**Un seul navbar pour tout le parcours** : les espaces École, Entreprise et Candidat
formateur sont des enfants de `PublicNavbarLayoutComponent` (`app.routes.ts`), donc
l'`app-navbar` n'est jamais recréé en passant d'« Institut » à « Espace entreprise ». Leurs
layouts ne portent plus que le fond de page — ne pas leur redonner une barre propre : les
onglets changeaient d'un coup à chaque passage (retour du 2026-09-21). Pas de cloche de
notifications pour ces trois profils : c'est celle des candidats, elle resterait vide.

**Le navbar public est pleine largeur, pas centré à 1280 px comme le contenu des pages**
(retour du 2026-09-22) : sur un grand écran, le logo et le cercle de profil restaient à
360 px du bord réel sur un écran de 1920 px, parce que la ligne du navbar reprenait
`max-w-container-max mx-auto` — pertinent pour le contenu, pas pour une barre qui doit
toucher les bords comme celles des espaces à sidebar (Admission, Pédagogie…). Les onglets
du centre restent groupés entre eux (`flex-1 justify-center` sur leur propre `<nav>`), ils
ne s'étalent pas sur toute la largeur.

**Sous le seuil `md`, le nom cède sa place au bouton menu, à l'extrême gauche** (retour du
2026-09-22) : `<a routerLink="/accueil">` porte `hidden md:inline` — **jamais**
`md:inline-flex` : ça transforme le lien en conteneur flexbox, dont les enfants (le texte
« INPG » et le `<span>` du suffixe) deviennent des items flex, ce qui **supprime l'espace
entre les deux** (« INPGConnect » collé, repéré via `.innerText()` qui renvoie alors un
saut de ligne entre les deux au lieu d'une espace). Le bouton `(click)="toggleMobileMenu()"`
est déplacé en premier enfant du conteneur du navbar (visible seulement sous `md`, `-ml-2`
pour compenser le padding) plutôt que dupliqué : un seul bouton, un seul `aria-label`.

**« Accueil » comme onglet explicite : seulement pour le candidat/étudiant, pas pour
École/Entreprise/Formateur.** Décision du 2026-08-25 revenue en recette le 2026-09-22 :
compter sur le seul logo comme retour à l'accueil n'était pas assez visible pour ce
public (`test.candidat@inpg.sn` et consorts). École, Entreprise et Candidat formateur
n'ont **pas** ce même onglet : ils ont déjà un équivalent — leur onglet propre
(« Accréditation », « Espace entreprise »…) mène à leur espace, et « Mon espace » du menu
de profil y ramène aussi ; ajouter « Accueil » chez eux serait redondant, pas chez le
candidat/étudiant qui n'a ni l'un ni l'autre. Le visiteur non connecté partage le même
contexte `'etudiant'` que le candidat et voit donc aussi cet onglet.
`auth.spec.ts`, `navbar.component.spec.ts`, `landing.component.spec.ts` et
`AuthControllerTest` figent ce comportement.

**L'erreur d'un champ s'affiche dès le premier caractère invalide, ou après le clic sur
le bouton d'envoi — jamais sur un champ vide qu'on a seulement quitté.** Une seule règle,
`erreurVisible()` (`shared/utils/erreur-champ.ts`), pour la connexion, l'inscription et le
contact. Elle a été réécrite trois fois : « au blur » (un champ vide rougissait dès qu'on le
quittait, et le bouton de connexion, désactivé tant que le formulaire était invalide,
interdisait tout clic), puis « après le clic seulement » (une saisie fausse restait muette),
puis celle-ci. Ne pas rebrancher `touched` dans ces formulaires. À l'inscription, le clic
ajoute un bandeau qui nomme le premier champ bloquant et y place le curseur. « Mot de passe
oublié » garde sa propre règle.

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

**Aucun compte de recette n'est sans rôle** — tous en portent un, ce qui masque toute une
famille de défauts propres au compte *fraîchement inscrit* (école, entreprise ou formateur
n'ayant encore rien soumis : les rôles métier ne s'accordent qu'à la première soumission).
Pour les reproduire, créer un compte jetable puis le retirer par un `DELETE` ciblé :

```bash
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"verif_ecole","email":"verif.ecole@inpg.sn","password":"Verif@2026","typeInscription":"ECOLE"}'
docker exec -i demo-postgres psql -U admin -d myappdb \
  -c "DELETE FROM users WHERE email = 'verif.ecole@inpg.sn';"   # après l'essai
```

`typeInscription` vaut `CANDIDAT`, `ECOLE`, `ENTREPRISE` ou `FORMATEUR`.

---

## 5. Méthode attendue

Reproduire un défaut avant de le déclarer, le corriger, puis le revérifier **en
exécution réelle** sur les conteneurs — pas seulement en lecture de code. Vérifier
qu'un test de non-régression échoue bien sur le code d'avant correction.

Restaurer les données après tout essai destructif, et cibler les `DELETE` par un
`WHERE` — la base sert aux démonstrations client.

Réponses en français, chaque terme technique expliqué brièvement.
