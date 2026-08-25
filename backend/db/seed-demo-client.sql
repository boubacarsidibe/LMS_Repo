-- =====================================================================
-- Données de démonstration client (17/08/2026)
-- =====================================================================
--
-- CE N'EST PAS UNE MIGRATION DE SCHÉMA. Comme seed-comptes-test.sql et
-- seed-demo-formateur.sql, ce script ajoute du CONTENU et ne touche à
-- aucune structure de table. Il reste volontairement hors de
-- src/main/resources/db/migration : le mélanger à Flyway empêcherait de
-- le rejouer après une remise à zéro de la base.
--
-- OBJECTIF
-- Remplir les écrans qui sonnent creux pendant une démonstration : des
-- barres de progression à zéro, une photothèque sans photo, un forum à
-- deux sujets, un espace admission sans test ni entretien. Les valeurs
-- sont volontairement VARIÉES — un étudiant en avance, un à mi-parcours,
-- un qui démarre — pour que les écrans racontent quelque chose plutôt
-- que d'afficher partout le même chiffre.
--
-- CE QU'IL NE FAIT PAS
-- Il ne modifie ni ne supprime aucune donnée existante : il n'y a que des
-- INSERT. En particulier, le statut des candidatures déjà déposées n'est
-- pas touché — c'est à vous de le faire évoluer devant le client, c'est
-- justement la démonstration du parcours d'admission.
--
-- EXÉCUTION
--   docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-client.sql
--
-- Le script est IDEMPOTENT : chaque insertion est gardée par un
-- WHERE NOT EXISTS, donc le relancer dix fois ne crée aucun doublon.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Progression des étudiants (espace étudiant → tableau de bord)
--
-- Alimente les barres de progression, le compteur « supports lus » et le
-- bloc « Reprendre où vous en étiez ». Les dates sont échelonnées sur les
-- dernières semaines pour que l'ordre de reprise soit crédible.
--
-- Répartition voulue :
--   Bineta Ndour (47, compte de démonstration) ... 4 supports sur 6
--   Aissatou Ba (38) ............................. cours 9 terminé
--   Cheikh Fall (39) ............................. cours 9 à mi-parcours
--   Fatou Diagne (40) ............................ cours 10 terminé
--   Ibrahima Sow (41) ............................ cours 10 à mi-parcours
--   Mamadou Diallo (42) .......................... cours 11 terminé
--   Fatou Ndiaye (44) ............................ vient de commencer
--   Awa Ndoye (43) ............................... n'a rien ouvert (état vide utile à montrer)
-- ---------------------------------------------------------------------
INSERT INTO progressions_support (etudiant_id, support_id, date_consultation)
SELECT v.etudiant, v.support, now() - (v.jours || ' days')::interval
FROM (VALUES
    -- compte de démonstration : progression visible sur trois cours
    (47, 11, 12), (47, 12, 9), (47, 13, 5), (47, 15, 2),
    -- cours 9 terminé / à mi-parcours
    (38, 11, 21), (38, 12, 18),
    (39, 11, 16),
    -- cours 10 terminé / à mi-parcours
    (40, 13, 20), (40, 14, 14),
    (41, 13, 11),
    -- cours 11 terminé
    (42, 15, 19), (42, 16, 13),
    -- démarrage récent
    (44, 11, 3)
) AS v(etudiant, support, jours)
WHERE NOT EXISTS (
    SELECT 1 FROM progressions_support p
    WHERE p.etudiant_id = v.etudiant AND p.support_id = v.support);

-- ---------------------------------------------------------------------
-- 2. Photothèque (espace étudiant → photothèque)
--
-- L'album « Visite du terminal pétrolier de Sendou » existait sans aucune
-- photo. Les trois fichiers référencés sont ceux déposés par
-- DataInitializer dans uploads/photos/ : ils existent réellement sur le
-- disque, les vignettes s'afficheront donc au lieu d'images cassées.
--
-- Le chemin est relatif à uploads/ et inclut le sous-dossier : PhotoStorageService
-- résout fichier_stocke depuis la racine des dépôts, pas depuis uploads/photos/.
-- ---------------------------------------------------------------------
INSERT INTO photos (album_id, fichier_stocke, nom_original, legende, date_ajout)
SELECT a.id, v.stocke, v.origine, v.legende, now() - (v.jours || ' days')::interval
FROM albums_photo a
CROSS JOIN (VALUES
    ('photos/demo-photo-ds.png',   'terminal-arrivee.png',   'Arrivée de la promotion sur le site', 24),
    ('photos/demo-photo-fin.png',  'terminal-cuves.png',     'Présentation des cuves de stockage',  24),
    ('photos/demo-photo-proc.png', 'terminal-securite.png',  'Briefing sécurité avant la visite',   24)
) AS v(stocke, origine, legende, jours)
WHERE a.titre = 'Visite du terminal pétrolier de Sendou'
  AND NOT EXISTS (
    SELECT 1 FROM photos p WHERE p.album_id = a.id AND p.fichier_stocke = v.stocke);

-- ---------------------------------------------------------------------
-- 3. Forum (espace étudiant et formateur)
--
-- Deux sujets seulement, dont aucun épinglé ni clos : impossible de
-- montrer l'épinglage, la résolution ou le filtrage. On ajoute quatre
-- sujets couvrant ces trois états, rattachés à des cours différents.
-- ---------------------------------------------------------------------
INSERT INTO forum_sujets (titre, cours_id, auteur_id, date_creation, epingle, clos)
SELECT v.titre, v.cours, v.auteur, now() - (v.jours || ' days')::interval, v.epingle, v.clos
FROM (VALUES
    ('Modalités de l''évaluation finale',        9,  37, 15, true,  false),
    ('Erreur au chargement du dataset Titanic',  9,  39,  8, false, true),
    ('Ressources complémentaires sur l''ESG',   10,  40,  6, false, false),
    ('Report de la séance du 20 août',          11,  37,  2, true,  false)
) AS v(titre, cours, auteur, jours, epingle, clos)
WHERE NOT EXISTS (SELECT 1 FROM forum_sujets s WHERE s.titre = v.titre);

