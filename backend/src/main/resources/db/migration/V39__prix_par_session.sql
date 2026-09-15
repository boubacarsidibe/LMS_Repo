-- Frais de dossier propres à chaque session de candidature (fixés par le responsable
-- admission, "par formation" au sens métier). NULL = pas de tarif spécifique défini,
-- le tarif global (app.recrutement.frais-dossier) s'applique alors en repli.
ALTER TABLE sessions_candidature ADD COLUMN montant_frais_dossier NUMERIC(38, 2);
