-- Fiche de poste jointe à une offre de recrutement de formateur (QMS-007 §1).
--
-- Un seul fichier par offre, facultatif : le responsable admission peut joindre le
-- document officiel du poste (PDF), que les candidats téléchargent depuis l'offre publiée.
-- Le fichier lui-même vit dans fichiers_stockes, comme les pièces des candidatures.

ALTER TABLE public.offres_recrutement_formateur
    ADD COLUMN fiche_poste_id BIGINT REFERENCES public.fichiers_stockes (id);