INSERT INTO forum_messages (sujet_id, auteur_id, contenu, date_creation)
SELECT s.id, v.auteur, v.contenu, s.date_creation + (v.heures || ' hours')::interval
FROM forum_sujets s
JOIN (VALUES
    ('Modalités de l''évaluation finale', 37,  0,
     'L''évaluation portera sur un projet individuel remis en fin de semestre, plus une soutenance de quinze minutes.'),
    ('Modalités de l''évaluation finale', 38,  5,
     'Merci. Le projet peut-il porter sur un jeu de données de notre choix ?'),
    ('Modalités de l''évaluation finale', 37, 20,
     'Oui, à condition de le valider avec moi avant le début des travaux.'),
    ('Erreur au chargement du dataset Titanic', 39, 0,
     'Le lien Kaggle me renvoie une erreur 403 depuis hier. Quelqu''un a le même souci ?'),
    ('Erreur au chargement du dataset Titanic', 42, 3,
     'Il faut être connecté à Kaggle avant de cliquer, sinon le téléchargement est refusé.'),
    ('Erreur au chargement du dataset Titanic', 39, 6,
     'C''était bien ça, merci !'),
    ('Ressources complémentaires sur l''ESG', 40, 0,
     'Est-ce qu''il existe une lecture recommandée en français sur les critères ESG ?'),
    ('Report de la séance du 20 août', 37, 0,
     'La séance du 20 août est reportée au 27, même horaire et même salle.')
) AS v(sujet, auteur, heures, contenu) ON v.sujet = s.titre
WHERE NOT EXISTS (
    SELECT 1 FROM forum_messages m WHERE m.sujet_id = s.id AND m.contenu = v.contenu);

-- ---------------------------------------------------------------------
-- 4. Tests académiques (espace responsable d'admission)
--
-- La table était vide : l'écran « Tests » n'avait rien à afficher. Deux
-- épreuves par candidat, avec des notes contrastées — un candidat au-dessus
-- du seuil, un juste en dessous — pour que la délibération ait du sens.
-- ---------------------------------------------------------------------
INSERT INTO tests_academiques (candidat_id, nom_epreuve, coefficient, note_obtenue, date_epreuve)
SELECT v.candidat, v.epreuve, v.coef, v.note, now() - (v.jours || ' days')::interval
FROM (VALUES
    (3,  'Culture scientifique',     1.0, 14.5, 30),
    (3,  'Mathématiques appliquées', 2.0, 15.0, 28),
    (4,  'Culture scientifique',     1.0, 11.0, 30),
    (4,  'Mathématiques appliquées', 2.0,  9.5, 28),
    (5,  'Culture scientifique',     1.0, 16.0, 30),
    (5,  'Mathématiques appliquées', 2.0, 17.5, 28),
    (46, 'Culture scientifique',     1.0, 12.5, 30),
    (46, 'Mathématiques appliquées', 2.0, 13.0, 28)
) AS v(candidat, epreuve, coef, note, jours)
WHERE NOT EXISTS (
    SELECT 1 FROM tests_academiques t
    WHERE t.candidat_id = v.candidat AND t.nom_epreuve = v.epreuve);

-- ---------------------------------------------------------------------
-- 5. Entretiens (espace responsable d'admission)
--
-- Trois situations différentes, pour montrer l'écran dans ses trois états :
-- un entretien passé et noté, un à venir confirmé, un à venir non confirmé.
-- ---------------------------------------------------------------------
INSERT INTO entretiens (candidat_id, date_heure, lieu_ou_lien, note_entretien,
                        appreciation_globale, confirme_par_candidat)
SELECT v.candidat, now() + (v.jours || ' days')::interval, v.lieu, v.note, v.appreciation, v.confirme
FROM (VALUES
    (3,  -12, 'Salle B12 — Immeuble Adja Koone', 15.0,
     'Motivation solide et projet professionnel clair. Bonne connaissance du secteur pétrolier.', true),
    (5,   -9, 'Visioconférence',                 17.0,
     'Excellent candidat : raisonnement structuré, anglais courant, expérience de terrain.',      true),
    (46,  4,  'Salle B12 — Immeuble Adja Koone', NULL, NULL, true),
    (4,   7,  'Visioconférence',                 NULL, NULL, false)
) AS v(candidat, jours, lieu, note, appreciation, confirme)
WHERE NOT EXISTS (SELECT 1 FROM entretiens e WHERE e.candidat_id = v.candidat);

COMMIT;

-- =====================================================================
-- CONTRÔLE
-- Après exécution, ces compteurs doivent tous être non nuls :
--
--   SELECT 'progressions' t, count(*) FROM progressions_support
--   UNION ALL SELECT 'photos',      count(*) FROM photos
--   UNION ALL SELECT 'sujets',      count(*) FROM forum_sujets
--   UNION ALL SELECT 'messages',    count(*) FROM forum_messages
--   UNION ALL SELECT 'tests',       count(*) FROM tests_academiques
--   UNION ALL SELECT 'entretiens',  count(*) FROM entretiens;
-- =====================================================================
