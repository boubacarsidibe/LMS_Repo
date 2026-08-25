-- =====================================================================
-- Données de démonstration — espace formateur (15/08/2026)
-- =====================================================================
--
-- CE N'EST PAS UNE MIGRATION DE SCHÉMA. Ce script ajoute du CONTENU
-- (séances, notes, présences, avis) pour que chaque section de la
-- sidebar formateur ait un exemple concret à montrer à un client,
-- plutôt que des écrans vides. Il ne touche à aucune structure de
-- table — pas de rapport avec Flyway (src/main/resources/db/migration),
-- volontairement laissé en dehors de ce dossier.
--
-- Portée : le formateur de démo (prof.demo@inpg.sn, id_user=37) et ses
-- 3 cours (Introduction au Machine Learning, Analyse Financière
-- Avancée, Génie des Procédés Industriels), rattachés à la Promotion
-- 2026 — Master Pétrole & Gaz (id=1, 8 étudiants).
--
-- Réexécutable sans dupliquer : les tables à contrainte d'unicité
-- utilisent ON CONFLICT DO NOTHING ; les autres sont protégées par un
-- WHERE NOT EXISTS sur la ligne qu'elles créeraient.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Rattacher les 3 cours du formateur à la promotion
--    Sans ce lien, « Suivi Étudiants » affiche 0 étudiant — la requête
--    du contrôleur (findByPromotion_IdIn) ne trouve personne.
-- ---------------------------------------------------------------------
INSERT INTO cours_promotion (cours_id, promotion_id) VALUES
    (9, 1), (10, 1), (11, 1)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. Séances : une passée (pour l'historique de présence), une à venir
--    (pour que le calendrier affiche autre chose qu'un mois vide).
-- ---------------------------------------------------------------------
INSERT INTO seance (titre, cours_id, date_heure, duree_minutes, lieu_ou_lien, type)
SELECT * FROM (VALUES
    ('Introduction au Machine Learning — séance d''ouverture', 9::bigint, '2026-08-04 09:00:00'::timestamp, 120, 'Amphi C', 'cours'),
    ('Introduction au Machine Learning — travaux pratiques',   9::bigint, '2026-08-20 14:00:00'::timestamp, 90,  'Salle Info 2', 'tutorat'),
    ('Analyse Financière Avancée — séance d''ouverture',       10::bigint, '2026-08-05 09:00:00'::timestamp, 120, 'Amphi B', 'cours'),
    ('Analyse Financière Avancée — étude de cas',              10::bigint, '2026-08-19 09:00:00'::timestamp, 120, 'Amphi B', 'cours'),
    ('Génie des Procédés Industriels — séance d''ouverture',   11::bigint, '2026-08-06 09:00:00'::timestamp, 120, 'Amphi A', 'cours'),
    ('Génie des Procédés Industriels — travaux dirigés',       11::bigint, '2026-08-21 10:00:00'::timestamp, 90,  'Salle B12', 'tutorat')
) AS nouvelles(titre, cours_id, date_heure, duree_minutes, lieu_ou_lien, type)
WHERE NOT EXISTS (
    SELECT 1 FROM seance s WHERE s.titre = nouvelles.titre AND s.cours_id = nouvelles.cours_id
);

-- ---------------------------------------------------------------------
-- 3. Présences : uniquement sur les séances déjà passées — marquer une
--    présence sur une séance future n'aurait pas de sens.
--    Répartition volontairement inégale (7-8 présents sur 8) pour que
--    la page ne montre pas un 100 % irréaliste.
-- ---------------------------------------------------------------------
INSERT INTO presence (present, etudiant_id, seance_id)
SELECT p.present, e.id_user, s.id
FROM etudiants e
JOIN seance s ON s.titre IN (
    'Introduction au Machine Learning — séance d''ouverture',
    'Analyse Financière Avancée — séance d''ouverture',
    'Génie des Procédés Industriels — séance d''ouverture'
)
JOIN (VALUES
    (18::bigint, true), (38::bigint, true), (39::bigint, true), (40::bigint, true),
    (41::bigint, false), (42::bigint, true), (43::bigint, true), (44::bigint, true)
) AS p(etudiant_id, present) ON p.etudiant_id = e.id_user
WHERE e.promotion_id = 1
ON CONFLICT (seance_id, etudiant_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 4. Notes de module : 2 par étudiant, réparties sur les 3 cours, avec
--    une vraie dispersion des résultats — pas uniformément excellentes,
--    sinon la page de suivi n'a plus rien d'instructif à montrer.
-- ---------------------------------------------------------------------
INSERT INTO note_module (commentaire, date_evaluation, formateur_nom, module, note, cours_id, etudiant_id)
SELECT * FROM (VALUES
    ('Bonne maîtrise des concepts fondamentaux.', '2026-08-07'::date, 'Moussa Ndiaye', 'Fondamentaux du ML', 16.0, 9::bigint, 38::bigint),
    ('Progression solide sur les modèles supervisés.', '2026-08-12'::date, 'Moussa Ndiaye', 'Apprentissage supervisé', 15.0, 9::bigint, 38::bigint),

    ('Des lacunes sur les prérequis mathématiques.', '2026-08-07'::date, 'Moussa Ndiaye', 'Fondamentaux du ML', 9.0, 9::bigint, 39::bigint),
    ('Peu d''amélioration depuis la dernière évaluation.', '2026-08-12'::date, 'Moussa Ndiaye', 'Apprentissage supervisé', 8.0, 9::bigint, 39::bigint),

    ('Analyse de bilan bien structurée.', '2026-08-08'::date, 'Moussa Ndiaye', 'Analyse de bilan', 13.0, 10::bigint, 40::bigint),
    ('Bonne compréhension des méthodes d''évaluation.', '2026-08-13'::date, 'Moussa Ndiaye', 'Évaluation de projets', 14.0, 10::bigint, 40::bigint),

    ('Difficultés à mobiliser les notions du cours en exercice.', '2026-08-08'::date, 'Moussa Ndiaye', 'Analyse de bilan', 6.0, 10::bigint, 41::bigint),
    ('Assiduité irrégulière, à recontacter.', '2026-08-13'::date, 'Moussa Ndiaye', 'Évaluation de projets', 9.0, 10::bigint, 41::bigint),

    ('Excellent travail sur le projet de thermodynamique.', '2026-08-09'::date, 'Moussa Ndiaye', 'Thermodynamique appliquée', 17.0, 11::bigint, 42::bigint),
    ('Rigueur exemplaire sur les protocoles de sécurité.', '2026-08-14'::date, 'Moussa Ndiaye', 'Sécurité des procédés', 18.0, 11::bigint, 42::bigint),

    ('Résultat correct, peut approfondir la partie pratique.', '2026-08-09'::date, 'Moussa Ndiaye', 'Thermodynamique appliquée', 11.0, 11::bigint, 43::bigint),
    ('Encore en retrait sur les cas d''application.', '2026-08-14'::date, 'Moussa Ndiaye', 'Sécurité des procédés', 10.0, 11::bigint, 43::bigint),

    ('Bonne participation, résultats réguliers.', '2026-08-07'::date, 'Moussa Ndiaye', 'Fondamentaux du ML', 14.0, 9::bigint, 44::bigint),
    ('Consolide bien ses acquis d''une séance à l''autre.', '2026-08-13'::date, 'Moussa Ndiaye', 'Évaluation de projets', 15.0, 10::bigint, 44::bigint),

    ('Niveau correct sur les bases, à consolider.', '2026-08-07'::date, 'Moussa Ndiaye', 'Fondamentaux du ML', 12.0, 9::bigint, 18::bigint),
    ('Peut mieux faire sur la partie pratique.', '2026-08-14'::date, 'Moussa Ndiaye', 'Sécurité des procédés', 13.0, 11::bigint, 18::bigint)
) AS nouvelles(commentaire, date_evaluation, formateur_nom, module, note, cours_id, etudiant_id)
WHERE NOT EXISTS (
    SELECT 1 FROM note_module n
    WHERE n.etudiant_id = nouvelles.etudiant_id AND n.module = nouvelles.module AND n.cours_id = nouvelles.cours_id
);

-- ---------------------------------------------------------------------
-- 5. Retour d'évaluation + certificat pour le 3ᵉ cours (Génie des
--    Procédés) : les deux autres cours en avaient déjà, celui-ci
--    n'avait ni l'un ni l'autre — incohérent pour une démonstration
--    qui montre les 3 cours côte à côte.
-- ---------------------------------------------------------------------
INSERT INTO retour_evaluation (commentaire, date_creation, epingle, note, statut, cours_id, etudiant_id)
SELECT 'Cours très concret, les études de cas industrielles aident beaucoup à comprendre.',
       now(), false, 5, 'VALIDE', 11, 42
WHERE NOT EXISTS (
    SELECT 1 FROM retour_evaluation WHERE cours_id = 11 AND etudiant_id = 42
);

INSERT INTO certificat (date_emission, formateur_nom, intitule_formation, reference, etudiant_id, promotion_id)
SELECT '2026-08-14'::date, 'Moussa Ndiaye', 'Génie des Procédés Industriels',
       'INPG-CERT-2026-DEM0F001', 42, 1
WHERE NOT EXISTS (
    SELECT 1 FROM certificat WHERE reference = 'INPG-CERT-2026-DEM0F001'
);

COMMIT;
