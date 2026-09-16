-- RemiseActivite expose une Double nullable côté Java ; Hibernate attend donc
-- un DOUBLE PRECISION PostgreSQL. La définition NUMERIC de V45 empêchait la
-- validation du schéma et le démarrage de l'API.
ALTER TABLE remise_activite
    ALTER COLUMN note TYPE DOUBLE PRECISION
    USING note::DOUBLE PRECISION;
