-- =====================================================================
-- Comptes de test — recette manuelle (16/08/2026)
-- =====================================================================
--
-- CE N'EST PAS UNE MIGRATION DE SCHÉMA, au même titre que
-- seed-demo-formateur.sql : ce script ajoute du CONTENU (comptes et un
-- dossier de candidature) et ne touche à aucune structure de table. Il
-- reste volontairement hors de src/main/resources/db/migration — le
-- mélanger à Flyway empêcherait de le rejouer après une remise à zéro.
--
-- Objectif : disposer d'un compte propre et prévisible pour CHACUN des
-- rôles, afin de tester chaque espace sans dépendre des comptes de
-- démo (dont les mots de passe diffèrent d'un rôle à l'autre) ni des
-- comptes historiques restés sans rôle. Complété le 2026-08-24 avec
-- ROLE_ECOLE_FORMATION et ROLE_EVALUATEUR_ACCREDITATION (TDR §F).
--
-- Mot de passe unique pour tous ces comptes : Test@2026
-- Le hachage est produit par pgcrypto (`gen_salt('bf', 10)`), ce qui
-- donne un bcrypt `$2a$10$…` — exactement le format que lit le
-- BCryptPasswordEncoder de Spring Security (voir PasswordConfig).
--
-- Réexécutable sans dupliquer : ON CONFLICT DO NOTHING sur les tables à
-- contrainte d'unicité, WHERE NOT EXISTS ailleurs.
--
-- Utilisation :
--   docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-comptes-test.sql
-- =====================================================================

BEGIN;

-- pgcrypto fournit crypt()/gen_salt() : sans elle, il faudrait générer les
-- hachages bcrypt hors de la base et les coller en dur ici.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------
-- 1. Les comptes
--    Un seul hachage est calculé puis réutilisé : tous ces comptes
--    partagent le mot de passe Test@2026, ce qui est le but recherché
--    pour de la recette manuelle.
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password, dtype)
SELECT v.username, v.email, crypt('Test@2026', gen_salt('bf', 10)), 'User'
FROM (VALUES
    ('test_candidat',  'test.candidat@inpg.sn'),
    ('test_candidat2', 'test.candidat2@inpg.sn'),
    ('test_etudiant',  'test.etudiant@inpg.sn'),
    ('test_formateur', 'test.formateur@inpg.sn'),
    ('test_admission', 'test.admission@inpg.sn'),
    ('test_pedago',    'test.pedago@inpg.sn'),
    ('test_alumni',    'test.alumni@inpg.sn'),
    ('test_ecole',      'test.ecole@inpg.sn'),
    ('test_evaluateur', 'test.evaluateur@inpg.sn')
) AS v(username, email)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = v.username OR u.email = v.email);

-- ---------------------------------------------------------------------
-- 2. Les rôles
--    Un compte ne détient qu'un seul rôle : c'est la règle appliquée par
--    le code (CandidatController#garantirRoleCandidat vide les rôles
--    avant d'ajouter CANDIDAT, AdminController#changeUserRoles fait de
--    même). Ces comptes doivent donc refléter cette exclusivité.
-- ---------------------------------------------------------------------
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM (VALUES
    ('test_candidat',  'ROLE_CANDIDAT'),
    ('test_candidat2', 'ROLE_CANDIDAT'),
    ('test_etudiant',  'ROLE_ETUDIANT'),
    ('test_formateur', 'ROLE_FORMATEUR'),
    ('test_admission', 'ROLE_RESPONSABLE_ADMISSION'),
    ('test_pedago',    'ROLE_RESPONSABLE_PEDAGOGIQUE'),
    ('test_alumni',    'ROLE_ALUMNI'),
    ('test_ecole',      'ROLE_ECOLE_FORMATION'),
    ('test_evaluateur', 'ROLE_EVALUATEUR_ACCREDITATION')
) AS v(username, code)
JOIN users u ON u.username = v.username
JOIN roles r ON r.code = v.code
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. Les profils métier
--    Chaque table de profil partage sa clé primaire avec users.id
--    (@MapsId côté JPA) : id_user est à la fois PK et FK. Sans la ligne
--    de profil, l'espace correspondant se comporte comme si le compte
--    n'existait pas — un formateur sans ligne « formateurs » ne voit
--    aucun cours.
-- ---------------------------------------------------------------------

-- Candidats : date_naissance, nationalite et niveau_etude sont NOT NULL.
-- Les deux candidats diffèrent volontairement par le niveau d'étude et
-- l'âge, pour pouvoir tester le filtre automatique de présélection
-- (SessionCandidature#respecteCriteres) une fois criteres_actifs activé.
INSERT INTO candidats (id_user, nom, prenom, telephone, nationalite, date_naissance, niveau_etude, genre, profession, annees_experience)
SELECT u.id, v.nom, v.prenom, v.telephone, v.nationalite, v.date_naissance::date, v.niveau_etude, v.genre, v.profession, v.annees_experience
FROM (VALUES
    ('test_candidat',  'Diop',  'Awa',    '+221770000101', 'Sénégalaise', '1999-04-12', 'Licence (L3)', 'F', 'Analyste junior', 2),
    ('test_candidat2', 'Sarr',  'Modou',  '+221770000102', 'Sénégalaise', '1992-11-03', 'Master 1 (M1)', 'M', 'Ingénieur', 7)
) AS v(username, nom, prenom, telephone, nationalite, date_naissance, niveau_etude, genre, profession, annees_experience)
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM candidats c WHERE c.id_user = u.id);

