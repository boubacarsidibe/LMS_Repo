-- Nature de la situation professionnelle (en emploi / en recherche / entreprenariat /
-- poursuite d'etudes / autre), distincte de date_fin IS NULL qui ne dit que "en cours",
-- pas ce qu'elle est. Sert la repartition affichee cote pedagogie.
ALTER TABLE situations_professionnelles
    ADD COLUMN statut_emploi VARCHAR(20) NOT NULL DEFAULT 'EN_EMPLOI';
