-- =====================================================================
-- Unification des deux definitions du « cours suivi »
-- =====================================================================
--
-- Le code repondait de deux facons a « cet etudiant suit-il ce cours ? » :
--
--   chaine A  Etudiant -> Promotion -> Formation -> formation_cours
--             (HabilitationCoursService, utilise par Scolarite/Forum/Evaluation)
--   chaine B  Cours -> cours_promotion -> Promotion -> Etudiant
--             (PerimetreFormateurService, utilise par tout le cote formateur)
--
-- Les deux ensembles avaient diverge au point de ne plus designer AUCUN cours
-- commun. La totalite des notes, des supports et des rattachements de formateurs
-- se trouvait du cote que les etudiants ne pouvaient pas atteindre : un etudiant
-- voyait « aucune note » et recevait 403 sur le cours portant les siennes, et le
-- meme PDF etait refuse par une route et servi par l'autre.
--
-- On retient la chaine A. Un cours appartient a un programme (une formation), et
-- une promotion suit ce programme : toute promotion de la formation herite donc
-- du cours, sans rattachement manuel a repeter a chaque rentree. C'est la regle
-- du §3.2 du cahier des charges, et elle repond aux deux sens de la question
-- (les cours d'un etudiant, les etudiants d'un cours) — cours_promotion
-- n'apportait rien qu'elle ne sache deja dire.

-- ---------------------------------------------------------------------
-- 1. Ne rien perdre : tout cours enseigne a une promotion rejoint le
--    programme de la formation de cette promotion.
--    ON CONFLICT rend l'operation rejouable sans erreur.
-- ---------------------------------------------------------------------
INSERT INTO formation_cours (formation_id, cours_id)
SELECT DISTINCT p.formation_id, cp.cours_id
FROM cours_promotion cp
JOIN promotion p ON p.id = cp.promotion_id
WHERE p.formation_id IS NOT NULL
ON CONFLICT (formation_id, cours_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. Supprimer la relation concurrente.
--    Tant qu'elle existe, la divergence peut se reformer : c'est
--    precisement ce qui s'etait produit, la table etant alimentee par un
--    chemin (rattachement manuel du formateur) que personne ne comparait
--    a l'autre.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cours_promotion;
