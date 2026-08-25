-- =====================================================================
-- V4 — Supprimer les vestiges du renommage Cohorte -> Promotion
-- =====================================================================
--
-- CONTEXTE
-- La fusion du 14/08/2026 a renommé Cohorte en Promotion, Annonce en
-- Annonces et Photo en Photos. Hibernate tournait alors en ddl-auto=update,
-- qui sait ajouter une table ou une colonne mais jamais en supprimer : les
-- anciennes structures sont donc restées en base, sans plus aucune entité
-- JPA en face.
--
-- ddl-auto=validate, en place depuis, ne les signale pas non plus : il
-- vérifie que chaque entité a sa table, jamais l'inverse. Une table sans
-- entité lui est parfaitement invisible — au même titre que le contenu
-- d'une contrainte CHECK (voir V2).
--
-- CE QUI EST SUPPRIMÉ
--   cohorte                 3 lignes   remplacée par promotion
--   cours_cohorte           3 lignes   jointure de l'ancienne cohorte
--   annonce                 2 lignes   remplacée par annonces
--   annonce_cohorte         0 ligne    jointure de l'ancienne annonce
--   photo                   3 lignes   remplacée par photos
--   criteres_selection      1 ligne    remplacée par les colonnes
--                                      criteres_* de sessions_candidature
--   resposable_peda_entity  1 ligne    entité supprimée, table restée
--                                      (la faute de frappe part avec)
--   etudiants.cohorte_id    6 valeurs  colonne morte sur une table vivante
--   certificat.cohorte_id   2 valeurs  colonne morte sur une table vivante
--
-- Ces 13 lignes sont du contenu de démonstration antérieur à la fusion.
-- Les DataInitializer et les scripts de seed regénèrent l'équivalent ;
-- aucune donnée réelle n'est concernée.
--
-- CE QUI N'EST PAS TOUCHÉ
-- sujet/message et forum_sujets/forum_messages se ressemblent mais sont
-- servis par deux contrôleurs distincts, tous deux vivants : ce n'est pas
-- une duplication accidentelle, et rien n'est supprimé de ce côté.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Colonnes mortes sur des tables vivantes
--    À traiter en premier : leur contrainte de clé étrangère référence
--    « cohorte », qui ne peut pas être supprimée tant qu'elle subsiste.
--    Aucune entité ne les mappe, donc aucun code ne les lit ni ne les
--    écrit — c'est précisément ce qui les rend invisibles et dangereuses.
-- ---------------------------------------------------------------------
ALTER TABLE etudiants  DROP COLUMN IF EXISTS cohorte_id;
ALTER TABLE certificat DROP COLUMN IF EXISTS cohorte_id;

-- ---------------------------------------------------------------------
-- 2. Tables de jointure de l'ancien modèle
--    Avant leurs tables parentes, sinon les clés étrangères bloquent.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cours_cohorte;
DROP TABLE IF EXISTS annonce_cohorte;

-- ---------------------------------------------------------------------
-- 3. Tables remplacées
--    « photo » porte une clé étrangère vers « cohorte » : elle part avant.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS photo;
DROP TABLE IF EXISTS annonce;
DROP TABLE IF EXISTS cohorte;

-- ---------------------------------------------------------------------
-- 4. Tables sans entité, hors renommage
--    criteres_selection : les critères de présélection sont désormais des
--    colonnes de sessions_candidature (criteres_actifs, criteres_age_*...).
--    resposable_peda_entity : le rôle RESPONSABLE_PEDAGOGIQUE suffit, il
--    n'y a plus d'entité de profil dédiée.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS criteres_selection;
DROP TABLE IF EXISTS resposable_peda_entity;

-- =====================================================================
-- Après cette migration, chaque table du schéma a une entité JPA ou une
-- @JoinTable en face. Pour le vérifier après un futur renommage :
--
--   SELECT tablename FROM pg_tables WHERE schemaname = 'public';
--
-- et confronter la liste aux @Table(name = ...) du code. Hibernate ne le
-- fera pas à votre place.
-- =====================================================================
