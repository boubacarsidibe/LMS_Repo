-- Les rôles sont des référentiels applicatifs, pas des données de démonstration.
-- Ils restent disponibles quand les initialiseurs de démo sont désactivés.
INSERT INTO roles (code, libelle, description) VALUES
    ('ROLE_CANDIDAT', 'Candidat', 'Étudiant postulant — soumet et suit sa candidature'),
    ('ROLE_RESPONSABLE_ADMISSION', 'Responsable Admission', 'Staff administratif — gère les candidatures, sessions, entretiens'),
    ('ROLE_ETUDIANT', 'Étudiant', 'Étudiant admis — accès à l''espace étudiant'),
    ('ROLE_FORMATEUR', 'Formateur', 'Enseignant — gère ses cours et ses supports pédagogiques'),
    ('ROLE_RESPONSABLE_PEDAGOGIQUE', 'Responsable Pédagogique', 'Staff pédagogique — pilote l''offre de formation'),
    ('ROLE_ALUMNI', 'Alumni', 'Ancien étudiant — accès à son profil et ses relevés délivrés'),
    ('ROLE_EVALUATEUR_ACCREDITATION', 'Évaluateur Accréditation', 'Staff — évalue les demandes d''accréditation'),
    ('ROLE_ECOLE_FORMATION', 'École de Formation', 'École externe — soumet une demande d''accréditation'),
    ('ROLE_ENTREPRISE', 'Entreprise', 'Entreprise partenaire — soumet des besoins de formation')
ON CONFLICT (code) DO NOTHING;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS session_version BIGINT NOT NULL DEFAULT 0;
