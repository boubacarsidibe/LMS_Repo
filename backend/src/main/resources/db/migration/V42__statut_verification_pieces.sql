-- Une pièce déposée n'est pas automatiquement non conforme : elle attend d'abord
-- le contrôle d'un agent. Le motif rend ensuite la demande de remplacement
-- compréhensible dans l'espace candidat.
ALTER TABLE pieces_justificatives
    ADD COLUMN statut_verification VARCHAR(20) NOT NULL DEFAULT 'A_VERIFIER',
    ADD COLUMN motif_non_conformite VARCHAR(500),
    ADD COLUMN date_verification TIMESTAMP;

-- Les anciennes lignes avaient seulement un booléen. Les pièces déjà validées le
-- restent ; les autres sont volontairement remises « à vérifier », car le schéma
-- antérieur ne permettait pas de savoir si elles avaient été refusées ou pas encore
-- examinées.
UPDATE pieces_justificatives
SET statut_verification = CASE
    WHEN est_conforme THEN 'CONFORME'
    ELSE 'A_VERIFIER'
END;
