-- =====================================================================
-- V34 — Élargir le libellé d'un critère d'accréditation
-- =====================================================================
--
-- La colonne « Condition » du Référentiel DRCQ dépasse 200 caractères pour
-- plusieurs critères (notamment 2.1). VARCHAR(200) suffisait pour un
-- référentiel saisi à la main, plus pour le texte officiel transcrit.
-- =====================================================================

ALTER TABLE public.criteres_accreditation
    ALTER COLUMN libelle TYPE VARCHAR(600);
