-- Forum scope par promotion (souhait utilisateur adjacent au TDR §B, journal du
-- 2026-08-26) : un etudiant doit pouvoir publier dans un fil vu de sa seule
-- promotion, et non plus seulement dans un salon general (portee = ETUDIANT) ou
-- rattache a un cours precis.
--
-- Additif, a cote de cours_id et portee — jamais en remplacement. Comme pour
-- cours_id, le rattachement est optionnel : un sujet ne porte au plus qu'un seul
-- des deux (cours OU promotion), jamais les deux, la logique applicative dans
-- ForumVisibiliteService le fait respecter.

ALTER TABLE public.forum_sujets
    ADD COLUMN promotion_id BIGINT REFERENCES public.promotion (id);

CREATE INDEX idx_forum_sujets_promotion ON public.forum_sujets (promotion_id);
