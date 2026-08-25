-- =====================================================================
-- Rattrapage de schéma après la fusion recrutement/pédagogie (14/08/2026)
-- =====================================================================
--
-- POURQUOI CE FICHIER
-- Le projet utilise spring.jpa.hibernate.ddl-auto=update. Hibernate sait
-- AJOUTER une colonne, mais ne sait ni en supprimer une devenue obsolète,
-- ni ajouter une colonne NOT NULL à une table qui contient déjà des lignes.
-- La fusion a fait les deux, donc le backend refusait de démarrer sur une
-- base créée avant la fusion.
--
-- QUI DOIT L'EXÉCUTER
-- Toute personne dont la base date d'avant le 14/08/2026. Si vous partez
-- d'une base vide (volume Docker supprimé), Hibernate crée le bon schéma
-- tout seul et ce script est inutile.
--
-- COMMENT L'EXÉCUTER
--   docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/2026-08-14_fusion-promotion.sql
--
-- Sauvegardez d'abord :
--   docker exec demo-postgres pg_dump -U admin -d myappdb > backup.sql
--
-- Le script est idempotent (IF EXISTS / IF NOT EXISTS) : le relancer ne
-- casse rien.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. seance : « date » (DATE) a été remplacée par « date_heure » (TIMESTAMP)
--    quand la séance est devenue un vrai créneau de planning. Hibernate a
--    ajouté date_heure mais a laissé « date » en NOT NULL : tout INSERT
--    échouait sur une colonne que le code ne renseigne plus.
-- ---------------------------------------------------------------------
ALTER TABLE seance DROP COLUMN IF EXISTS date;

-- ---------------------------------------------------------------------
-- 2. sessions_candidature : cinq colonnes NOT NULL ajoutées par la branche
--    recrutement. Hibernate n'a pas pu les créer car les lignes existantes
--    auraient valu NULL. On les ajoute avec un DEFAULT, ce qui remplit les
--    lignes déjà présentes au passage.
-- ---------------------------------------------------------------------
ALTER TABLE sessions_candidature ADD COLUMN IF NOT EXISTS quota_max           integer      NOT NULL DEFAULT 220;
ALTER TABLE sessions_candidature ADD COLUMN IF NOT EXISTS quota_actuel        integer      NOT NULL DEFAULT 0;
ALTER TABLE sessions_candidature ADD COLUMN IF NOT EXISTS status              varchar(255) NOT NULL DEFAULT 'BROUILLON';
ALTER TABLE sessions_candidature ADD COLUMN IF NOT EXISTS criteres_actifs     boolean      NOT NULL DEFAULT false;
ALTER TABLE sessions_candidature ADD COLUMN IF NOT EXISTS seuil_reussite_test real         NOT NULL DEFAULT 10;

-- Le DEFAULT ne connaît pas l'état réel des sessions : on le rétablit, sinon
-- une session ouverte se retrouverait en brouillon et disparaîtrait des écrans.
UPDATE sessions_candidature SET status = 'OUVERT' WHERE est_ouverte = true;

-- Idem pour le compteur de places : le remettre au nombre réel de dossiers.
UPDATE sessions_candidature s
   SET quota_actuel = (SELECT COUNT(*) FROM candidatures c WHERE c.session_candidature_id = s.id);

COMMIT;

-- =====================================================================
-- POUR LA SUITE
-- Ce fichier est un rattrapage manuel, pas une vraie migration. Avant la
-- mise en production il faudra passer à Flyway (ou Liquibase) et repasser
-- ddl-auto à « validate » : sinon chaque renommage se reproduira, et en
-- production les données ne seront pas jetables.
-- =====================================================================
