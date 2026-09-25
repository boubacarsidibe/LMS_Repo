-- Certificat de formation au modèle officiel de l'INPG.
--
-- Le modèle porte, en plus de l'étudiant et de l'intitulé déjà stockés : la civilité et la date de
-- naissance du bénéficiaire, la période de la formation (« Du 02 au 06 février 2026 ») et un code
-- propre à chaque certificat (ex. CG1001260206/03). Ces valeurs sont figées à la délivrance, comme le
-- reste du certificat.
--
-- Colonnes nullables : les certificats délivrés avant cette migration n'ont ni période ni code ; leur
-- PDF omet simplement ces lignes et leur signature électronique reste celle calculée à l'époque.

ALTER TABLE public.certificat
    ADD COLUMN civilite         VARCHAR(20),
    ADD COLUMN date_naissance   DATE,
    ADD COLUMN date_debut       DATE,
    ADD COLUMN date_fin         DATE,
    ADD COLUMN code_certificat  VARCHAR(60);
