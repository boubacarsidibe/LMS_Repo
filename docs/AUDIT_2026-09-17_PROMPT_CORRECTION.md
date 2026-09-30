# Mission de correction — LMS INPG

Tu interviens sur le dépôt LMS INPG (Spring Boot 4 + Angular 20 + PostgreSQL 16, pile
Docker Compose). Un audit en exécution réelle a été mené le 2026-09-17 sur la pile
locale ; les douze défauts ci-dessous ont tous été **reproduits**, pas déduits de la
lecture du code. Ta mission est de les corriger, dans l'ordre donné.

---

## Règles du projet — à lire avant toute modification

Le fichier `CLAUDE.md` à la racine fait autorité. Les points qui vont te concerner :

1. **Méthode imposée.** Reproduis le défaut *avant* de le déclarer, corrige, puis
   revérifie **en exécution réelle sur les conteneurs** — pas seulement en relisant le
   code. Vérifie qu'un test de non-régression échoue bien sur le code d'avant correction.
2. **Fins de ligne.** Beaucoup de fichiers sont en CRLF. Un remplacement multi-lignes
   écrit avec `\n` échoue **en silence**. Détecte toujours :
   ```python
   raw = p.read_bytes().decode("utf-8")
   NL = "\r\n" if "\r\n" in raw else "\n"
   ```
   N'utilise **jamais** `sed -i` ni `Path.write_text()` sur ces fichiers : ils
   normalisent les fins de ligne et transforment un correctif de 20 lignes en diff de 2000.
3. **Migrations.** Ne renomme jamais `V1`–`V47`. Toute nouvelle migration prend la forme
   `V<AAAAMMJJHHMM>__sujet.sql` (horodatage, jamais un entier séquentiel).
4. **Charte graphique.** Tout le style passe par les jetons de `frontend/tailwind.config.js`.
   Piège central : `font-<échelle>` ne définit que la **famille**, la taille vient de
   `text-<échelle>`, une classe **distincte**. Les deux doivent être présentes.
5. **Cache de build.** Si une modification frontend semble ne pas s'appliquer, le suspect
   est `frontend/.angular/cache`, pas le navigateur :
   ```bash
   docker compose stop frontend && rm -rf frontend/.angular/cache frontend/dist && docker compose up -d frontend
   ```
6. **Base de démonstration.** Cible tout `DELETE` par un `WHERE`, restaure après tout
   essai destructif : cette base sert aux démonstrations client.
7. **Réponses en français**, chaque terme technique expliqué brièvement.

### Démarrer la pile

```bash
docker compose up -d                 # backend:8080, frontend:4200, postgres:5432
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-comptes-test.sql
```

Comptes : mot de passe `Test@2026` pour tous les `test.*@inpg.sn`
(`test.pedago`, `test.admission`, `test.etudiant`, `test.formateur`, `test.alumni`,
`test.candidat2`, `test.ecole`, `test.evaluateur`), `prof.demo@inpg.sn` / `Demo@2026`,
`admin@inpg.sn` / `Admin@2026`.

---

## LOT 1 — Bloquant : trois écrans en erreur 500

### 1.1 `LazyInitializationException` sur les sessions et les paramètres

**Reproduction :**
```bash
TOK=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"test.admission@inpg.sn","password":"Test@2026"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")
curl -s -w "\n%{http_code}\n" -H "Authorization: Bearer $TOK" http://localhost:8080/api/sessions/kpis
curl -s -w "\n%{http_code}\n" -H "Authorization: Bearer $TOK" http://localhost:8080/api/sessions/programs
curl -s -w "\n%{http_code}\n" -H "Authorization: Bearer $TOK" http://localhost:8080/api/settings/programs
```
Les trois renvoient **500**. Trace serveur (`docker compose logs backend`) :
```
org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role
'inpg.sn.lms.recrutement.entity.SessionCandidature.candidatures' with key '1' (no session)
  at inpg.sn.lms.recrutement.controller.SessionController.lambda$getKpis$1(SessionController.java:57)
```

