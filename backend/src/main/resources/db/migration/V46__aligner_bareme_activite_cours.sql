-- ActiviteCours emploie un double Java ; Hibernate attend donc un DOUBLE PRECISION
-- PostgreSQL. V45 créait cette colonne en NUMERIC(5,2), ce qui bloquait la
-- validation du schéma au démarrage avant même que l'application ne serve l'API.
ALTER TABLE activite_cours
    ALTER COLUMN bareme TYPE DOUBLE PRECISION
    USING bareme::DOUBLE PRECISION;
