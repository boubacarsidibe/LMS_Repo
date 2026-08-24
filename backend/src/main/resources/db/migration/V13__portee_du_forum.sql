-- Portee d'un sujet de forum (suite de TDR section C, « Espace Alumni »).
--
-- ForumSujet.cours distingue deja un sujet rattache a un cours d'un sujet general
-- (cours IS NULL), mais rien ne distingue QUELLE categorie d'utilisateurs voit un
-- sujet general : la migration V12 (espace alumni) avait explicitement laisse ce
-- point de cote, faute de decision de modele (voir LOG_BLOQUANT.md, point 8).
--
-- Deux valeurs seulement : ETUDIANT (comportement actuel, valeur par defaut — les
-- 216 sujets existants restent visibles exactement comme avant) et ALUMNI (nouveaux
-- sujets ouverts par des alumni, dans leur propre espace). Un sujet rattache a un
-- cours garde toujours portee = ETUDIANT : la portee n'a de sens que pour un sujet
-- general, exactement comme documente au point 8 de LOG_BLOQUANT.md.
--
-- Pas de valeur MIXTE : rien dans le TDR ne demande un fil partage entre etudiants
-- et alumni, et l'ajouter sans besoin reel recreerait le genre de decision non
-- sollicitee que CLAUDE.md met en garde contre. Une valeur supplementaire de l'enum
-- resterait une migration additive si le besoin apparait reellement.

ALTER TABLE public.forum_sujets
    ADD COLUMN portee VARCHAR(20) NOT NULL DEFAULT 'ETUDIANT';

ALTER TABLE public.forum_sujets
    ADD CONSTRAINT forum_sujets_portee_valide CHECK (portee IN ('ETUDIANT', 'ALUMNI'));