-- Étudiant : rattaché à la promotion existante, sinon « Ma promotion »,
-- le calendrier et les cours autorisés restent vides.
INSERT INTO etudiants (id_user, nom, prenom, telephone, matricule, promotion_id)
SELECT u.id, 'Ndour', 'Bineta', '+221770000103', 'ETU-TEST-001',
       (SELECT p.id FROM promotion p ORDER BY p.id LIMIT 1)
FROM users u
WHERE u.username = 'test_etudiant'
  AND NOT EXISTS (SELECT 1 FROM etudiants e WHERE e.id_user = u.id);

-- Alumni : un alumni EST un etudiant (@MapsId), sa ligne etudiants reste donc
-- necessaire (matricule, releve). Voir migration V12.
INSERT INTO etudiants (id_user, nom, prenom, telephone, matricule)
SELECT u.id, 'Gueye', 'Rokhaya', '+221770000106', 'ETU-TEST-002'
FROM users u
WHERE u.username = 'test_alumni'
  AND NOT EXISTS (SELECT 1 FROM etudiants e WHERE e.id_user = u.id);

INSERT INTO alumni_profils (id_user, poste_actuel, entreprise, secteur_activite)
SELECT u.id, 'Consultante Energie', 'Cabinet ENR Conseil', 'Energies renouvelables'
FROM users u
WHERE u.username = 'test_alumni'
  AND NOT EXISTS (SELECT 1 FROM alumni_profils a WHERE a.id_user = u.id);

INSERT INTO formateurs (id_user, nom, prenom, telephone)
SELECT u.id, 'Camara', 'Ousmane', '+221770000104'
FROM users u
WHERE u.username = 'test_formateur'
  AND NOT EXISTS (SELECT 1 FROM formateurs f WHERE f.id_user = u.id);

INSERT INTO responsables_admission (id_user, nom, prenom, telephone)
SELECT u.id, 'Faye', 'Aminata', '+221770000105'
FROM users u
WHERE u.username = 'test_admission'
  AND NOT EXISTS (SELECT 1 FROM responsables_admission ra WHERE ra.id_user = u.id);

INSERT INTO resposable_peda_entity (id_user)
SELECT u.id
FROM users u
WHERE u.username = 'test_pedago'
  AND NOT EXISTS (SELECT 1 FROM resposable_peda_entity rp WHERE rp.id_user = u.id);

-- École de formation : profil créé normalement à la première soumission
-- (EcoleAccreditationController#soumettreDemande), reproduit ici pour un
-- compte de recette déjà utilisable sans passer par le formulaire.
INSERT INTO ecoles_formation (id_user, nom_ecole, adresse, telephone, site_web)
SELECT u.id, 'Institut Supérieur de Formation Test', '12 rue des Tests, Dakar', '+221770000107', 'https://isft-test.example.sn'
FROM users u
WHERE u.username = 'test_ecole'
  AND NOT EXISTS (SELECT 1 FROM ecoles_formation e WHERE e.id_user = u.id);

-- test_evaluateur n'a volontairement aucune ligne de profil : rôle de
-- staff assigné par un responsable admission (écran générique), pas un
-- espace auto-inscrit — voir LOG_BLOQUANT.md point 17.

-- ---------------------------------------------------------------------
-- 4. Un dossier déjà soumis, pour test_candidat2
--    Le back-office (candidatures, tests, entretiens, délibération) a
--    ainsi une ligne à manipuler dès la première connexion.
--    test_candidat reste SANS dossier, volontairement : c'est le compte
--    avec lequel dérouler le parcours de dépôt de bout en bout.
-- ---------------------------------------------------------------------
INSERT INTO candidatures (numero_dossier, statut, date_soumission, candidat_id, session_candidature_id)
SELECT 'INPG-TEST0002', 'SOUMIS', NOW(), c.id_user,
       (SELECT s.id FROM sessions_candidature s WHERE s.est_ouverte ORDER BY s.id LIMIT 1)
FROM candidats c
JOIN users u ON u.id = c.id_user AND u.username = 'test_candidat2'
WHERE NOT EXISTS (SELECT 1 FROM candidatures k WHERE k.numero_dossier = 'INPG-TEST0002')
  AND EXISTS (SELECT 1 FROM sessions_candidature s WHERE s.est_ouverte);

-- ---------------------------------------------------------------------
-- 5. Une demande d'accréditation déjà soumise, pour test_ecole
--    Ainsi test_evaluateur a immédiatement une ligne à consulter/décider
--    dès la première connexion (statut SOUMISE, pas encore prise en charge).
-- ---------------------------------------------------------------------
INSERT INTO demandes_accreditation (ecole_id, statut, date_soumission)
SELECT e.id_user, 'SOUMISE', NOW()
FROM ecoles_formation e
JOIN users u ON u.id = e.id_user AND u.username = 'test_ecole'
WHERE NOT EXISTS (
    SELECT 1 FROM demandes_accreditation d
    WHERE d.ecole_id = e.id_user AND d.statut IN ('SOUMISE', 'EN_COURS_EVALUATION')
);

COMMIT;

-- ---------------------------------------------------------------------
-- Contrôle : doit afficher 9 lignes, chacune avec exactement un rôle.
-- ---------------------------------------------------------------------
SELECT u.username, u.email, r.code AS role
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.role_id
WHERE u.username LIKE 'test\_%'
ORDER BY r.code, u.username;
