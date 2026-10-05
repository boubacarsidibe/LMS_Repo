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
--   Bineta Ndour (test_etudiant, compte de démonstration) ... 4 supports sur 6
--   Aissatou Ba (aissatou_ba) ............................... cours DS-101 terminé
--   Cheikh Fall (cheikh_fall) ............................... cours DS-101 à mi-parcours
--   Fatou Diagne (fatou_diagne) ............................. cours FIN-201 terminé
--   Ibrahima Sow (ibrahima_sow) ............................. cours FIN-201 à mi-parcours
--   Mamadou Diallo (mamadou_diallo) ......................... cours PROC-301 terminé
--   Fatou Ndiaye (etudiant_demo) ............................ vient de commencer
--   Awa Ndoye (awa_ndoye) ................................... n'a rien ouvert (état vide utile à montrer)
-- ---------------------------------------------------------------------
-- Étudiants et supports sont résolus par identifiant stable (username, titre),
-- jamais par id en dur : DataInitializer ne garantit ni que ces comptes
-- tombent sur les ids attendus, ni que ces 6 lignes de support_cours tombent
-- sur les ids 11-16 initialement écrits ici (elles tombent sur 1-6 dans une
-- base fraîche). Un id en dur faisait échouer tout le script sur une
-- contrainte de clé étrangère, en transaction unique donc sans rien
-- appliquer (ROLLBACK complet, cf. CLAUDE.md).
INSERT INTO progressions_support (etudiant_id, support_id, date_consultation)
SELECT u.id, sc.id, now() - (v.jours || ' days')::interval
FROM (VALUES
    -- compte de démonstration : progression visible sur trois cours
    ('test_etudiant',  'Slides - Introduction au Machine Learning', 12),
    ('test_etudiant',  'Dataset Kaggle - Titanic', 9),
    ('test_etudiant',  'Support - Analyse Financiere Avancee', 5),
    ('test_etudiant',  'Cours - Genie des Procedes Chapitre 1', 2),
    -- cours DS-101 terminé / à mi-parcours
    ('aissatou_ba',    'Slides - Introduction au Machine Learning', 21),
    ('aissatou_ba',    'Dataset Kaggle - Titanic', 18),
    ('cheikh_fall',    'Slides - Introduction au Machine Learning', 16),
    -- cours FIN-201 terminé / à mi-parcours
    ('fatou_diagne',   'Support - Analyse Financiere Avancee', 20),
    ('fatou_diagne',   'Article ESG - Banque Mondiale', 14),
    ('ibrahima_sow',   'Support - Analyse Financiere Avancee', 11),
    -- cours PROC-301 terminé
    ('mamadou_diallo', 'Cours - Genie des Procedes Chapitre 1', 19),
    ('mamadou_diallo', 'Norme ISO 9001', 13),
    -- démarrage récent
    ('etudiant_demo',  'Slides - Introduction au Machine Learning', 3)
    -- awa_ndoye volontairement absente de cette liste : état "n'a rien ouvert"
) AS v(username, support_titre, jours)
JOIN users u ON u.username = v.username
JOIN support_cours sc ON sc.titre = v.support_titre
WHERE NOT EXISTS (
    SELECT 1 FROM progressions_support p
    WHERE p.etudiant_id = u.id AND p.support_id = sc.id);

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
--
-- Cours et auteurs résolus par identifiant stable (nom de cours, username),
-- jamais par id en dur — même raison qu'à la section 1.
-- ---------------------------------------------------------------------
INSERT INTO forum_sujets (titre, cours_id, auteur_id, date_creation, epingle, clos)
SELECT v.titre, c.id, u.id, now() - (v.jours || ' days')::interval, v.epingle, v.clos
FROM (VALUES
    ('Modalités de l''évaluation finale',        'DS-101',   'prof_demo',    15, true,  false),
    ('Erreur au chargement du dataset Titanic',  'DS-101',   'cheikh_fall',   8, false, true),
    ('Ressources complémentaires sur l''ESG',    'FIN-201',  'fatou_diagne',  6, false, false),
    ('Report de la séance du 20 août',           'PROC-301', 'prof_demo',     2, true,  false)
) AS v(titre, cours_nom, username, jours, epingle, clos)
JOIN cours c ON c.nom = v.cours_nom
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM forum_sujets s WHERE s.titre = v.titre);

