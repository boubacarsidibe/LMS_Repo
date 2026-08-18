-- Coefficient d'un cours dans la maquette pedagogique.
--
-- La moyenne generale d'un etudiant etait calculee sur la liste a plat de toutes
-- ses notes, sans regroupement par cours. Le poids de chaque cours dependait donc
-- du NOMBRE de devoirs que son formateur avait saisis : un cours evalue trois fois
-- pesait trois fois plus qu'un cours evalue une seule fois. Personne n'avait choisi
-- cette ponderation, elle resultait d'une habitude de saisie.
--
-- Le coefficient est une valeur propre au cours, fixee par la maquette. Il ne se
-- deduit ni du volume horaire ni du nombre d'evaluations.
--
-- Valeur par defaut 1 : les cours existants gardent un poids egal entre eux, ce qui
-- reproduit exactement le comportement attendu tant qu'aucun coefficient n'est saisi.
-- NOT NULL evite d'avoir a traiter le cas nul dans le calcul de la moyenne.

ALTER TABLE public.cours
    ADD COLUMN coefficient DOUBLE PRECISION NOT NULL DEFAULT 1;

-- Un coefficient nul ou negatif viderait ou inverserait la moyenne ponderee.
ALTER TABLE public.cours
    ADD CONSTRAINT cours_coefficient_positif CHECK (coefficient > 0);