**Cause.** `spring.jpa.open-in-view=false` (choix délibéré). `SessionController` et
`SettingsController` ne portent **aucun** `@Transactional` : la session Hibernate est
déjà fermée quand le contrôleur parcourt `s.getCandidatures()` (ligne 57) ou quand
Jackson sérialise l'entité. C'est la famille de bugs que le `CLAUDE.md` signale comme
étant « survenue trois fois » et que `EspaceEtudiantGardeFousTest` protège — mais ce
garde-fou ne couvre que l'espace étudiant.

**Attendu.**
- Ajouter `@Transactional(readOnly = true)` sur toutes les méthodes de lecture de
  `SessionController` et `SettingsController`, et `@Transactional` sur les écritures.
- `SettingsController.listPrograms()` renvoie `List<Program>`, une **entité JPA brute**.
  Le `CLAUDE.md` rappelle que des endpoints sérialisant les entités brutes ont déjà été
  retirés pour cette raison. Introduis un DTO (`ProgramDto`) plutôt que d'élargir la
  transaction pour contourner le symptôme.
- Étendre le garde-fou : écris un test qui **échoue sur le code actuel** et vérifie que
  tout endpoint de ces deux contrôleurs répond sans exception avec des données réelles
  en base. Inspire-toi de `EspaceEtudiantGardeFousTest`.

**Dette liée, à traiter dans la foulée.** Onze autres contrôleurs n'ont aucun
`@Transactional` et exposent le même risque. Par volume d'endpoints :
`AdminRecrutementController` (54), `AuthController` (10), `AnalyticsController` (8),
`InterviewController` (6), `AcademicTestController` (5), `PublicController` (4),
`NotificationController` (4), `DashboardController` (4), `AdminController` (2),
`StatusController` (1), `CertificatPublicController` (1).
Traite-les par ordre de volume, en vérifiant chaque endpoint en exécution réelle —
n'ajoute pas l'annotation mécaniquement partout sans vérifier ce qu'elle change.

### 1.2 Cinq cours sur huit sont invisibles dans tout espace formateur

**Reproduction :**
```bash
docker exec demo-postgres psql -U admin -d myappdb -c \
"SELECT titre, COALESCE((SELECT 'oui' FROM cours_formateur cf WHERE cf.cours_id=c.id LIMIT 1),'AUCUN') AS formateur FROM cours c ORDER BY id;"
```
Résultat sur une base neuve : **5 cours sur 8 sans ligne dans `cours_formateur`**
(« Géologie des réservoirs », « Techniques de forage », « Production et traitement »,
« Sécurité et environnement », « Économie pétrolière »). Vérification côté API :
`GET /api/espace-etudiant/formateur/cours` avec `prof.demo@inpg.sn` ne renvoie que 3 cours.

**Cause.** `cours_formateur` commande tout le cloisonnement enseignant
(`PerimetreFormateurService` y lit « ce cours est-il le mien ? »). Un cours absent de
cette table n'apparaît dans **aucun** espace formateur : ni support, ni séance, ni note
n'y sont possibles. Rien ne le signale côté base — `ddl-auto=validate` ne compare jamais
deux relations entre elles. Le `CLAUDE.md` documente déjà ce piège (« Six cours sur neuf
étaient dans ce cas ») : il **se reproduit à chaque installation neuve**, parce que
`DataInitializer` crée les cours de « Master Pétrole & Gaz » sans jamais les rattacher.

**Attendu.**
- Corriger `backend/src/main/java/inpg/sn/lms/config/DataInitializer.java` pour que tout
  cours créé soit rattaché à au moins un formateur.
- Ne pas se contenter du seed : ajouter un test qui échoue si un cours amorcé se retrouve
  sans formateur.