INSERT INTO forum_messages (sujet_id, auteur_id, contenu, date_creation)
SELECT s.id, u.id, v.contenu, s.date_creation + (v.heures || ' hours')::interval
FROM forum_sujets s
JOIN (VALUES
    ('Modalités de l''évaluation finale', 'prof_demo',  0,
     'L''évaluation portera sur un projet individuel remis en fin de semestre, plus une soutenance de quinze minutes.'),
    ('Modalités de l''évaluation finale', 'aissatou_ba',  5,
     'Merci. Le projet peut-il porter sur un jeu de données de notre choix ?'),
    ('Modalités de l''évaluation finale', 'prof_demo', 20,
     'Oui, à condition de le valider avec moi avant le début des travaux.'),
    ('Erreur au chargement du dataset Titanic', 'cheikh_fall', 0,
     'Le lien Kaggle me renvoie une erreur 403 depuis hier. Quelqu''un a le même souci ?'),
    ('Erreur au chargement du dataset Titanic', 'mamadou_diallo', 3,
     'Il faut être connecté à Kaggle avant de cliquer, sinon le téléchargement est refusé.'),
    ('Erreur au chargement du dataset Titanic', 'cheikh_fall', 6,
     'C''était bien ça, merci !'),
    ('Ressources complémentaires sur l''ESG', 'fatou_diagne', 0,
     'Est-ce qu''il existe une lecture recommandée en français sur les critères ESG ?'),
    ('Report de la séance du 20 août', 'prof_demo', 0,
     'La séance du 20 août est reportée au 27, même horaire et même salle.')
) AS v(sujet, username, heures, contenu) ON v.sujet = s.titre
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (
    SELECT 1 FROM forum_messages m WHERE m.sujet_id = s.id AND m.contenu = v.contenu);

-- ---------------------------------------------------------------------
-- 4. Tests académiques (espace responsable d'admission)
--
-- La table était vide : l'écran « Tests » n'avait rien à afficher.
--
-- Le jeu de comptes de base (seed-comptes-test.sql) ne compte qu'un seul
-- candidat avec une candidature réellement déposée : test.candidat2
-- (test.candidat est délibérément sans dossier — état vide à montrer, cf.
-- CLAUDE.md). Impossible donc de contraster plusieurs profils de candidats
-- sans inventer des comptes qui n'existent nulle part ailleurs dans la
-- documentation de recette ; on se limite à deux épreuves pour ce candidat,
-- ce qui suffit à sortir l'écran de son état vide.
-- ---------------------------------------------------------------------
-- groupe_epreuve_id (V202609211030) regroupe les épreuves d'une même séance :
-- les deux lignes ci-dessous forment une seule séance pour ce candidat, d'où
-- l'UUID commun plutôt qu'un gen_random_uuid() par ligne.
INSERT INTO tests_academiques (candidat_id, nom_epreuve, coefficient, note_obtenue, date_epreuve, groupe_epreuve_id)
SELECT u.id, v.epreuve, v.coef, v.note, now() - (v.jours || ' days')::interval, seance.id
FROM (VALUES
    ('test_candidat2', 'Culture scientifique',     1.0, 14.5, 30),
    ('test_candidat2', 'Mathématiques appliquées', 2.0, 15.0, 28)
) AS v(username, epreuve, coef, note, jours)
JOIN users u ON u.username = v.username
CROSS JOIN (SELECT gen_random_uuid() AS id) AS seance
WHERE NOT EXISTS (
    SELECT 1 FROM tests_academiques t
    WHERE t.candidat_id = u.id AND t.nom_epreuve = v.epreuve);

-- ---------------------------------------------------------------------
-- 5. Entretiens (espace responsable d'admission)
--
-- Trois situations différentes montreraient l'écran dans ses trois états (un
-- entretien passé et noté, un à venir confirmé, un à venir non confirmé),
-- mais elles demandent chacune un candidat distinct. Même limite qu'à la
-- section 4 : un seul candidat (test.candidat2) a une candidature déposée
-- dans le jeu de comptes de base. On seme l'état le plus informatif — un
-- entretien passé et noté — pour sortir l'écran de son état vide ; les deux
-- autres états resteront à démontrer manuellement, ou demanderont d'enrichir
-- le jeu de comptes de test si le besoin se confirme.
-- ---------------------------------------------------------------------
-- groupe_entretien_id (V202609211100) regroupe les entretiens d'une même
-- convocation : une seule ligne ici, donc un UUID par ligne suffit.
INSERT INTO entretiens (candidat_id, date_heure, lieu_ou_lien, note_entretien,
                        appreciation_globale, confirme_par_candidat, groupe_entretien_id)
SELECT u.id, now() + (v.jours || ' days')::interval, v.lieu, v.note, v.appreciation, v.confirme, gen_random_uuid()
FROM (VALUES
    ('test_candidat2', -12, 'Salle B12 — Immeuble Adja Koone', 15.0,
     'Motivation solide et projet professionnel clair. Bonne connaissance du secteur pétrolier.', true)
) AS v(username, jours, lieu, note, appreciation, confirme)
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM entretiens e WHERE e.candidat_id = u.id);

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
