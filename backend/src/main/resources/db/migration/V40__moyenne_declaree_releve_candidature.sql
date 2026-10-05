-- Le candidat declare desormais la moyenne obtenue pour chaque releve de notes
-- qu'il televerse (au plus 3, un par annee recente) : le PDF sert de preuve a
-- cette valeur, saisie a la main puisqu'aucun systeme ne peut l'extraire d'un
-- document scanne. Nullable : les pieces deja deposees, et tout type de document
-- autre qu'un releve, n'en portent pas.
ALTER TABLE public.pieces_justificatives ADD COLUMN moyenne_declaree DOUBLE PRECISION;
