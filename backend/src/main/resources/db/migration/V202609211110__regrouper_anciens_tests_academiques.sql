-- Les anciennes convocations étaient une ligne par candidat. Une même épreuve
-- est reconnue par ses informations de planification communes afin de reconstituer
-- sa feuille de notes sans mêler des épreuves différentes.
UPDATE tests_academiques
SET groupe_epreuve_id = md5(
    coalesce(nom_epreuve, '') || '|' ||
    coalesce(date_epreuve::text, '') || '|' ||
    coalesce(lieu_ou_lien, '') || '|' ||
    coalesce(coefficient::text, '')
)::uuid;
