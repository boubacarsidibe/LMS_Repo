-- Ce que le candidat saisit au dépôt et que le serveur ne gardait pas (audit du parcours candidat, 2026-10-13).
--
-- Le formulaire exige une adresse postale et une certification sur l'honneur ; le serveur n'avait ni colonne
-- pour l'une (le DTO d'état civil ignorait « adresse » sans erreur), ni trace de l'autre. Les parcours
-- académique et professionnel, eux, avaient bien leurs colonnes (candidatures.informations_academiques et
-- informations_professionnelles) mais aucun code ne les remplissait : voir CandidatController#soumettreCandidature.
--
-- Colonnes facultatives : les dossiers déjà déposés n'ont pas ces informations, et rien ne permet de les
-- reconstituer — elles n'ont jamais quitté le navigateur.

ALTER TABLE public.candidats
    ADD COLUMN adresse VARCHAR(500);

-- Instant où le candidat a certifié sur l'honneur l'exactitude de son dossier. Nul = non enregistré (dépôt
-- antérieur, ou client qui ne l'envoie pas encore).
ALTER TABLE public.candidatures
    ADD COLUMN consentement_le TIMESTAMP;
