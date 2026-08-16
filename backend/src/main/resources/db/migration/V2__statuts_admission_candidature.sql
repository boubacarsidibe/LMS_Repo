-- =====================================================================
-- V2 — Aligner la contrainte de statut des candidatures sur l'énumération Java
-- =====================================================================
--
-- PROBLÈME CORRIGÉ
-- V1__baseline.sql a figé « candidatures_statut_check » avec la liste
-- (BROUILLON, SOUMIS, EN_COURS_DE_PRESELECTION, ADMIS, REJETE), héritée
-- d'un schéma antérieur. Or EnumStatutCandidature déclare trois paliers
-- d'admission distincts — ADMIS1 (test académique), ADMIS2 (entretien),
-- ADMIS3 (admission finale) — et aucune valeur « ADMIS ».
--
-- Conséquence : les trois endpoints /candidatures/{id}/admettre-* du
-- back-office échouaient en ConstraintViolationException, remontée en
-- HTTP 500. Aucun candidat ne pouvait dépasser EN_COURS_DE_PRESELECTION,
-- et le recrutement s'arrêtait là.
--
-- POURQUOI RIEN NE L'A SIGNALÉ
-- spring.jpa.hibernate.ddl-auto=validate compare les tables et les
-- colonnes aux entités, mais ne lit pas le contenu des contraintes CHECK.
-- Le backend démarrait donc normalement ; le défaut ne se manifestait
-- qu'à la première tentative d'admission.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Convertir les éventuelles lignes « ADMIS » héritées
--    Sans rôle sur une base neuve, mais indispensable sur une base
--    ancienne : une ligne « ADMIS » ne correspond à aucune constante de
--    EnumStatutCandidature et ferait échouer sa simple lecture par
--    Hibernate. « ADMIS » désignait l'admission aboutie, ce que porte
--    aujourd'hui ADMIS3 — le dernier des trois paliers.
--    À exécuter AVANT de poser la nouvelle contrainte, qui refuserait
--    ces lignes.
-- ---------------------------------------------------------------------
UPDATE candidatures SET statut = 'ADMIS3' WHERE statut = 'ADMIS';

-- ---------------------------------------------------------------------
-- 2. Remplacer la contrainte
--    DROP puis ADD : PostgreSQL ne sait pas modifier une contrainte
--    CHECK en place. IF EXISTS rend l'étape rejouable sur une base où
--    la contrainte aurait déjà été retirée à la main.
-- ---------------------------------------------------------------------
ALTER TABLE candidatures DROP CONSTRAINT IF EXISTS candidatures_statut_check;

ALTER TABLE candidatures ADD CONSTRAINT candidatures_statut_check
    CHECK (statut IN (
        'BROUILLON',
        'SOUMIS',
        'EN_COURS_DE_PRESELECTION',
        'ADMIS1',
        'ADMIS2',
        'ADMIS3',
        'REJETE'
    ));

-- =====================================================================
-- NOTE POUR LES PROCHAINES ÉVOLUTIONS
-- Toute constante ajoutée à EnumStatutCandidature doit être ajoutée ici
-- par une NOUVELLE migration (V3, V4…). Ce fichier ne doit plus être
-- modifié une fois appliqué : Flyway en conserve une empreinte dans
-- flyway_schema_history et refuse de démarrer si elle change.
-- =====================================================================