- Évaluer l'ajout d'un signalement côté espace pédagogique (le `CLAUDE.md` indique que
  les trois maillons `promotion → formation → cours → formateur` sont déjà signalés en
  rouge — vérifier que ce cas précis y remonte bien, et ne **pas** retirer ces alertes).

### 1.3 Le script de données de démonstration échoue intégralement

**Reproduction :**
```bash
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-client.sql
```
```
ERROR: insert or update on table "progressions_support" violates foreign key constraint
DETAIL: Key (support_id)=(13) is not present in table "support_cours".
ROLLBACK
```
Le script est enveloppé dans une transaction : **rien n'est inséré**, aucun dégât, mais
les données de démonstration ne se chargent jamais. Le `CLAUDE.md` le présente pourtant
comme « relançable sans risque » et comme ce qui « remplit les écrans qui sonnent creux
en démonstration ». En l'état, toute démonstration client part d'écrans vides.

**Attendu.** Corriger `backend/db/seed-demo-client.sql` (ligne 51 et suivantes) pour qu'il
ne présuppose plus d'identifiants figés : résoudre les `support_id` par sous-requête sur
le code/titre du support plutôt qu'en dur. Le script doit rester **relançable sans
risque** (idempotent). Vérifie en le lançant deux fois de suite sur une base fraîche.

---

## LOT 2 — Cohérence d'interface : le kit UI maison est contourné

Le projet possède un kit d'interface complet et soigné dans
`frontend/src/app/shared/components/ui/` : `notification.service.ts`,
`confirmation.service.ts`, `ui-confirmation.component.ts`, `ui-notifications.component.ts`,
`ui-empty-state.component.ts`, `ui-spinner.component.ts`, `ui-badge.component.ts`.

**Mais 21 appels à `confirm()` / `alert()` natifs du navigateur subsistent dans
16 composants**, alors que `ConfirmationService` n'est utilisé que par 4 composants.
Résultat : selon l'écran, supprimer un élément ouvre soit une boîte de dialogue maison
aux couleurs de l'INPG, soit la fenêtre grise native du navigateur affichant l'URL du
Codespace. C'est le défaut le plus visible en démonstration.

**Reproduction :**
```bash
cd frontend/src && grep -rn "[^a-zA-Z.]confirm(\|[^a-zA-Z.]alert(" --include="*.ts" app \
  | grep -v spec | grep -v "shared/components/ui/"
```

Fichiers concernés (liste non exhaustive, régénère-la avec la commande ci-dessus) :
`recrutement-formateurs/admin/offres-admin/offres-admin.component.ts:146`,
`pedagogie/pages/etudiants/pedagogie-etudiants.component.ts:238`,
`pedagogie/pages/formateurs/pedagogie-formateurs.component.ts:88`,
`accreditation/pages/demande-accreditation/demande-accreditation.ts:648`,
`personnel/pages/activites-personnel/activites-personnel.component.ts:140`,
`recrutement-formateurs/pages/ma-candidature/ma-candidature.ts:896`,
`evaluateur-accreditation/pages/criteres/criteres.ts:295`,
`rendez-vous/pages/rendez-vous/rendez-vous.component.ts:223`, plus les composants
`responsable-admission/*` (candidatures, sessions, entretiens, tests, paiements).

**Attendu.** Remplacer chaque `confirm()` par `ConfirmationService` et chaque `alert()`
par `NotificationService`, en conservant le texte existant des messages. Attention :
`confirm()` est **synchrone** et `ConfirmationService` est asynchrone — la logique
appelante doit être réécrite (promesse ou observable), pas seulement l'appel. Vérifie
chaque écran en exécution réelle après conversion : une suppression qui ne se déclenche
plus est pire que la boîte native.

---

## LOT 3 — Trois conventions concurrentes pour signaler une erreur de champ

Le même besoin — « ce champ est invalide » — est traité de trois façons différentes :

