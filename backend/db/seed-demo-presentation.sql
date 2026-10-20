-- =====================================================================
-- Données de présentation de la plateforme (19/10/2026)
-- =====================================================================
--
-- CE N'EST PAS UNE MIGRATION DE SCHÉMA. Comme les autres scripts de ce dossier, il ajoute du CONTENU et ne touche
-- à aucune structure de table ; il reste hors de db/migration pour pouvoir être rejoué après une remise à zéro.
--
-- OBJECTIF
-- Alimenter les vidéos de présentation (videos-presentation/) : un espace qui montre « Aucun résultat » ne présente
-- rien. Ce script fait vivre chaque espace avec des données plausibles et VARIÉES — des candidatures à tous les
-- stades du parcours, des étudiants aux résultats contrastés, des demandes d'accréditation, etc.
--
-- À EXÉCUTER APRÈS seed-comptes-test.sql, seed-demo-formateur.sql et seed-demo-client.sql :
--   docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-presentation.sql
--
-- CONVENTIONS
--  - Tout est résolu par clé naturelle (nom d'utilisateur, code, libellé), jamais par identifiant numérique : les
--    identifiants diffèrent d'une base à l'autre (c'est ce qui avait cassé seed-demo-formateur.sql).
--  - Chaque insertion est gardée par un WHERE NOT EXISTS : le script est idempotent.
--  - Les dates sont relatives à aujourd'hui : l'écran reste crédible le jour où l'on rejoue le script.
--  - Mot de passe de tous les comptes créés ici : Demo@2026.
--  - Les pièces justificatives pointent vers /app/uploads/demo/*.pdf : voir videos-presentation/preparer-donnees.sh,
--    qui y dépose les fichiers (sans eux, le téléchargement d'une pièce répond 404).
-- =====================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Réparation : une première version de ce script écrivait le statut « NON_CONFORME », qui n'existe pas dans
-- EnumStatutVerificationPiece (A_VERIFIER, CONFORME, A_REMPLACER). Une seule ligne de ce genre suffit à faire échouer
-- TOUTE la liste des candidatures (HTTP 400 « No enum constant ») : le serveur ne sait pas lire la ligne.
UPDATE pieces_justificatives SET statut_verification = 'A_REMPLACER' WHERE statut_verification = 'NON_CONFORME';

-- Les trois formations « DEMO-* » portent les cours de démonstration (DS-101, FIN-201, PROC-301), mais leur fiche est un
-- texte provisoire (« Programme de démonstration… ») sans image : elles n'ont rien à faire dans la vitrine publique.
-- Elles restent utilisables en interne (promotions, cours), seule leur publication est retirée.
UPDATE formation SET publiee = false WHERE code IN ('DEMO-DS', 'DEMO-FIN', 'DEMO-PROC');

-- =====================================================================
-- A. ADMISSION — une session ouverte et seize dossiers à tous les stades
-- =====================================================================

-- A.1 Session ouverte aujourd'hui. Les sessions du jeu de base ont des dates figées en 2026 : à la date du jour
--     aucune n'est « active », et le parcours de dépôt d'un candidat est impossible.
INSERT INTO sessions_candidature (libelle, description, date_ouverture, date_fermeture, est_ouverte, status,
                                  program_id, quota_max, seuil_reussite_test, montant_frais_dossier,
                                  info_frais_scolarite, info_carte_etudiante, info_rentree, criteres_actifs)
SELECT 'Session Hiver 2026-2027 — Executive MBA',
       'Admission à l''Executive MBA Pétrole & Gaz — rentrée de janvier 2027.',
       current_date - 30, current_date + 40, true, 'OUVERTE',
       (SELECT id FROM programs WHERE title = 'Executive MBA Petrole & Gaz'),
       60, 10, 25000,
       'Frais de scolarité : 1 800 000 FCFA, payables en trois tranches.',
       'Carte d''étudiant délivrée à l''inscription, sur présentation d''une pièce d''identité.',
       'Rentrée le 11 janvier 2027 à 9 h, Immeuble Adja Koone, Dakar.',
       false
WHERE NOT EXISTS (SELECT 1 FROM sessions_candidature WHERE libelle = 'Session Hiver 2026-2027 — Executive MBA');

-- A.2 Les candidats : un compte + un profil par personne.
CREATE TEMP TABLE _cand (username text, email text, nom text, prenom text, tel text, naissance date,
                         nationalite text, niveau text, genre text, profession text, experience int,
                         statut text, jours int, motif text, rang int) ON COMMIT DROP;
INSERT INTO _cand VALUES
 ('aminata_sarr',     'aminata.sarr@inpg-demo.sn',     'Sarr',     'Aminata',    '+221771100001', '1996-03-14', 'Sénégalaise',  'Master 1 (M1)',  'F', 'Ingénieure géologue',       4, 'SOUMIS',               1, NULL, NULL),
 ('ibrahima_ndiaye',  'ibrahima.ndiaye@inpg-demo.sn',  'Ndiaye',   'Ibrahima',   '+221771100002', '1994-08-02', 'Sénégalaise',  'Licence (L3)',   'M', 'Technicien de maintenance', 6, 'SOUMIS',               2, NULL, NULL),
 ('khady_seck',       'khady.seck@inpg-demo.sn',       'Seck',     'Khady',      '+221771100003', '1992-12-21', 'Sénégalaise',  'Master 2 (M2)',  'F', 'Chargée HSE',               5, 'DOSSIER_VALIDE',       6, NULL, NULL),
 ('ousmane_ba',       'ousmane.ba@inpg-demo.sn',       'Ba',       'Ousmane',    '+221771100004', '1997-05-30', 'Mauritanienne','Master 1 (M1)',  'M', 'Ingénieur procédés',        3, 'PRESELECTIONNE',       8, NULL, NULL),
 ('mariama_diallo',   'mariama.diallo@inpg-demo.sn',   'Diallo',   'Mariama',    '+221771100005', '1999-01-17', 'Guinéenne',    'Licence (L3)',   'F', 'Analyste financière',       2, 'PAIEMENT_EN_ATTENTE',  9, NULL, NULL),
 ('cheikh_mbaye',     'cheikh.mbaye@inpg-demo.sn',     'Mbaye',    'Cheikh',     '+221771100006', '1990-07-09', 'Sénégalaise',  'Master 1 (M1)',  'M', 'Ingénieur forage',          8, 'PAIEMENT_CONFIRME',   12, NULL, NULL),
 ('fatoumata_sow',    'fatoumata.sow@inpg-demo.sn',    'Sow',      'Fatoumata',  '+221771100007', '1993-10-25', 'Malienne',     'Master 2 (M2)',  'F', 'Économiste',                6, 'CONVOQUE_TEST',       14, NULL, NULL),
 ('moussa_cisse',     'moussa.cisse@inpg-demo.sn',     'Cissé',    'Moussa',     '+221771100008', '1995-04-04', 'Ivoirienne',   'Licence (L3)',   'M', 'Géomètre',                  3, 'TEST_REUSSI',         18, NULL, NULL),
 ('awa_thiam',        'awa.thiam@inpg-demo.sn',        'Thiam',    'Awa',        '+221771100009', '2001-09-12', 'Sénégalaise',  'Baccalauréat',   'F', 'Assistante administrative', 1, 'TEST_ECHOUE',         18, NULL, NULL),
 ('abdoulaye_fall',   'abdoulaye.fall@inpg-demo.sn',   'Fall',     'Abdoulaye',  '+221771100010', '1991-02-28', 'Sénégalaise',  'Master 2 (M2)',  'M', 'Ingénieur réservoir',       7, 'CONVOQUE_ENTRETIEN',  22, NULL, NULL),
 ('ndeye_gueye',      'ndeye.gueye@inpg-demo.sn',      'Gueye',    'Ndèye',      '+221771100011', '1988-06-15', 'Sénégalaise',  'Master 2 (M2)',  'F', 'Directrice QHSE',          11, 'ENTRETIEN_EFFECTUE',  26, NULL, NULL),
 ('pape_samb',        'pape.samb@inpg-demo.sn',        'Samb',     'Pape',       '+221771100012', '1989-11-03', 'Sénégalaise',  'Licence (L3)',   'M', 'Opérateur de production',   9, 'ENTRETIEN_EFFECTUE',  26, NULL, NULL),
 ('rama_kane',        'rama.kane@inpg-demo.sn',        'Kane',     'Rama',       '+221771100013', '1995-08-19', 'Sénégalaise',  'Master 1 (M1)',  'F', 'Ingénieure électricité',    5, 'ADMIS',               28, NULL, NULL),
 ('seydou_camara',    'seydou.camara@inpg-demo.sn',    'Camara',   'Seydou',     '+221771100014', '1996-12-01', 'Guinéenne',    'Master 1 (M1)',  'M', 'Technicien instrumentiste', 4, 'LISTE_ATTENTE',       28, NULL, 1),
 ('mame_diarra_faye', 'mame.diarra.faye@inpg-demo.sn', 'Faye',     'Mame Diarra','+221771100015', '1998-03-23', 'Sénégalaise',  'Licence (L3)',   'F', 'Comptable',                 3, 'NON_ADMIS',          28, NULL, NULL),
 ('lamine_gomis',     'lamine.gomis@inpg-demo.sn',     'Gomis',    'Lamine',     '+221771100016', '2002-02-10', 'Sénégalaise',  'Baccalauréat',   'M', 'Commerçant',                0, 'DOSSIER_REJETE',      20,
  'Pièces justificatives incomplètes : le relevé de notes est illisible et le diplôme n''est pas joint.', NULL);

INSERT INTO users (username, email, password, dtype, date_creation)
SELECT c.username, c.email, crypt('Demo@2026', gen_salt('bf', 10)), 'User', now() - ((c.jours + 3) || ' days')::interval
FROM _cand c
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = c.username OR u.email = c.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM _cand c JOIN users u ON u.username = c.username JOIN roles r ON r.code = 'ROLE_CANDIDAT'
ON CONFLICT DO NOTHING;

INSERT INTO candidats (id_user, nom, prenom, telephone, nationalite, date_naissance, niveau_etude, genre, profession, annees_experience)
SELECT u.id, c.nom, c.prenom, c.tel, c.nationalite, c.naissance, c.niveau, c.genre, c.profession, c.experience
FROM _cand c JOIN users u ON u.username = c.username
WHERE NOT EXISTS (SELECT 1 FROM candidats x WHERE x.id_user = u.id);

-- A.3 Les dossiers.
INSERT INTO candidatures (numero_dossier, statut, date_soumission, candidat_id, session_candidature_id,
                          motif_rejet, rang_liste_attente)
SELECT 'INPG-' || upper(substr(md5(c.username), 1, 8)), c.statut, now() - (c.jours || ' days')::interval, u.id,
       (SELECT id FROM sessions_candidature WHERE libelle = 'Session Hiver 2026-2027 — Executive MBA'),
       c.motif, c.rang
FROM _cand c JOIN users u ON u.username = c.username
WHERE NOT EXISTS (SELECT 1 FROM candidatures k WHERE k.candidat_id = u.id);

-- Le dossier du jeu de comptes de test portait déjà des notes de test et un entretien (seed-demo-client.sql) mais
-- était resté « SOUMIS » : un statut qui contredit ses données. On le met à la hauteur de ce qu'il contient.
UPDATE candidatures SET statut = 'ENTRETIEN_EFFECTUE'
WHERE numero_dossier = 'INPG-TEST0002' AND statut = 'SOUMIS'
  AND EXISTS (SELECT 1 FROM entretiens e WHERE e.candidat_id = candidatures.candidat_id AND e.note_entretien IS NOT NULL);

-- A.4 Pièces justificatives. La table impose UN fichier par pièce (fichier_stocke_id est unique) : on crée donc une
--     ligne fichiers_stockes par pièce et par dossier, toutes pointant vers l'un des cinq PDF de démonstration.
CREATE TEMP TABLE _pieces (type text, chemin text, description text, etiquette text) ON COMMIT DROP;
INSERT INTO _pieces VALUES
    ('PIECE_IDENTITE',    '/app/uploads/demo/piece-identite.pdf',    'Carte nationale d''identité',        'CNI'),
    ('CV',                '/app/uploads/demo/cv.pdf',                'Curriculum vitae à jour',            'CV'),
    ('DIPLOME',           '/app/uploads/demo/diplome.pdf',           'Dernier diplôme obtenu',             'Diplome'),
    ('RELEVE_NOTES',      '/app/uploads/demo/releve-notes.pdf',      'Relevé de notes du dernier diplôme', 'Releve-de-notes'),
    ('LETTRE_MOTIVATION', '/app/uploads/demo/lettre-motivation.pdf', 'Lettre de motivation',               'Lettre-de-motivation');

CREATE TEMP VIEW _dossier_piece AS
SELECT k.id AS candidature_id, k.statut, k.date_soumission, p.type, p.chemin, p.description,
       p.etiquette || '-' || replace(c.prenom || '-' || c.nom, ' ', '-') || '.pdf' AS nom_fichier
FROM candidatures k
JOIN users u ON u.id = k.candidat_id
JOIN _cand c ON c.username = u.username
CROSS JOIN _pieces p;

INSERT INTO fichiers_stockes (chemin_stockage, date_upload, mime_type, nom_original, taille_octets)
SELECT d.chemin, d.date_soumission, 'application/pdf', d.nom_fichier, 22000
FROM _dossier_piece d
WHERE NOT EXISTS (SELECT 1 FROM fichiers_stockes f WHERE f.nom_original = d.nom_fichier);

INSERT INTO pieces_justificatives (est_conforme, type_document, candidature_id, fichier_stocke_id, moyenne_declaree,
                                   description, statut_verification, motif_non_conformite, date_verification)
SELECT (verif.statut = 'CONFORME'),
       d.type, d.candidature_id, f.id,
       CASE WHEN d.type = 'RELEVE_NOTES' THEN round((11 + (d.candidature_id % 7) * 1.1)::numeric, 1)::double precision END,
       d.description,
       verif.statut, verif.motif,
       CASE WHEN d.statut <> 'SOUMIS' THEN d.date_soumission + interval '1 day' END
FROM _dossier_piece d
JOIN fichiers_stockes f ON f.nom_original = d.nom_fichier
CROSS JOIN LATERAL (SELECT
    CASE WHEN d.statut = 'SOUMIS' THEN 'A_VERIFIER'
         WHEN d.statut = 'DOSSIER_REJETE' AND d.type IN ('RELEVE_NOTES', 'DIPLOME') THEN 'A_REMPLACER'
         ELSE 'CONFORME' END AS statut,
    CASE WHEN d.statut = 'DOSSIER_REJETE' AND d.type = 'RELEVE_NOTES' THEN 'Relevé illisible.'
         WHEN d.statut = 'DOSSIER_REJETE' AND d.type = 'DIPLOME' THEN 'Diplôme non joint au dossier.' END AS motif
) AS verif
WHERE NOT EXISTS (SELECT 1 FROM pieces_justificatives x WHERE x.candidature_id = d.candidature_id AND x.type_document = d.type);

-- A.5 Paiements des frais de dossier (25 000 FCFA) : confirmés pour ceux qui ont dépassé cette étape,
--     en attente pour Mariama Diallo.
INSERT INTO paiements (date_paiement, devise, montant, reference_transaction, statut, candidature_id, methode_paiement)
SELECT k.date_soumission + interval '3 days', 'XOF', 25000,
       'PAY-' || upper(substr(md5(c.username || 'paiement'), 1, 10)),
       CASE WHEN c.statut = 'PAIEMENT_EN_ATTENTE' THEN 'EN_ATTENTE' ELSE 'SUCCES' END,
       k.id,
       CASE WHEN k.id % 2 = 0 THEN 'Wave' ELSE 'Orange Money' END
FROM _cand c
JOIN users u ON u.username = c.username
JOIN candidatures k ON k.candidat_id = u.id
WHERE c.statut IN ('PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME', 'CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE',
                   'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ADMIS', 'LISTE_ATTENTE', 'NON_ADMIS')
  AND NOT EXISTS (SELECT 1 FROM paiements p WHERE p.candidature_id = k.id);

-- A.6 Tests académiques : deux épreuves communes (même groupe), notes /20 saisies pour ceux qui les ont passées.
--     Le groupe est dérivé du nom de l'épreuve, comme dans seed-demo-client.sql : ils se rejoignent.
INSERT INTO tests_academiques (candidat_id, nom_epreuve, coefficient, note_obtenue, date_epreuve, groupe_epreuve_id, lieu_ou_lien)
SELECT u.id, e.epreuve, e.coef,
       CASE WHEN n.note1 IS NULL THEN NULL WHEN e.epreuve = 'Culture scientifique' THEN n.note1 ELSE n.note2 END,
       (current_date - 20) + time '09:00',
       md5('seed-demo-client:epreuve:' || e.epreuve)::uuid,
       'Immeuble Adja Koone — Salle B12'
FROM (VALUES
    ('fatoumata_sow', NULL::real, NULL::real),
    ('moussa_cisse',  13, 15),
    ('awa_thiam',      6, 7.5),
    ('abdoulaye_fall',14, 12.5),
    ('ndeye_gueye',   16, 15),
    ('pape_samb',     12, 13),
    ('rama_kane',     17, 16),
    ('seydou_camara', 11, 12),
    ('mame_diarra_faye', 9, 10)
) AS n(username, note1, note2)
JOIN users u ON u.username = n.username
CROSS JOIN (VALUES ('Culture scientifique', 1.0::real), ('Mathématiques appliquées', 2.0::real)) AS e(epreuve, coef)
WHERE NOT EXISTS (SELECT 1 FROM tests_academiques t WHERE t.candidat_id = u.id AND t.nom_epreuve = e.epreuve);

-- A.7 Entretiens : une session passée (notes saisies) et une à venir (Abdoulaye Fall, convoqué).
INSERT INTO entretiens (candidat_id, date_heure, lieu_ou_lien, note_entretien, appreciation_globale,
                        confirme_par_candidat, groupe_entretien_id)
SELECT u.id,
       (current_date + v.decalage) + time '10:00' + (v.rang * interval '30 minutes'),
       'Immeuble Adja Koone — Salle de jury',
       v.note, v.appreciation, true,
       md5('seed-demo-presentation:entretiens:' || v.decalage)::uuid
FROM (VALUES
    ('ndeye_gueye',   -8, 0, 17::real, 'Parcours remarquable, vision claire du métier de la QHSE. Très bonne expression orale.'),
    ('pape_samb',     -8, 1, 12::real, 'Solide expérience de terrain ; projet de reconversion à préciser.'),
    ('rama_kane',     -8, 2, 18::real, 'Candidate brillante, motivation et rigueur évidentes.'),
    ('seydou_camara', -8, 3, 11::real, 'Bon profil technique, manque d''expérience en encadrement.'),
    ('mame_diarra_faye', -8, 4, 8::real, 'Projet professionnel peu abouti ; connaissance du secteur insuffisante.'),
    ('abdoulaye_fall', 5, 0, NULL::real, NULL)
) AS v(username, decalage, rang, note, appreciation)
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM entretiens e WHERE e.candidat_id = u.id);

-- A.8 Journal du dossier (procédure qualité QMS-009) : réception pour tous, puis les étapes franchies.
INSERT INTO etapes_dossier (candidature_id, etape, statut_resultant, auteur_nom, date_etape, commentaire)
SELECT k.id, v.etape, v.resultat, v.auteur, k.date_soumission + (v.delai || ' days')::interval, v.commentaire
FROM candidatures k
JOIN users u ON u.id = k.candidat_id
JOIN _cand c ON c.username = u.username
JOIN LATERAL (VALUES
    ('RECEPTION',  'SOUMIS',         'Système',                   0, 'Dossier reçu.',                            1),
    ('EVALUATION', 'DOSSIER_VALIDE', 'Aminata Faye (admission)',  1, 'Dossier complet : validé.',                2),
    ('SELECTION',  c.statut,         'Aminata Faye (admission)',  9, 'Décision prise après délibération.',       3)
) AS v(etape, resultat, auteur, delai, commentaire, ordre) ON true
WHERE ((v.ordre = 1)
   OR (v.ordre = 2 AND c.statut NOT IN ('SOUMIS', 'DOSSIER_REJETE'))
   OR (v.ordre = 3 AND c.statut IN ('ADMIS', 'LISTE_ATTENTE', 'NON_ADMIS')))
  AND NOT EXISTS (SELECT 1 FROM etapes_dossier e WHERE e.candidature_id = k.id AND e.etape = v.etape);

-- =====================================================================
-- C. PROMOTION MPG-2026 — étudiants, supports, séances, notes, forum, annonces, avis
--    La promotion « Master Pétrole & Gaz » (cinq cours, un seul étudiant dans le jeu de base) est celle que
--    présentent les vidéos : espace étudiant, formateur et responsable pédagogique.
-- =====================================================================

-- C.1 Dix étudiants de plus (mot de passe Etudiant@2026, comme les autres comptes de démonstration).
--     date_creation dans le passé : l'isolation temporelle masque à un compte tout ce qui a été publié avant sa création.
CREATE TEMP TABLE _etu (username text, email text, nom text, prenom text, tel text, matricule text) ON COMMIT DROP;
INSERT INTO _etu VALUES
 ('mariama_fall',   'mariama.fall@inpg-demo.sn',   'Fall',   'Mariama',   '+221772000001', 'INPG-2026-0010'),
 ('ousmane_diop',   'ousmane.diop@inpg-demo.sn',   'Diop',   'Ousmane',   '+221772000002', 'INPG-2026-0011'),
 ('khadija_ndiaye', 'khadija.ndiaye@inpg-demo.sn', 'Ndiaye', 'Khadija',   '+221772000003', 'INPG-2026-0012'),
 ('moustapha_sy',   'moustapha.sy@inpg-demo.sn',   'Sy',     'Moustapha', '+221772000004', 'INPG-2026-0013'),
 ('astou_mbaye',    'astou.mbaye@inpg-demo.sn',    'Mbaye',  'Astou',     '+221772000005', 'INPG-2026-0014'),
 ('babacar_thiam',  'babacar.thiam@inpg-demo.sn',  'Thiam',  'Babacar',   '+221772000006', 'INPG-2026-0015'),
 ('coumba_sarr',    'coumba.sarr@inpg-demo.sn',    'Sarr',   'Coumba',    '+221772000007', 'INPG-2026-0016'),
 ('elhadji_diouf',  'elhadji.diouf@inpg-demo.sn',  'Diouf',  'Elhadji',   '+221772000008', 'INPG-2026-0017'),
 ('sokhna_ba',      'sokhna.ba@inpg-demo.sn',      'Ba',     'Sokhna',    '+221772000009', 'INPG-2026-0018'),
 ('daouda_cisse',   'daouda.cisse@inpg-demo.sn',   'Cissé',  'Daouda',    '+221772000010', 'INPG-2026-0019');

INSERT INTO users (username, email, password, dtype, date_creation)
SELECT e.username, e.email, crypt('Etudiant@2026', gen_salt('bf', 10)), 'User', TIMESTAMP '2026-01-10 08:00:00'
FROM _etu e WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = e.username OR u.email = e.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM _etu e JOIN users u ON u.username = e.username JOIN roles r ON r.code = 'ROLE_ETUDIANT'
ON CONFLICT DO NOTHING;

INSERT INTO etudiants (id_user, nom, prenom, telephone, matricule, promotion_id)
SELECT u.id, e.nom, e.prenom, e.tel, e.matricule, (SELECT id FROM promotion WHERE code = 'MPG-2026')
FROM _etu e JOIN users u ON u.username = e.username
WHERE NOT EXISTS (SELECT 1 FROM etudiants x WHERE x.id_user = u.id);

-- C.2 Supports de cours : un PDF (fichier déposé par preparer-donnees.sh) et un lien par cours.
INSERT INTO support_cours (titre, type_ressource, url, fichier_stocke, cours_id, date_publication)
SELECT v.titre, v.type, v.url, v.fichier, c.id, current_date - v.age
FROM (VALUES
    ('GEO-101', 'Introduction à la géologie des réservoirs', 'pdf',  '', 'demo-support-geo101-reservoirs.pdf',  30),
    ('GEO-101', 'Atlas du bassin sédimentaire sénégalais',   'lien', 'https://www.iogp.org/', NULL,            28),
    ('FOR-102', 'Techniques de forage — notions de base',    'pdf',  '', 'demo-support-for102-forage.pdf',      26),
    ('FOR-102', 'Référentiel des entrepreneurs de forage',   'lien', 'https://www.iadc.org/', NULL,            25),
    ('PRO-201', 'Production et traitement — du puits au terminal', 'pdf', '', 'demo-support-pro201-production.pdf', 21),
    ('PRO-201', 'Ressources de la Society of Petroleum Engineers', 'lien', 'https://www.spe.org/', NULL,        20),
    ('HSE-202', 'Sécurité et environnement — gestion des risques', 'pdf', '', 'demo-support-hse202-securite.pdf', 15),
    ('HSE-202', 'Bonnes pratiques de sécurité au travail',   'lien', 'https://www.ilo.org/', NULL,             14),
    ('ECO-203', 'Économie pétrolière — marchés et contenu local', 'pdf', '', 'demo-support-eco203-economie.pdf', 9),
    ('ECO-203', 'Rapports statistiques de l''OPEP',          'lien', 'https://www.opec.org/', NULL,             8)
) AS v(cours_nom, titre, type, url, fichier, age)
JOIN cours c ON c.nom = v.cours_nom
WHERE NOT EXISTS (SELECT 1 FROM support_cours s WHERE s.titre = v.titre AND s.cours_id = c.id);
-- L'adresse de téléchargement d'un PDF porte l'identifiant du support lui-même.
UPDATE support_cours SET url = '/api/espace-etudiant/supports/cours/' || id || '/download'
WHERE type_ressource = 'pdf' AND (url IS NULL OR url = '');

-- C.3 Séances à venir (le jeu de base ne contient que des séances passées) : cours, TD, TP et un examen.
INSERT INTO seance (titre, cours_id, date_heure, duree_minutes, lieu_ou_lien, type)
SELECT v.titre, c.id, date_trunc('day', now()) + (v.jours || ' days')::interval + v.heure::time, v.duree, v.lieu, v.type
FROM (VALUES
    ('GEO-101', 'Géologie des réservoirs — pétrophysique',         2,  '09:00', 120, 'Amphi A',           'cours'),
    ('FOR-102', 'Techniques de forage — fluides de forage',        3,  '14:00', 120, 'Salle B12',         'td'),
    ('PRO-201', 'Production et traitement — visite du terminal',   5,  '08:30', 240, 'Terminal de Sendou','tp'),
    ('HSE-202', 'Sécurité et environnement — exercice d''évacuation', 7, '10:00', 90, 'Cour du bâtiment',  'tp'),
    ('ECO-203', 'Économie pétrolière — contenu local',             9,  '09:00', 120, 'Amphi B',           'cours'),
    ('HSE-202', 'Sécurité et environnement — examen de mi-parcours', 14, '09:00', 120, 'Amphi A',        'examen')
) AS v(cours_nom, titre, jours, heure, duree, lieu, type)
JOIN cours c ON c.nom = v.cours_nom
WHERE NOT EXISTS (SELECT 1 FROM seance s WHERE s.titre = v.titre AND s.cours_id = c.id);

-- C.4 Présences aux séances déjà passées des cours de la promotion (environ quatre présents sur cinq).
INSERT INTO presence (present, etudiant_id, seance_id)
SELECT (e.id_user % 5) <> 0, e.id_user, s.id
FROM seance s
JOIN cours c ON c.id = s.cours_id AND c.nom IN ('GEO-101', 'FOR-102', 'PRO-201', 'HSE-202', 'ECO-203')
JOIN etudiants e ON e.promotion_id = (SELECT id FROM promotion WHERE code = 'MPG-2026')
WHERE s.date_heure < now()
ON CONFLICT (seance_id, etudiant_id) DO NOTHING;

-- C.5 Notes de module sur 100 : deux par étudiant et par cours. Chaque étudiant a son niveau propre (écart de -20 à +20
--     points autour de la moyenne) : sans cela, les moyennes de dix notes pseudo-aléatoires convergent toutes vers 60 et
--     l'écran de suivi montre une promotion uniformément « à la limite », ce qui ne ressemble à aucune promotion réelle.
--     La table temporaire est la définition de référence : l'INSERT crée les notes manquantes, l'UPDATE remet à niveau
--     celles d'une exécution précédente (le script reste rejouable et converge vers les mêmes valeurs).
CREATE TEMP TABLE _notes_cible ON COMMIT DROP AS
SELECT m.module, c.id AS cours_id, e.id_user AS etudiant_id, m.age,
       LEAST(98, GREATEST(20, 30 + ((e.id_user * 7 + m.rang * 5 + c.id * 3) % 13) * 5
                              + ((e.id_user * 11) % 9 - 4) * 5))::double precision AS note
FROM (VALUES
    ('GEO-101', 'Stratigraphie',            1, 22), ('GEO-101', 'Pétrophysique',          2, 6),
    ('FOR-102', 'Fluides de forage',        1, 20), ('FOR-102', 'Programme de tubage',    2, 5),
    ('PRO-201', 'Séparation des phases',    1, 16), ('PRO-201', 'Traitement du gaz',      2, 4),
    ('HSE-202', 'Gestion des risques',      1, 11), ('HSE-202', 'Plan d''urgence',        2, 3),
    ('ECO-203', 'Marchés pétroliers',       1, 7),  ('ECO-203', 'Contenu local',          2, 2)
) AS m(cours_nom, module, rang, age)
JOIN cours c ON c.nom = m.cours_nom
JOIN etudiants e ON e.promotion_id = (SELECT id FROM promotion WHERE code = 'MPG-2026');

INSERT INTO note_module (commentaire, date_evaluation, formateur_nom, module, note, cours_id, etudiant_id)
SELECT CASE
           WHEN t.note >= 75 THEN 'Très bonne maîtrise des notions, travail rigoureux.'
           WHEN t.note >= 60 THEN 'Résultat solide, à consolider sur la partie pratique.'
           WHEN t.note >= 50 THEN 'Niveau correct ; peut mieux faire avec un peu plus de régularité.'
           ELSE 'Des lacunes à combler : un point individuel est conseillé.'
       END,
       (current_date - t.age)::date, 'Moussa Ndiaye', t.module, t.note, t.cours_id, t.etudiant_id
FROM _notes_cible t
WHERE NOT EXISTS (
    SELECT 1 FROM note_module n WHERE n.etudiant_id = t.etudiant_id AND n.module = t.module AND n.cours_id = t.cours_id
);

UPDATE note_module n
SET note = t.note,
    commentaire = CASE
           WHEN t.note >= 75 THEN 'Très bonne maîtrise des notions, travail rigoureux.'
           WHEN t.note >= 60 THEN 'Résultat solide, à consolider sur la partie pratique.'
           WHEN t.note >= 50 THEN 'Niveau correct ; peut mieux faire avec un peu plus de régularité.'
           ELSE 'Des lacunes à combler : un point individuel est conseillé.'
       END
FROM _notes_cible t
WHERE n.etudiant_id = t.etudiant_id AND n.module = t.module AND n.cours_id = t.cours_id
  AND n.formateur_nom = 'Moussa Ndiaye' AND n.note <> t.note;

-- C.6 Supports consultés : une progression variée, du plus avancé au plus lent.
INSERT INTO progressions_support (etudiant_id, support_id, date_consultation)
SELECT e.id_user, s.id, now() - ((s.id + e.id_user) % 14 || ' days')::interval
FROM etudiants e
JOIN support_cours s ON s.cours_id IN (SELECT id FROM cours WHERE nom IN ('GEO-101', 'FOR-102', 'PRO-201', 'HSE-202', 'ECO-203'))
WHERE e.promotion_id = (SELECT id FROM promotion WHERE code = 'MPG-2026')
  AND ((e.id_user * 3 + s.id) % 4) <> 0
  AND NOT EXISTS (SELECT 1 FROM progressions_support p WHERE p.etudiant_id = e.id_user AND p.support_id = s.id);

-- C.7 Forum : cinq fils sur les cours de la promotion — un épinglé, un résolu (clos), des réponses de l'équipe et des pairs.
INSERT INTO forum_sujets (titre, cours_id, auteur_id, date_creation, epingle, clos, portee)
SELECT v.titre, c.id, u.id, now() - (v.jours || ' days')::interval, v.epingle, v.clos, 'ETUDIANT'
FROM (VALUES
    ('Calendrier des visites de terrain',                  'PRO-201', 'prof_demo',      12, true,  false),
    ('Différence entre porosité et perméabilité ?',        'GEO-101', 'mariama_fall',   10, false, true),
    ('Quelle norme pour le plan d''urgence ?',             'HSE-202', 'moustapha_sy',    6, false, false),
    ('Documentation sur le contenu local au Sénégal',      'ECO-203', 'astou_mbaye',     4, false, false),
    ('Choix du fluide de forage pour un puits profond',    'FOR-102', 'babacar_thiam',   3, false, false)
) AS v(titre, cours_nom, username, jours, epingle, clos)
JOIN cours c ON c.nom = v.cours_nom
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM forum_sujets s WHERE s.titre = v.titre);

INSERT INTO forum_messages (sujet_id, auteur_id, contenu, date_creation)
SELECT s.id, u.id, v.contenu, s.date_creation + (v.heures || ' hours')::interval
FROM forum_sujets s
JOIN (VALUES
    ('Calendrier des visites de terrain', 'prof_demo', 0, 'La visite du terminal de Sendou aura lieu le jeudi à 8 h 30. Prévoyez vos équipements de protection : casque, chaussures de sécurité et gilet.'),
    ('Calendrier des visites de terrain', 'sokhna_ba', 3, 'Le transport depuis l''institut est-il prévu ?'),
    ('Calendrier des visites de terrain', 'prof_demo', 5, 'Oui, un car partira de l''institut à 7 h 45.'),
    ('Différence entre porosité et perméabilité ?', 'mariama_fall', 0, 'Je confonds toujours les deux notions. Quelqu''un peut m''expliquer simplement ?'),
    ('Différence entre porosité et perméabilité ?', 'prof_demo', 4, 'La porosité mesure le volume de vide de la roche ; la perméabilité, la facilité avec laquelle un fluide y circule. Une roche peut être très poreuse mais peu perméable.'),
    ('Différence entre porosité et perméabilité ?', 'mariama_fall', 9, 'C''est clair, merci beaucoup !'),
    ('Quelle norme pour le plan d''urgence ?', 'moustapha_sy', 0, 'Pour le TP de sécurité, quelle norme doit-on citer dans notre plan d''urgence ?'),
    ('Quelle norme pour le plan d''urgence ?', 'coumba_sarr', 6, 'Le support du cours renvoie à la gestion des risques ; je pense qu''on peut s''en inspirer.'),
    ('Documentation sur le contenu local au Sénégal', 'astou_mbaye', 0, 'Connaissez-vous des ressources officielles sur le contenu local dans la filière pétrole et gaz ?'),
    ('Choix du fluide de forage pour un puits profond', 'babacar_thiam', 0, 'Pour un puits profond à haute température, boue à l''eau ou boue à l''huile ?')
) AS v(sujet, username, heures, contenu) ON v.sujet = s.titre
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM forum_messages m WHERE m.sujet_id = s.id AND m.contenu = v.contenu);

-- C.8 Communications de l'institut à la promotion.
INSERT INTO annonces (titre, contenu, type, epinglee, date_publication, auteur_id, promotion_id)
SELECT v.titre, v.contenu, v.type, v.epinglee, now() - (v.jours || ' days')::interval,
       (SELECT id FROM users WHERE username = 'admin_admission'), (SELECT id FROM promotion WHERE code = 'MPG-2026')
FROM (VALUES
    ('Visite du terminal de Sendou', 'Une visite technique du terminal pétrolier est organisée pour toute la promotion. Départ de l''institut à 7 h 45 ; équipements de protection obligatoires.', 'ANNONCE', true, 3),
    ('Examens de mi-parcours', 'Les examens de mi-parcours se dérouleront dans deux semaines. Le calendrier détaillé est disponible dans votre espace.', 'ANNONCE', false, 5),
    ('Bibliothèque : nouveaux ouvrages', 'Plus de cinquante nouveaux ouvrages consacrés au forage et à la production sont disponibles à la bibliothèque.', 'MESSAGE', false, 8)
) AS v(titre, contenu, type, epinglee, jours)
WHERE NOT EXISTS (SELECT 1 FROM annonces a WHERE a.titre = v.titre);

-- C.9 Avis des étudiants sur les cours (suivi qualité) : de très bons, quelques-uns à surveiller.
INSERT INTO retour_evaluation (commentaire, date_creation, epingle, note, statut, cours_id, etudiant_id)
SELECT v.commentaire, now() - (v.jours || ' days')::interval, false, v.note,
       CASE WHEN v.note <= 3 THEN 'ACTION_REQUISE' ELSE 'VALIDE' END, c.id, u.id
FROM (VALUES
    ('GEO-101', 'mariama_fall',   5, 'Cours très clair, les schémas de réservoir aident beaucoup.', 9),
    ('GEO-101', 'ousmane_diop',   4, 'Bon contenu ; j''aurais aimé davantage d''études de cas.', 9),
    ('FOR-102', 'khadija_ndiaye', 5, 'Les démonstrations sur les fluides de forage sont excellentes.', 8),
    ('FOR-102', 'moustapha_sy',   3, 'Rythme trop rapide sur le programme de tubage.', 8),
    ('PRO-201', 'astou_mbaye',    4, 'La visite du terminal rend le cours très concret.', 6),
    ('PRO-201', 'babacar_thiam',  5, 'Un des meilleurs cours du semestre.', 6),
    ('HSE-202', 'coumba_sarr',    2, 'Trop de théorie, pas assez de mises en situation.', 5),
    ('HSE-202', 'elhadji_diouf',  3, 'Intéressant mais le support est difficile à suivre.', 5),
    ('ECO-203', 'sokhna_ba',      4, 'Très utile pour comprendre le contexte du secteur.', 3),
    ('ECO-203', 'daouda_cisse',   5, 'Intervenant passionnant, exemples actuels.', 3)
) AS v(cours_nom, username, note, commentaire, jours)
JOIN cours c ON c.nom = v.cours_nom
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM retour_evaluation r WHERE r.cours_id = c.id AND r.etudiant_id = u.id);

COMMIT;

-- =====================================================================
-- CONTRÔLE
--   SELECT statut, count(*) FROM candidatures GROUP BY statut ORDER BY 2 DESC;
-- =====================================================================
