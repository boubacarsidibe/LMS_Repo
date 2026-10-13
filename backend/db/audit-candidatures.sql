-- Audit des dossiers de candidature étudiante — LECTURE SEULE (aucun INSERT/UPDATE/DELETE).
--
--   docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/audit-candidatures.sql
--
-- À lancer après tout incident d'envoi de pièces, et avant une campagne de relance : chaque requête répond à une
-- question précise et doit, sur une base saine, ne rendre aucune ligne (sauf les deux comptages de la section 0).
-- Documents exigés : pièce d'identité, CV, relevé de notes, et diplôme sauf niveau « 4e secondaire »
-- (règle écrite dans CompletudeDossier côté serveur ; à garder identique ici).
-- Suppose la migration V202610131100 appliquée (colonnes candidats.adresse et candidatures.consentement_le).

\echo
\echo '=== 0. Vue d''ensemble : dossiers par statut ==='
SELECT statut, COUNT(*) AS dossiers FROM candidatures GROUP BY statut ORDER BY dossiers DESC;

\echo
\echo '=== 1. Dossiers déposés SANS AUCUNE pièce (le candidat a soumis, aucun envoi n''a abouti) ==='
SELECT c.id, c.numero_dossier, c.statut, c.date_soumission, ca.nom, ca.prenom
FROM candidatures c
JOIN candidats ca ON ca.id_user = c.candidat_id
WHERE c.statut <> 'BROUILLON'
  AND NOT EXISTS (SELECT 1 FROM pieces_justificatives p WHERE p.candidature_id = c.id)
ORDER BY c.date_soumission;

\echo
\echo '=== 2. Dossiers encore modifiables à qui il MANQUE un document exigé (ceux que le suivi réclame) ==='
SELECT c.id, c.numero_dossier, c.statut, ca.niveau_etude,
       string_agg(r.type_requis, ', ' ORDER BY r.type_requis) AS manquants
FROM candidatures c
JOIN candidats ca ON ca.id_user = c.candidat_id
CROSS JOIN LATERAL (
    SELECT t AS type_requis
    FROM unnest(ARRAY['PIECE_IDENTITE', 'CV', 'RELEVE_NOTES', 'DIPLOME']) AS t
    WHERE (t <> 'DIPLOME' OR COALESCE(ca.niveau_etude, '') <> '4e secondaire')
      AND NOT EXISTS (SELECT 1 FROM pieces_justificatives p WHERE p.candidature_id = c.id AND p.type_document = t)
) r
WHERE c.statut IN ('SOUMIS', 'INCOMPLET')
GROUP BY c.id, c.numero_dossier, c.statut, ca.niveau_etude
ORDER BY c.id;

\echo
\echo '=== 3. Dossiers APRÈS la décision d''examen alors qu''un document exigé manque (n''aurait pas dû passer) ==='
SELECT c.id, c.numero_dossier, c.statut, ca.niveau_etude,
       string_agg(r.type_requis, ', ' ORDER BY r.type_requis) AS manquants
FROM candidatures c
JOIN candidats ca ON ca.id_user = c.candidat_id
CROSS JOIN LATERAL (
    SELECT t AS type_requis
    FROM unnest(ARRAY['PIECE_IDENTITE', 'CV', 'RELEVE_NOTES', 'DIPLOME']) AS t
    WHERE (t <> 'DIPLOME' OR COALESCE(ca.niveau_etude, '') <> '4e secondaire')
      AND NOT EXISTS (SELECT 1 FROM pieces_justificatives p WHERE p.candidature_id = c.id AND p.type_document = t)
) r
WHERE c.statut NOT IN ('BROUILLON', 'SOUMIS', 'INCOMPLET', 'DOSSIER_REJETE', 'DESISTEMENT', 'NON_ADMIS')
GROUP BY c.id, c.numero_dossier, c.statut, ca.niveau_etude
ORDER BY c.id;

\echo
\echo '=== 4. Pièces sans fichier, ou dont le fichier n''est plus référencé (pièce « fantôme ») ==='
SELECT p.id AS piece_id, p.candidature_id, p.type_document, p.fichier_stocke_id
FROM pieces_justificatives p
LEFT JOIN fichiers_stockes f ON f.id = p.fichier_stocke_id
WHERE p.fichier_stocke_id IS NULL OR f.id IS NULL;

\echo
\echo '=== 5. Fichiers stockés qu''aucune pièce ne référence (envoi coupé entre l''écriture du fichier et celle de la pièce) ==='
SELECT f.id, f.nom_original, f.taille_octets, f.date_upload
FROM fichiers_stockes f
WHERE NOT EXISTS (SELECT 1 FROM pieces_justificatives p WHERE p.fichier_stocke_id = f.id)
  AND NOT EXISTS (SELECT 1 FROM pieces_accreditation p WHERE p.fichier_stocke_id = f.id)
  AND NOT EXISTS (SELECT 1 FROM pieces_candidature_formateur p WHERE p.fichier_stocke_id = f.id)
  AND NOT EXISTS (SELECT 1 FROM offres_recrutement_formateur o WHERE o.fiche_poste_id = f.id)
ORDER BY f.date_upload DESC
LIMIT 50;

\echo
\echo '=== 6. Pièces en double : même dossier, même type, même nom et même taille (renvoi après une réponse perdue) ==='
SELECT p.candidature_id, p.type_document, f.nom_original, f.taille_octets, COUNT(*) AS exemplaires,
       array_agg(p.id ORDER BY p.id) AS pieces
