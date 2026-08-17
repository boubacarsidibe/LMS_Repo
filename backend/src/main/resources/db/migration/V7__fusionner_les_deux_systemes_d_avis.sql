-- =====================================================================
-- Fusion des deux systemes d'avis sur les cours
-- =====================================================================
--
-- Deux tables recueillaient l'appreciation d'un cours par un etudiant, sans
-- jamais se parler :
--
--   evaluations_cours   ecrite par l'etudiant (POST /evaluations/cours/{id}),
--                       relue par lui seul (« Mes evaluations »).
--   retour_evaluation   lue par le formateur (« Retours & evaluations »),
--                       alimentee par POST /retours — qu'aucun code frontend
--                       n'appelle.
--
-- Consequence : l'avis depose par un etudiant n'atteignait jamais son
-- formateur, et l'ecran du formateur ne pouvait afficher que des donnees de
-- demonstration. Le §3.7 du cahier des charges demande un suivi qualite ; en
-- l'etat, rien ne remontait.
--
-- On retient retour_evaluation : c'est un sur-ensemble (statut et epinglage en
-- plus), et l'ecran formateur qui s'en sert exploite reellement ces colonnes
-- (filtres, epinglage, export CSV). Les deux echelles de note sont identiques
-- (1 a 5), la fusion ne perd donc aucune information.

-- ---------------------------------------------------------------------
-- 1. Un avis peut n'etre qu'une note, sans texte : le commentaire etait
--    obligatoire ici alors qu'il etait facultatif cote etudiant.
-- ---------------------------------------------------------------------
ALTER TABLE retour_evaluation ALTER COLUMN commentaire DROP NOT NULL;

-- ---------------------------------------------------------------------
-- 2. Reporter les avis existants. Le statut se deduit de la note, selon la
--    regle deja appliquee par POST /retours : au plus 3 sur 5 appelle une
--    action du formateur.
-- ---------------------------------------------------------------------
INSERT INTO retour_evaluation (cours_id, etudiant_id, note, commentaire, date_creation, statut, epingle)
SELECT e.cours_id, e.etudiant_id, e.note, e.commentaire, e.date_soumission,
       CASE WHEN e.note <= 3 THEN 'ACTION_REQUISE' ELSE 'VALIDE' END, false
FROM evaluations_cours e
WHERE NOT EXISTS (
    SELECT 1 FROM retour_evaluation r
    WHERE r.etudiant_id = e.etudiant_id AND r.cours_id = e.cours_id);

-- ---------------------------------------------------------------------
-- 3. Un etudiant n'a qu'un avis par cours — le redeposer le met a jour.
--    evaluations_cours portait cette regle, pas retour_evaluation : sans elle,
--    le meme etudiant pouvait empiler des avis sur un meme cours et fausser la
--    moyenne affichee.
-- ---------------------------------------------------------------------
ALTER TABLE retour_evaluation
    ADD CONSTRAINT uk_retour_evaluation_etudiant_cours UNIQUE (etudiant_id, cours_id);

DROP TABLE IF EXISTS evaluations_cours;
