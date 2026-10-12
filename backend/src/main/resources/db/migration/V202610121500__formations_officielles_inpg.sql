-- Formations officielles de l'INPG, d'après le questionnaire de collecte de données rempli par le personnel
-- de l'établissement (2026-09-30). Voir docs/COLLECTE_DONNEES_INPG_2026-10-12.md pour ce qui est connu et ce qui
-- reste à demander (contenu pédagogique, tarifs, dates de rentrée, maquettes, coefficients).
--
-- N'est consigné ici que ce que l'INPG a communiqué : intitulé, type de diplôme, niveau d'accès, durée. Aucun
-- contenu pédagogique n'est inventé ; les champs qu'il alimenterait (fiche détaillée, image, crédits, volume
-- horaire) restent vides jusqu'à réception des informations.
--
-- Le code sert de clé : rejouable, et jamais d'écrasement d'une formation déjà présente.

INSERT INTO public.formation (code, intitule, description, domaine, niveau, duree, publiee)
VALUES
    ('MIPG',
     'Mastère en Ingénierie Pétrolier et Gazier',
     'Mastère en ingénierie pétrolière et gazière. Niveau d''accès : Bac+5. Durée : 18 mois.',
     'Ingénierie', 'Mastère', '18 mois', TRUE),
    ('LIC-MAINT-ELEC',
     'Licence en Maintenance électrique des installations pétrolière et gazière',
     'Licence en maintenance électrique des installations pétrolières et gazières. Niveau d''accès : DUT, BTS ou Licence 3 (Bac+2 / Bac+3). Durée : 12 mois.',
     'Technique', 'Licence', '12 mois', TRUE),
    ('LIC-MAINT-MECA',
     'Licence en Maintenance mécanique des installations pétrolière et gazière',
     'Licence en maintenance mécanique des installations pétrolières et gazières. Niveau d''accès : DUT, BTS ou Licence 3 (Bac+2 / Bac+3). Durée : 12 mois.',
     'Technique', 'Licence', '12 mois', TRUE),
    ('CPS-MAINT-ELEC',
     'CPS en Maintenance électrique des installations pétrolière et gazière',
     'CPS en maintenance électrique des installations pétrolières et gazières. Niveau d''accès : 4e secondaire. Durée : 6 mois.',
     'Technique', 'CPS', '6 mois', TRUE),
    ('CPS-MAINT-MECA',
     'CPS en Maintenance mécanique des installations pétrolière et gazière',
     'CPS en maintenance mécanique des installations pétrolières et gazières. Niveau d''accès : 4e secondaire. Durée : 6 mois.',
     'Technique', 'CPS', '6 mois', TRUE),
    ('CPS-QHSE',
     'CPS en Qualité Hygiène Sécurité Environnement',
     'CPS en qualité, hygiène, sécurité et environnement. Niveau d''accès : 4e secondaire. Durée : 6 mois.',
     'Sécurité', 'CPS', '6 mois', TRUE),
    ('CPS-SOUDAGE',
     'CPS en Soudage industriel',
     'CPS en soudage industriel. Niveau d''accès : 4e secondaire. Durée : 6 mois.',
     'Technique', 'CPS', '6 mois', TRUE)
ON CONFLICT (code) DO NOTHING;

-- Le MBA existait déjà (catalogue du site) avec « Executive MBA » et « 18-24 Mois » : l'INPG indique « MBA », Bac+5,
-- 18 mois. Corrigé seulement si la fiche porte encore les anciennes valeurs, pour ne jamais écraser une retouche faite
-- depuis le back-office entre-temps.
UPDATE public.formation
   SET niveau = 'MBA',
       duree  = '18 mois'
 WHERE code = 'MBA-ECO-MAN-01'
   AND niveau = 'Executive MBA'
   AND duree  = '18-24 Mois';
