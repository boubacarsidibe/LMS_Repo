-- Une situation « en recherche d'emploi », « poursuite d'études » ou « autre »
-- ne désigne aucun poste occupé : imposer un intitulé et une entreprise forçait
-- l'alumni à en inventer un pour pouvoir simplement consigner cette situation.
-- Seuls EN_EMPLOI et ENTREPRENARIAT continuent de les exiger, côté application
-- (AlumniController#validerSituation).
ALTER TABLE situations_professionnelles ALTER COLUMN intitule_poste DROP NOT NULL;
ALTER TABLE situations_professionnelles ALTER COLUMN entreprise DROP NOT NULL;
