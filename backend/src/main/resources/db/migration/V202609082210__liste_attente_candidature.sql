-- =====================================================================
-- Liste complémentaire (TDR §3.1.C) — nouvelle valeur LISTE_ATTENTE
-- =====================================================================
--
-- Le frontend affichait déjà ce statut (libellé « Liste complémentaire »,
-- étape 5 de la timeline, message candidat), mais EnumStatutCandidature ne
-- le déclarait pas : le code était mort et, surtout, aucun moyen n'existait
-- de placer un candidat en liste d'attente — alors que le TDR §3.1.C exige
-- la « saisie des listes retenus et en attente à l'issue des entretiens ».
--
-- V2__statuts_admission_candidature.sql prévenait : toute constante ajoutée
-- à l'énumération Java doit être ajoutée à la contrainte CHECK par une
-- NOUVELLE migration. C'est l'objet de ce fichier — sans lui, l'endpoint
-- échouerait en ConstraintViolationException remontée en HTTP 500, et
-- ddl-auto=validate ne le signalerait pas (il ne lit pas les CHECK).
--
-- Première migration horodatée : voir CLAUDE.md, la numérotation par
-- entiers provoquait des collisions entre travaux parallèles.
-- =====================================================================

ALTER TABLE candidatures DROP CONSTRAINT IF EXISTS candidatures_statut_check;

ALTER TABLE candidatures ADD CONSTRAINT candidatures_statut_check
    CHECK (statut IN (
        'BROUILLON',
        'SOUMIS',
        'EN_COURS_DE_PRESELECTION',
        'ADMIS1',
        'ADMIS2',
        'LISTE_ATTENTE',
        'ADMIS3',
        'REJETE'
    ));
