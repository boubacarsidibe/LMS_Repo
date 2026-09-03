-- =====================================================================
-- V31 — Accréditation : la demande porte sur un PROGRAMME, décision
--       conditionnelle, validité 3 ans, référentiel DRCQ codifié
-- =====================================================================
--
-- CONTEXTE
-- Le module d'accréditation a été bâti sur le seul TDR §F. Les deux
-- référentiels officiels de la DRCQ (Cadre de référence, rév. 19/01/2026 ;
-- Référentiel d'accréditation Oil & Gas, rév. 23/03/2026) précisent :
--
--   * « L'accréditation INPG concerne les programmes de formation, et non
--     l'établissement dans son ensemble » (Cadre §II) — une demande doit
--     donc décrire le programme visé, sa cohorte et le type de structure.
--   * La décision peut être « une accréditation pleine, une accréditation
--     conditionnelle assortie d'un plan d'actions, ou un refus »
--     (Cadre §IV.8).
--   * « Chaque programme accrédité bénéficie d'une période de validité de
--     trois ans » (Cadre §V.4) — d'où une date d'expiration.
--   * Le Référentiel énumère 31 conditions codées (1.1 … 4.5) réparties en
--     4 sections — criteres_accreditation gagne code / section / ordre pour
--     porter cette structure (amorçage : AccreditationReferentielInitializer).
--
-- Toutes les colonnes ajoutées sont nullables (ou à défaut) : les demandes
-- déjà en base (données de démonstration) restent valides. Le caractère
-- obligatoire des champs programme est imposé côté application, pour les
-- nouvelles soumissions uniquement (EcoleAccreditationController).
-- statut reste un VARCHAR(30) : la nouvelle valeur ACCREDITE_SOUS_RESERVE
-- ne demande aucune modification de schéma.
-- =====================================================================

-- ---------- 1. La demande décrit le programme visé ----------
ALTER TABLE public.demandes_accreditation
    ADD COLUMN intitule_programme  VARCHAR(200),
    ADD COLUMN niveau_programme    VARCHAR(30),
    ADD COLUMN type_structure      VARCHAR(40),
    ADD COLUMN effectif_cohorte    INTEGER,
    ADD COLUMN date_debut_cohorte  DATE;

-- ---------- 2. Décision conditionnelle + validité 3 ans ----------
ALTER TABLE public.demandes_accreditation
    ADD COLUMN plan_actions    VARCHAR(2000),
    ADD COLUMN date_expiration DATE;

-- ---------- 3. Référentiel DRCQ codifié ----------
ALTER TABLE public.criteres_accreditation
    ADD COLUMN code    VARCHAR(10),
    ADD COLUMN section VARCHAR(120),
    ADD COLUMN ordre   INTEGER NOT NULL DEFAULT 0;

-- Les lignes directrices concaténées (colonne description) dépassent 500
-- caractères pour plusieurs conditions du référentiel.
ALTER TABLE public.criteres_accreditation
    ALTER COLUMN description TYPE VARCHAR(2000);

-- Un code de référentiel identifie un critère de façon unique quand il est
-- renseigné ; les critères ad hoc créés à la main (code NULL) restent permis.
CREATE UNIQUE INDEX idx_critere_accreditation_code
    ON public.criteres_accreditation (code)
    WHERE code IS NOT NULL;
