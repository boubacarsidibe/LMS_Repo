-- Aligne les statuts historiques ADMIS1/2/3 sur les etapes metier validees.
ALTER TABLE candidatures DROP CONSTRAINT IF EXISTS candidatures_statut_check;

UPDATE candidatures SET statut = CASE statut
    WHEN 'EN_COURS_DE_PRESELECTION' THEN 'PRESELECTIONNE'
    WHEN 'ADMIS1' THEN 'CONVOQUE_TEST'
    WHEN 'ADMIS2' THEN 'CONVOQUE_ENTRETIEN'
    WHEN 'ADMIS3' THEN 'ADMIS'
    WHEN 'REJETE' THEN CASE
        WHEN statut_precedent IN ('SOUMIS', 'EN_COURS_DE_PRESELECTION') THEN 'DOSSIER_REJETE'
        ELSE 'NON_ADMIS'
    END
    ELSE statut
END;

UPDATE candidatures SET statut_precedent = CASE statut_precedent
    WHEN 'EN_COURS_DE_PRESELECTION' THEN 'PRESELECTIONNE'
    WHEN 'ADMIS1' THEN 'CONVOQUE_TEST'
    WHEN 'ADMIS2' THEN 'CONVOQUE_ENTRETIEN'
    WHEN 'ADMIS3' THEN 'ADMIS'
    WHEN 'REJETE' THEN 'NON_ADMIS'
    ELSE statut_precedent
END;

UPDATE etapes_dossier SET statut_resultant = CASE statut_resultant
    WHEN 'EN_COURS_DE_PRESELECTION' THEN 'PRESELECTIONNE'
    WHEN 'ADMIS1' THEN 'CONVOQUE_TEST'
    WHEN 'ADMIS2' THEN 'CONVOQUE_ENTRETIEN'
    WHEN 'ADMIS3' THEN 'ADMIS'
    WHEN 'REJETE' THEN 'NON_ADMIS'
    ELSE statut_resultant
END;

ALTER TABLE candidatures ADD CONSTRAINT candidatures_statut_check CHECK (statut IN (
    'BROUILLON', 'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE', 'DOSSIER_REJETE',
    'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME',
    'CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST',
    'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN',
    'ADMIS', 'LISTE_ATTENTE', 'NON_ADMIS', 'INSCRIPTION_FINALISEE', 'DESISTEMENT'
));

ALTER TABLE candidatures ADD COLUMN IF NOT EXISTS rang_liste_attente INTEGER;
ALTER TABLE candidatures ADD CONSTRAINT candidatures_rang_liste_attente_check
    CHECK (rang_liste_attente IS NULL OR rang_liste_attente > 0);
CREATE UNIQUE INDEX IF NOT EXISTS uq_candidature_rang_attente_session
    ON candidatures (session_candidature_id, rang_liste_attente)
    WHERE rang_liste_attente IS NOT NULL;

CREATE TABLE IF NOT EXISTS session_candidature_programmes (
    session_candidature_id BIGINT NOT NULL REFERENCES sessions_candidature(id) ON DELETE CASCADE,
    program_id BIGINT NOT NULL REFERENCES programs(id),
    PRIMARY KEY (session_candidature_id, program_id)
);
INSERT INTO session_candidature_programmes (session_candidature_id, program_id)
SELECT id, program_id FROM sessions_candidature WHERE program_id IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE sessions_candidature DROP CONSTRAINT IF EXISTS sessions_candidature_status_check;
UPDATE sessions_candidature SET status = CASE status
    WHEN 'OUVERT' THEN 'OUVERTE'
    WHEN 'SUSPENDU' THEN 'SUSPENDUE'
    WHEN 'CLOTURE' THEN 'CLOTUREE'
    ELSE status
END;
ALTER TABLE sessions_candidature ADD CONSTRAINT sessions_candidature_status_check
    CHECK (status IN ('BROUILLON', 'OUVERTE', 'SUSPENDUE', 'CLOTUREE', 'TERMINEE'));
