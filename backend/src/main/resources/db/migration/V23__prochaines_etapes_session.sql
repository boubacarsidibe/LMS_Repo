-- Informations affichées au candidat admis sur l'écran "Prochaines étapes"
-- (délai de paiement des frais, disponibilité de la carte étudiante, date de
-- rentrée). Texte libre, saisi par le responsable admission à la configuration
-- de chaque session : ces informations varient d'une session à l'autre et
-- n'avaient jamais été stockées — elles étaient codées en dur dans le frontend
-- ("avant le 15 décembre", "en janvier"...), invariables et parfois fausses.
ALTER TABLE sessions_candidature
    ADD COLUMN info_frais_scolarite VARCHAR(255),
    ADD COLUMN info_carte_etudiante VARCHAR(255),
    ADD COLUMN info_rentree VARCHAR(255);