| Convention | Exemple | Statut |
|---|---|---|
| `text-error` / `border-error` (jetons) | `features/auth/login/login.html:41-43` | **la bonne** |
| `text-[#ba1a1a]` (hex en dur) | `recrutement/candidature/formulaire/formulaire.component.html:84,87` | à convertir |
| `text-red-500` (palette Tailwind par défaut) | `features/auth/forgot-password/forgot-password.html` | à convertir |

`#ba1a1a` **est** la valeur du jeton `error` : la correction ne change rien visuellement,
elle supprime une entorse au système. Plus largement, **715 couleurs hexadécimales en dur**
subsistent hors `tailwind.config.js` dans les `.html` et `.css`.

**Attendu.**
1. Aligner d'abord les formulaires sur la convention par jetons (`text-error`,
   `border-error`), en commençant par `formulaire.component.html` (le formulaire de
   candidature, le plus utilisé du produit) et `forgot-password.html`.
2. `recrutement/catalogue/contact/contact.component.html` a **6 champs et aucun retour
   d'erreur visible** — ajoute la validation en suivant le patron de `login.html`.
3. Résorber progressivement les couleurs en dur. Ne fais pas un
   chercher-remplacer aveugle : vérifie que chaque hex correspond bien au jeton que tu lui
   substitues, et procède par fichier avec vérification visuelle.

**Piège de charte à corriger au passage** — 19 endroits déclarent `font-<échelle>` sans le
`text-<échelle>` correspondant, donc s'affichent en 16 px au lieu de leur taille prévue :
```bash
cd frontend/src && python3 - <<'PY'
import re, pathlib
ech=['display','headline-lg','headline-md','headline-sm','title-lg','title-md','title-sm',
     'body-lg','body-md','body-sm','label-lg','label-md','label-sm']
for p in pathlib.Path('app').rglob('*.html'):
    t=p.read_text(encoding='utf-8',errors='ignore')
    for m in re.finditer(r'class="([^"]*)"', t):
        for e in ech:
            if f'font-{e}' in m.group(1) and f'text-{e}' not in m.group(1):
                print(p, '->', e)
PY
```
Principaux fichiers : `espace-etudiants/pages/mes-cours/mes-cours.html` (6),
`mon-profil/mon-profil.html` (3), `retours-evaluations/retours-evaluations.html` (3),
`espace-alumni/pages/mon-parcours/mon-parcours.html` (2),
`profil-etudiant/profil-etudiant.html` (2).

---

## LOT 4 — Accessibilité

Mesures relevées sur `frontend/src/app` :

| Défaut | Volume | Conséquence |
|---|---|---|
| `<input>` sans `id`, donc sans `<label for>` possible | **203** | Lecteur d'écran muet ; cliquer le libellé ne place pas le curseur dans le champ |
| Boutons à icône seule sans `aria-label` | **55** | Annoncés « bouton » sans indication de leur fonction |
| `<img>` sans `alt` | 5 | Image non décrite |

Les libellés visuels existent la plupart du temps, mais ne sont pas **associés
programmatiquement** au champ. Exemples : `pedagogie-promotions.component.html`,
`pedagogie-certificats.component.html`.

**Attendu.** Ajouter `id` sur les champs et `for` sur les `<label>` correspondants,
`aria-label` sur les boutons à icône seule, `alt` sur les images. Commence par les
parcours publics (inscription, connexion, candidature, catalogue) : ce sont les seuls
accessibles sans compte, et ceux qu'un audit externe regardera en premier.

---

## LOT 5 — Le frontend n'est pas testé

| | Fichiers | Tests |
|---|---|---|
| Backend | 67 classes | **414** tests |
| Frontend | 21 specs | **22** `it()` |

22 tests pour 21 fichiers : ce sont, à une exception près, les smoke tests
« should create » générés par Angular CLI. **Le frontend, où se concentrent les défauts
signalés par le client, n'a aucune couverture réelle.** C'est la cause structurelle
derrière plusieurs bugs récents passés inaperçus jusqu'en démonstration — dont un bouton
« Créer un compte » qui ne donnait aucun retour visible sur formulaire invalide.

