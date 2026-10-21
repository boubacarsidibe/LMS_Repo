-- =====================================================================
-- Données de démonstration — espace formateur (15/08/2026, réécrit le 19/10/2026)
-- =====================================================================
--
-- CE N'EST PAS UNE MIGRATION DE SCHÉMA. Ce script ajoute du CONTENU
-- (séances, notes, présences, avis) pour que chaque section de la
-- sidebar formateur ait un exemple concret à montrer à un client,
-- plutôt que des écrans vides. Il ne touche à aucune structure de
-- table — pas de rapport avec Flyway (src/main/resources/db/migration),
-- volontairement laissé en dehors de ce dossier.
--
-- Portée : le formateur de démo (prof.demo@inpg.sn) et ses trois cours de
-- démonstration (DS-101 Introduction au Machine Learning, FIN-201 Analyse
-- Financière Avancée, PROC-301 Génie des Procédés Industriels).
--
-- POURQUOI CETTE RÉÉCRITURE
-- La première version désignait les cours, les étudiants et la promotion par
-- des identifiants numériques (9, 10, 11, 37, 38-44…) : ceux de la base de son
-- auteur. Sur toute autre base, ils tombent ailleurs ; et elle écrivait dans
-- `cours_promotion`, table supprimée par la migration V6 (un cours se rattache
-- à une formation, jamais à une promotion — voir CLAUDE.md §3). Résultat : le
-- script échouait en bloc (ROLLBACK) et laissait l'espace formateur vide, alors
-- que le README le recommande en cas d'« écrans vides côté formateur ».
-- Tout est désormais résolu par clé naturelle (nom de cours, nom d'utilisateur)
-- et par la chaîne promotion → formation → cours, jamais par identifiant.
--
-- Réexécutable sans dupliquer : chaque insertion est gardée par un
-- WHERE NOT EXISTS sur la ligne qu'elle créerait.
--
-- Utilisation :
--   docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-formateur.sql
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Séances : deux passées (pour l'historique de présence) et une à venir
--    (pour que le calendrier affiche autre chose qu'un mois écoulé). Les
--    dates sont relatives à aujourd'hui : le script reste crédible quel que
--    soit le jour où on le rejoue.
-- ---------------------------------------------------------------------
INSERT INTO seance (titre, cours_id, date_heure, duree_minutes, lieu_ou_lien, type)
SELECT v.titre, c.id, date_trunc('day', now()) + (v.decalage_jours || ' days')::interval + v.heure::time,
       v.duree, v.lieu, v.type
FROM (VALUES
    ('Introduction au Machine Learning — séance d''ouverture', 'DS-101',   -45, '09:00', 120, 'Amphi C',       'cours'),
    ('Introduction au Machine Learning — travaux pratiques',   'DS-101',   -12, '14:00',  90, 'Salle Info 2',  'tp'),
    ('Introduction au Machine Learning — projet encadré',      'DS-101',     6, '14:00',  90, 'Salle Info 2',  'td'),
    ('Analyse Financière Avancée — séance d''ouverture',       'FIN-201',  -44, '09:00', 120, 'Amphi B',       'cours'),
    ('Analyse Financière Avancée — étude de cas',              'FIN-201',  -10, '09:00', 120, 'Amphi B',       'td'),
    ('Analyse Financière Avancée — évaluation finale',         'FIN-201',    9, '09:00', 120, 'Amphi B',       'examen'),
    ('Génie des Procédés Industriels — séance d''ouverture',   'PROC-301', -43, '09:00', 120, 'Amphi A',       'cours'),
    ('Génie des Procédés Industriels — travaux dirigés',       'PROC-301',  -9, '10:00',  90, 'Salle B12',     'td'),
    ('Génie des Procédés Industriels — visite d''atelier',     'PROC-301',   8, '10:00', 180, 'Atelier pilote', 'tp')
) AS v(titre, cours_nom, decalage_jours, heure, duree, lieu, type)
JOIN cours c ON c.nom = v.cours_nom
WHERE NOT EXISTS (
    SELECT 1 FROM seance s WHERE s.titre = v.titre AND s.cours_id = c.id
);

-- ---------------------------------------------------------------------
-- 2. Présences : uniquement sur les séances déjà passées — marquer une
--    présence sur une séance future n'aurait pas de sens. Les étudiants
--    concernés sont ceux dont la promotion suit une formation qui contient
--    le cours (promotion → formation → cours). Répartition volontairement
--    inégale (environ quatre présents sur cinq) pour que la page ne montre
--    pas un 100 % irréaliste.
-- ---------------------------------------------------------------------
INSERT INTO presence (present, etudiant_id, seance_id)
SELECT (e.id_user % 5) <> 0, e.id_user, s.id
FROM seance s
JOIN formation_cours fc ON fc.cours_id = s.cours_id
JOIN promotion p ON p.formation_id = fc.formation_id
JOIN etudiants e ON e.promotion_id = p.id
JOIN cours c ON c.id = s.cours_id
WHERE c.nom IN ('DS-101', 'FIN-201', 'PROC-301')
  AND s.date_heure < now()
ON CONFLICT (seance_id, etudiant_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. Notes de module : deux par étudiant et par cours, avec une vraie
--    dispersion des résultats (de 30 à 90 sur 100) — pas uniformément
--    excellentes, sinon la page de suivi n'a plus rien d'instructif à montrer.
--    Les notes de module se saisissent SUR 100 (le serveur refuse hors de
--    0-100 ; l'équivalent sur 20 n'est qu'un affichage). La première version
--    de ce script les écrivait sur 20, ce qui donnait « 12/100 (2,4/20) » à
--    l'écran. Le calcul est déterministe (dérivé des identifiants) : rejouer le
--    script redonne exactement les mêmes notes.
-- ---------------------------------------------------------------------
INSERT INTO note_module (commentaire, date_evaluation, formateur_nom, module, note, cours_id, etudiant_id)
SELECT CASE
           WHEN t.note >= 75 THEN 'Très bonne maîtrise des notions, travail rigoureux.'
           WHEN t.note >= 60 THEN 'Résultat solide, à consolider sur la partie pratique.'
           WHEN t.note >= 50 THEN 'Niveau correct ; peut mieux faire avec un peu plus de régularité.'
           ELSE 'Des lacunes à combler : un point individuel est conseillé.'
       END,
       (current_date - t.age_jours)::date, 'Moussa Ndiaye', t.module, t.note, t.cours_id, t.etudiant_id
FROM (
    SELECT m.module, c.id AS cours_id, e.id_user AS etudiant_id,
           (30 + ((e.id_user * 7 + m.rang * 5 + c.id * 3) % 13) * 5)::double precision AS note,
           m.age_jours
    FROM (VALUES
        ('DS-101',   'Fondamentaux du ML',          1, 40),
        ('DS-101',   'Apprentissage supervisé',     2, 14),
        ('FIN-201',  'Analyse de bilan',            1, 39),
        ('FIN-201',  'Évaluation de projets',       2, 13),
        ('PROC-301', 'Thermodynamique appliquée',   1, 38),
        ('PROC-301', 'Sécurité des procédés',       2, 12)
    ) AS m(cours_nom, module, rang, age_jours)
    JOIN cours c ON c.nom = m.cours_nom
    JOIN formation_cours fc ON fc.cours_id = c.id
    JOIN promotion p ON p.formation_id = fc.formation_id
    JOIN etudiants e ON e.promotion_id = p.id
) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM note_module n
    WHERE n.etudiant_id = t.etudiant_id AND n.module = t.module AND n.cours_id = t.cours_id
);

-- ---------------------------------------------------------------------
-- 4. Retour d'évaluation + certificat pour le 3ᵉ cours (Génie des
--    Procédés) : les deux autres cours en avaient déjà, celui-ci
--    n'avait ni l'un ni l'autre — incohérent pour une démonstration
--    qui montre les trois cours côte à côte.
-- ---------------------------------------------------------------------
INSERT INTO retour_evaluation (commentaire, date_creation, epingle, note, statut, cours_id, etudiant_id)
SELECT 'Cours très concret, les études de cas industrielles aident beaucoup à comprendre.',
       now(), false, 5, 'VALIDE', c.id, u.id
FROM users u, cours c
WHERE u.username = 'mamadou_diallo' AND c.nom = 'PROC-301'
  AND NOT EXISTS (
    SELECT 1 FROM retour_evaluation r WHERE r.cours_id = c.id AND r.etudiant_id = u.id
  );

INSERT INTO certificat (date_emission, formateur_nom, intitule_formation, reference, etudiant_id, promotion_id)
SELECT current_date - 20, 'Moussa Ndiaye', 'Génie des Procédés Industriels',
       'INPG-CERT-2026-DEM0F001', e.id_user, e.promotion_id
FROM etudiants e
JOIN users u ON u.id = e.id_user
WHERE u.username = 'mamadou_diallo'
  AND NOT EXISTS (SELECT 1 FROM certificat WHERE reference = 'INPG-CERT-2026-DEM0F001');

COMMIT;

-- =====================================================================
-- CONTRÔLE
-- Après exécution, ces compteurs doivent tous être non nuls :
--
--   SELECT 'seances' t, count(*) FROM seance
--   UNION ALL SELECT 'presences', count(*) FROM presence
--   UNION ALL SELECT 'notes',     count(*) FROM note_module
--   UNION ALL SELECT 'retours',   count(*) FROM retour_evaluation
--   UNION ALL SELECT 'certificats', count(*) FROM certificat;
-- =====================================================================
