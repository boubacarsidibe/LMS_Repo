-- Rangement du catalogue des formations, après réception des données officielles de l'INPG et vérification sur le
-- site de l'établissement (voir docs/COLLECTE_DONNEES_INPG_2026-10-12.md). Trois corrections, toutes gardées par la
-- valeur qu'elles remplacent : une retouche faite depuis le back-office entre-temps n'est jamais écrasée.

-- 1. Le Mastère existait deux fois. « Ingénierie du Pétrole et du Gaz » (fiche reprise du site : « 12 mois (cours) +
--    6 mois (stage) », pour des ingénieurs) est le Mastère en Ingénierie Pétrolier et Gazier de l'INPG, que
--    V202610121500 avait ajouté une seconde fois sous le code MIPG. On garde la fiche existante, la plus riche, et on la
--    met aux données officielles : 18 mois au total. Le code ING-PET-GAZ-02 reste : le chargeur du catalogue s'en sert
--    comme clé et recréerait la fiche s'il avait changé.
UPDATE public.formation
   SET intitule = 'Mastère en Ingénierie Pétrolier et Gazier',
       niveau   = 'Mastère',
       duree    = '18 mois'
 WHERE code = 'ING-PET-GAZ-02'
   AND niveau = 'Master 2'
   AND duree  = '12 mois';

--    La ligne MIPG, devenue doublon, est retirée seulement si rien ne s'y rattache.
DELETE FROM public.formation f
 WHERE f.code = 'MIPG'
   AND NOT EXISTS (SELECT 1 FROM public.demandes_entreprise x WHERE x.formation_id = f.id)
   AND NOT EXISTS (SELECT 1 FROM public.formation_cours x WHERE x.formation_id = f.id)
   AND NOT EXISTS (SELECT 1 FROM public.offres_recrutement_formateur x WHERE x.formation_id = f.id)
   AND NOT EXISTS (SELECT 1 FROM public.promotion x WHERE x.formation_id = f.id)
   AND NOT EXISTS (SELECT 1 FROM public.user_favorites x WHERE x.formation_id = f.id);

-- 2. Les formations de démonstration ne sont pas celles de l'INPG : retirées du catalogue public (et de la liste
--    proposée aux entreprises), sans toucher à leurs promotions, cours et étudiants, dont les démonstrations ont besoin.
--    Ces quatre codes ne figurent pas dans le chargeur du catalogue : rien ne les republiera au démarrage.
UPDATE public.formation
   SET publiee = FALSE
 WHERE code IN ('DEMO-DS', 'DEMO-FIN', 'DEMO-PROC', 'MPG')
   AND publiee = TRUE;

-- 3. Trois fiches annonçaient une « prochaine rentrée » de 2024, donc passée : une date fausse vaut moins qu'une
--    date absente. Valeurs retirées : GEO-ING-RES-06 « Octobre 2024 », HYG-SEC-TRA-09 « 15 Octobre 2024 »,
--    MBA-FIN-STR-16 « 15 Septembre 2024 ». L'INPG n'ayant pas communiqué les dates réelles, rien ne les remplace.
UPDATE public.formation
   SET fiche_detaillee = (fiche_detaillee::jsonb #- '{sidebar,prochaineRentree}')::text
 WHERE code IN ('GEO-ING-RES-06', 'HYG-SEC-TRA-09', 'MBA-FIN-STR-16')
   AND fiche_detaillee::jsonb #>> '{sidebar,prochaineRentree}' LIKE '%2024';