FROM pieces_justificatives p
JOIN fichiers_stockes f ON f.id = p.fichier_stocke_id
GROUP BY p.candidature_id, p.type_document, f.nom_original, f.taille_octets, p.description, p.moyenne_declaree
HAVING COUNT(*) > 1
ORDER BY exemplaires DESC;

\echo
\echo '=== 7. Plus de 9 relevés de notes dans un même dossier (plafond du formulaire) ==='
SELECT candidature_id, COUNT(*) AS releves
FROM pieces_justificatives WHERE type_document = 'RELEVE_NOTES'
GROUP BY candidature_id HAVING COUNT(*) > 9;

\echo
\echo '=== 8. Diplômes déposés sans description (le serveur l''exige) ==='
SELECT id, candidature_id FROM pieces_justificatives
WHERE type_document = 'DIPLOME' AND (description IS NULL OR btrim(description) = '');

\echo
\echo '=== 9. Dossiers déposés dont le parcours académique n''a pas été enregistré (avant le correctif du 2026-10-13) ==='
SELECT COUNT(*) AS dossiers_sans_parcours,
       COUNT(*) FILTER (WHERE statut IN ('SOUMIS', 'INCOMPLET')) AS dont_encore_modifiables
FROM candidatures
WHERE statut <> 'BROUILLON' AND (informations_academiques IS NULL OR btrim(informations_academiques) = '');

\echo
\echo '=== 10. Dossiers déposés sans certification sur l''honneur datée, ni adresse du candidat ==='
SELECT COUNT(*) FILTER (WHERE c.consentement_le IS NULL) AS sans_certification_datee,
       COUNT(*) FILTER (WHERE ca.adresse IS NULL OR btrim(ca.adresse) = '') AS sans_adresse
FROM candidatures c
JOIN candidats ca ON ca.id_user = c.candidat_id
WHERE c.statut <> 'BROUILLON';

\echo
\echo '=== 11. Brouillons qui survivent à un dossier déposé (recréés après le dépôt : le formulaire se rouvrirait pré-rempli) ==='
SELECT b.id_user, b.date_maj, c.numero_dossier, c.statut
FROM brouillons_candidature b
JOIN candidats ca ON ca.id_user = b.id_user
JOIN candidatures c ON c.candidat_id = ca.id_user
WHERE c.statut NOT IN ('BROUILLON', 'DOSSIER_REJETE', 'NON_ADMIS');

\echo
\echo '=== 12. Candidats dont le téléphone est « N/A » (compte créé sans formulaire) ou partagé entre deux comptes ==='
SELECT id_user, nom, prenom, telephone FROM candidats WHERE telephone = 'N/A'
UNION ALL
SELECT id_user, nom, prenom, telephone FROM candidats
WHERE telephone IN (SELECT telephone FROM candidats WHERE telephone <> 'N/A' GROUP BY telephone HAVING COUNT(*) > 1);

\echo
\echo '=== 13. Plusieurs dossiers « actifs » pour un même candidat (le serveur n''en admet qu''un) ==='
SELECT candidat_id, COUNT(*) AS dossiers_actifs, array_agg(numero_dossier ORDER BY id) AS dossiers
FROM candidatures
WHERE statut NOT IN ('DOSSIER_REJETE', 'NON_ADMIS')
GROUP BY candidat_id HAVING COUNT(*) > 1;

\echo
\echo '=== 14. Dossiers « à compléter » dont toutes les pièces exigées sont fournies (le retour automatique à l\'examen a manqué) ==='
SELECT c.id, c.numero_dossier, ca.niveau_etude
FROM candidatures c
JOIN candidats ca ON ca.id_user = c.candidat_id
WHERE c.statut = 'INCOMPLET'
  AND NOT EXISTS (
        SELECT 1 FROM unnest(ARRAY['PIECE_IDENTITE', 'CV', 'RELEVE_NOTES', 'DIPLOME']) AS t
        WHERE (t <> 'DIPLOME' OR COALESCE(ca.niveau_etude, '') <> '4e secondaire')
          AND NOT EXISTS (SELECT 1 FROM pieces_justificatives p WHERE p.candidature_id = c.id AND p.type_document = t));

\echo
\echo '=== 15. Dossiers présélectionnés (ou plus loin) auxquels il manque un document exigé : décision prise avant la règle du 2026-10-13 ==='
SELECT c.id, c.numero_dossier, c.statut, string_agg(r.type_requis, ', ' ORDER BY r.type_requis) AS manquants
FROM candidatures c
JOIN candidats ca ON ca.id_user = c.candidat_id
CROSS JOIN LATERAL (
    SELECT t AS type_requis FROM unnest(ARRAY['PIECE_IDENTITE', 'CV', 'RELEVE_NOTES', 'DIPLOME']) AS t
    WHERE (t <> 'DIPLOME' OR COALESCE(ca.niveau_etude, '') <> '4e secondaire')
      AND NOT EXISTS (SELECT 1 FROM pieces_justificatives p WHERE p.candidature_id = c.id AND p.type_document = t)
) r
WHERE c.statut IN ('PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME', 'CONVOQUE_TEST', 'TEST_REUSSI', 'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ADMIS', 'LISTE_ATTENTE')
GROUP BY c.id, c.numero_dossier, c.statut
ORDER BY c.id;
