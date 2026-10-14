-- Les trois photos de démonstration créées par DataInitializer étaient enregistrées sans leur sous-dossier : le
-- fichier existe dans <uploads>/photos/, la ligne pointait vers <uploads>/ — 404 à chaque affichage de la photothèque
-- (audit du 2026-10-14). Ciblé sur ces trois noms, sans effet ailleurs ni sur une base qui ne les contient pas.
UPDATE public.photos
SET fichier_stocke = 'photos/' || fichier_stocke
WHERE fichier_stocke IN ('demo-photo-ds.png', 'demo-photo-fin.png', 'demo-photo-proc.png');