**Attendu.** Ne vise pas un pourcentage de couverture. Écris des tests sur les
comportements qui ont déjà cassé :
- un formulaire invalide affiche bien un message par champ (inscription, candidature,
  contact, mot de passe oublié) ;
- un bouton de soumission passe bien en état « en cours » puis revient ;
- une suppression demande confirmation et n'agit qu'après acceptation ;
- une liste vide affiche l'état vide (`ui-empty-state`) et non une page blanche.

`frontend/tsconfig.spec.json` existe déjà et la CI (`.github/workflows/build.yml`) lance
déjà les tests Angular — l'infrastructure est en place, seul le contenu manque.

---

## LOT 6 — Points mineurs

1. **Six boutons sans action** (ni `(click)`, ni `type="submit"`, ni `routerLink`) :
   2 dans `recrutement/candidature/formulaire/formulaire.component.html`, 1 dans chacun de
   `espace-etudiants/pages/planning-calendrier/planning-calendrier.html`,
   `espace-etudiants/pages/phototheque/phototheque.html`,
   `recrutement/catalogue/formation-detail/formation-detail.component.html`,
   `shared/components/navbar/navbar.component.html`. Un élément qui ressemble à un bouton
   sans en être un est précisément ce que le `CLAUDE.md` reproche déjà au cercle de profil.
   Soit tu les câbles, soit tu les retires.
2. **404 au lieu d'un état vide.** Les endpoints `/api/espace-etudiant/scolarite/*` sont
   ouverts à `hasAnyRole('ETUDIANT','FORMATEUR')`, mais renvoient 404 pour un formateur
   sans fiche étudiant. Techniquement défendable, pratiquement trompeur : côté navigateur,
   rien ne distingue « cette page n'existe pas » de « vous n'avez pas de scolarité ».
   Préfère une réponse 200 avec une charge utile vide, ou un 403 explicite.
3. **Un `console.log` résiduel** dans `frontend/src/app/core/auth/services/authService.ts`.

---

## Ce qui va bien — ne le casse pas

L'audit a aussi vérifié ces points, qui sont corrects et ne demandent **aucune**
intervention :

- **CORS** (`config/CorsConfig.java`) : origines explicites, pas de caractère générique,
  `allowCredentials` correctement géré, `Content-Disposition` exposé pour les
  téléchargements. Ne le « simplifie » pas.
- **Swagger / `/v3/api-docs`** : derrière authentification (401 sans jeton).
- **Secrets** : `.env` correctement ignoré par git, seul `.env.example` est versionné,
  aucune fuite dans l'historique.
- **Cloisonnement par rôle** : sur 1026 appels croisés (114 endpoints × 9 rôles),
  692 réponses 403 conformes. Le cloisonnement fonctionne.
- **Propreté du code** : aucun `TODO`/`FIXME`/`HACK`, un seul `console.log`.
- **Interface entièrement en français**, sans mélange de langues.
- **414 tests backend** qui passent.

---

## Ordre de traitement recommandé

1. **Lot 1** d'abord : ce sont les seuls défauts qui cassent des écrans en production.
2. **Lot 2** ensuite : c'est le plus visible en démonstration client, pour un coût faible.
3. **Lot 5** en parallèle des suivants : chaque correction des lots 3/4/6 devrait arriver
   avec son test, plutôt que de traiter la dette de test comme un chantier séparé.
4. **Lots 3, 4, 6** progressivement, fichier par fichier, avec vérification visuelle.

Pour chaque lot : un commit par sujet cohérent, message en français expliquant le
**pourquoi** et pas seulement le quoi, et mention explicite de la vérification faite en
exécution réelle (commande lancée, résultat observé avant et après).
