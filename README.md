# LMS INPG

Plateforme de gestion de la formation de l'**Institut National du Pétrole et du Gaz** (Dakar).

Elle couvre le parcours d'un apprenant de bout en bout : dépôt de candidature, présélection,
test académique, entretien, admission, puis accès à l'espace étudiant (cours, calendrier,
forum, notes, certificats).

---

## Démarrage rapide

### Prérequis

- **Docker** et **Docker Compose** — c'est tout. Java et Node sont fournis par les images.
- Ports libres : `4200` (frontend), `8080` (backend), `5432` (PostgreSQL).

### 1. Configurer les secrets

```bash
cp backend/.env.example backend/.env
```

Puis remplir `backend/.env`. Deux valeurs sont **obligatoires**, l'application refuse de
démarrer sans elles :

| Variable | Comment l'obtenir |
|---|---|
| `JWT_SECRET` | `openssl rand -base64 64` — minimum 64 caractères |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Identifiants SMTP Brevo |

> `backend/.env` n'est **jamais** commité. Une clé SMTP a déjà fuité par le passé et a dû
> être révoquée : aucun secret ne doit revenir dans un fichier versionné.

### 2. Lancer

```bash
docker compose up -d --build
```

Au premier démarrage, Flyway crée le schéma et les `DataInitializer` insèrent les données de
démonstration. Comptez une à deux minutes.

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API | http://localhost:8080 |

> **Swagger est installé mais inaccessible.** La dépendance `springdoc-openapi` est bien
> présente, mais `/swagger-ui/**` et `/v3/api-docs` ne figurent pas dans les routes ouvertes
> de `SecurityConfig` : ils répondent `403`. À corriger le jour où l'on veut s'en servir.

### 3. Charger les comptes de test

```bash
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-comptes-test.sql
```

---

## Comptes

### Comptes de recette — mot de passe `Test@2026`

Un compte par rôle, pour tester chaque espace sans effet de bord.

| E-mail | Rôle | Particularité |
|---|---|---|
| `test.candidat@inpg.sn` | Candidat | Aucun dossier — pour dérouler le dépôt complet |
| `test.candidat2@inpg.sn` | Candidat | Dossier `INPG-TEST0002` déjà soumis |
| `test.etudiant@inpg.sn` | Étudiant | Rattaché à la promotion existante |
| `test.formateur@inpg.sn` | Formateur | |
| `test.admission@inpg.sn` | Responsable Admission | Back-office recrutement |
| `test.pedago@inpg.sn` | Responsable Pédagogique | Offre de cours |

### Comptes de démonstration

Créés automatiquement au démarrage, avec des mots de passe **différents par rôle** :

| Identifiant | Mot de passe |
|---|---|
| `admin@inpg.sn` | `Admin@2026` |
| `prof.demo@inpg.sn` | `Demo@2026` |
| `etudiant@inpg.sn` et les 6 étudiants de démo | `Etudiant@2026` |

> Un compte ne peut détenir **qu'un seul rôle**. Déposer une candidature depuis un compte
> qui possède déjà un espace (étudiant, formateur…) est refusé — sinon le compte perdrait
> l'accès à ses cours et ses notes.

---

## Base de données

Le schéma est piloté par **Flyway**, pas par Hibernate : `ddl-auto=validate` se contente de
vérifier que les entités correspondent aux tables et refuse de démarrer sinon.

**Les migrations vivent dans `backend/src/main/resources/db/migration/` :**

| Version | Objet |
|---|---|
| `V1__baseline.sql` | Schéma initial (50 tables) |
| `V2__statuts_admission_candidature.sql` | Aligne la contrainte de statut sur `ADMIS1/2/3` |
| `V3__supprimer_quota_actuel.sql` | Retire un compteur qui n'était jamais incrémenté |

**Règle absolue : une migration appliquée ne se modifie jamais.** Flyway en conserve une
empreinte et refuse de démarrer si elle change. Pour corriger, on écrit une migration
suivante.

📖 **Détails, dépannage et procédure d'ajout : [backend/db/README.md](backend/db/README.md)**

### Scripts hors Flyway

Ce sont du **contenu**, pas du schéma — donc volontairement en dehors de `db/migration/`, ce
qui permet de les rejouer après une remise à zéro. À relancer à la main :

```bash
# Comptes de recette (voir plus haut)
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-comptes-test.sql

# Contenu de démonstration pour l'espace formateur (séances, notes, présences, avis)
docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-formateur.sql
```

---

## Tests

Le `Dockerfile` construit l'image avec `-DskipTests` pour aller plus vite — **cela ne veut pas
dire qu'il n'y a pas de tests.** Pour les lancer :

```bash
cd backend && mvn -o test
```

Les tests sont des tests unitaires Mockito, sans contexte Spring : ils s'exécutent en quelques
secondes.

---

## Architecture

```
├── backend/          Spring Boot 4 · Java 17 · PostgreSQL · JWT
│   ├── src/main/java/inpg/sn/lms/
│   │   ├── auth/            Comptes, rôles, connexion, OTP
│   │   ├── recrutement/     Candidatures, tests, entretiens, paiements
│   │   ├── espace_etudiant/ Cours, calendrier, forum, notes, certificats
│   │   ├── security/        Filtre JWT
│   │   ├── config/          Sécurité, CORS, Jackson, données initiales
│   │   └── exception/       Exceptions métier et gestionnaire global
│   └── db/           Scripts hors Flyway + documentation base de données
├── frontend/         Angular 21 · Tailwind CSS 4
│   └── src/app/
│       ├── core/           Authentification, gardes de route, intercepteurs
│       ├── features/       Un dossier par domaine métier
│       └── shared/         Composants réutilisables
└── docker-compose.yml
```

### Les cinq rôles

| Rôle | Espace |
|---|---|
| `ROLE_CANDIDAT` | Dépôt et suivi de candidature |
| `ROLE_RESPONSABLE_ADMISSION` | Back-office recrutement, délibération, statistiques |
| `ROLE_ETUDIANT` | Cours, calendrier, forum, notes, certificats |
| `ROLE_FORMATEUR` | Ses cours, ses supports, le suivi de ses étudiants |
| `ROLE_RESPONSABLE_PEDAGOGIQUE` | Pilotage de l'offre de formation |

### Configuration

Tout se règle par variables d'environnement (voir `backend/src/main/resources/application.properties`) :

| Variable | Défaut | Rôle |
|---|---|---|
| `APP_CORS_ORIGINS` | `http://localhost:4200` | Origines autorisées à appeler l'API. **À renseigner en production.** |
| `APP_FRAIS_DOSSIER` | `25000` | Frais de dossier, en XOF |
| `APP_UPLOAD_DIR` | `/app/uploads` | Dossier des fichiers déposés |

---

## Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| Le backend ne démarre pas, `JWT_SECRET` manquant | `backend/.env` absent ou incomplet | Copier `.env.example` et remplir |
| `Schema-validation: missing column` | Entité modifiée sans migration | Écrire la migration `V…__.sql` correspondante |
| `Migration checksum mismatch` | Une migration déjà appliquée a été modifiée | Restaurer le fichier d'origine ; corriger via une nouvelle migration |
| Le frontend n'atteint pas l'API | Origine non autorisée | Vérifier `APP_CORS_ORIGINS` |
| Écrans vides côté formateur | Données de démonstration absentes | Rejouer `seed-demo-formateur.sql` |

**Repartir d'une base neuve — en développement uniquement :**

```bash
docker compose down -v && docker compose up -d --build
```

Les données de démonstration sont regénérées ; les deux scripts de seed sont à rejouer.
**Ne jamais faire cela sur des données réelles.**
