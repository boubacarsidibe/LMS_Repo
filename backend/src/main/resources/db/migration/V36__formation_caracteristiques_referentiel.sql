-- =====================================================================
-- V36 — Caractéristiques du §3 du Référentiel d'accréditation DRCQ sur le
--       catalogue de formations
-- =====================================================================
--
-- Le Référentiel d'accréditation Oil & Gas (§3 « Structure et contenu des
-- programmes ») impose des caractéristiques mesurables : heures de simulation
-- en laboratoire INPG (20 h pour un technicien, 70 h pour un ingénieur — §3.1),
-- enseignement d'anglais professionnel (§3.4), compétences transversales /
-- soft skills (§3.5), unités d'enseignement avec volume horaire et crédits
-- (§3.6). Le catalogue (table formation) ne portait aucun de ces attributs.
--
-- Colonnes nullables (ou par défaut false) : les 16 fiches déjà en base restent
-- valides, l'amorçage complète les valeurs connues.
-- =====================================================================

ALTER TABLE public.formation
    ADD COLUMN heures_simulation_labo  INTEGER,
    ADD COLUMN anglais_professionnel   BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN soft_skills             BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN credits_totaux          INTEGER,
    ADD COLUMN volume_horaire_total    INTEGER;
