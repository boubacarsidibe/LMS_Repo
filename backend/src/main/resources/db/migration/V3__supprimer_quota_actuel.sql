-- =====================================================================
-- V3 — Retirer le compteur quota_actuel des sessions de candidature
-- =====================================================================
--
-- PROBLÈME CORRIGÉ
-- sessions_candidature.quota_actuel était lu à onze endroits (tableau de
-- bord, export CSV, KPI, résumé de session) mais incrémenté nulle part :
-- aucun code ne le touchait à la soumission d'un dossier. Le « taux de
-- remplissage » affiché au back-office restait donc figé sur la valeur
-- posée à la création de la session — un chiffre décoratif.
--
-- CHOIX RETENU
-- L'INPG suit le remplissage sans bloquer les dépôts : quota_max reste,
-- comme repère indicatif. Le nombre de dossiers est désormais compté à la
-- demande dans « candidatures », où il est vrai par construction, plutôt
-- que recopié dans une colonne qui peut diverger. Une requête de plus,
-- une désynchronisation de moins.
--
-- Le contrat de l'API ne change pas : les réponses exposent toujours la
-- clé « quotaActuel », mais avec une valeur calculée.
-- =====================================================================

ALTER TABLE sessions_candidature DROP COLUMN IF EXISTS quota_actuel;

-- =====================================================================
-- Si le plafond devait un jour être réellement appliqué, la règle irait
-- dans le code de soumission (CandidatController), pas dans une colonne :
-- c'est au moment d'accepter un dossier que la comparaison a un sens.
-- =====================================================================
