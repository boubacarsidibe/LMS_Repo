-- Civilité et date de naissance de l'étudiant, mémorisées sur sa fiche.
--
-- Le certificat au modèle INPG imprime « Monsieur/Madame NOM, né(e) le JJ/MM/AAAA ».
-- Ces deux valeurs n'existaient nulle part sur l'étudiant : elles se ressaisissaient à
-- chaque certificat, ce qui est tenable pour un document mais pas pour toute une
-- promotion délivrée d'un coup. Elles sont désormais lues sur la fiche, et complétées
-- à la délivrance si elles manquent.
--
-- Le certificat, lui, garde sa propre copie (colonnes déjà présentes sur `certificat`) :
-- un document remis ne se réécrit pas parce qu'on corrige une fiche (cf. CLAUDE.md,
-- relevés délivrés).

ALTER TABLE public.etudiants
    ADD COLUMN civilite       VARCHAR(20),
    ADD COLUMN date_naissance DATE;
