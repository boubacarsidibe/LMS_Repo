-- =====================================================================
-- V5 — Fusionner les deux forums en un seul
-- =====================================================================
--
-- PROBLÈME CORRIGÉ
-- L'application hébergeait deux forums parallèles et étanches :
--
--   sujet / message           servis par EspaceEtudiantController
--                             appelés par sujet.service.ts (écran formateur)
--   forum_sujets / forum_messages
--                             servis par ForumController
--                             appelés par communication.service.ts (écrans étudiant)
--
-- Une question posée par un étudiant n'apparaissait donc jamais dans
-- l'écran forum de son formateur, et inversement. Ce n'était pas une
-- duplication technique mais un défaut fonctionnel : le canal de
-- communication apprenant/formateur du TDR §3.5 était coupé en deux.
--
-- MODÈLE RETENU : forum_sujets / forum_messages
-- L'ancien modèle stockait l'auteur, le cours et le module comme de
-- simples chaînes de caractères (auteur_nom, cours_nom, module). Sans clé
-- étrangère, impossible de savoir QUI a écrit : ni notification ciblée, ni
-- contrôle d'accès, ni filtrage par cursus. forum_sujets référence
-- réellement Cours et User, ce qui permet à ForumController de n'exposer à
-- un étudiant que les sujets des cours de sa promotion.
--
-- Les capacités qui n'existaient que dans l'ancien modèle — pièces
-- jointes, sujet urgent, sujet résolu — sont portées ici plutôt que
-- perdues : les colonnes de pièce jointe sont ajoutées ci-dessous, et les
-- états urgent/résolu se lisent sur les colonnes « epingle » et « clos »
-- qui existaient déjà.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Porter les pièces jointes sur le modèle conservé
-- ---------------------------------------------------------------------
ALTER TABLE forum_messages ADD COLUMN IF NOT EXISTS piece_jointe_nom_original  VARCHAR(255);
ALTER TABLE forum_messages ADD COLUMN IF NOT EXISTS piece_jointe_nom_stocke    VARCHAR(255);
ALTER TABLE forum_messages ADD COLUMN IF NOT EXISTS piece_jointe_type_contenu  VARCHAR(255);

-- ---------------------------------------------------------------------
-- 2. Reprendre les sujets existants
--    cours_id et auteur_id restent NULL : l'ancien modèle ne stockait que
--    des libellés (« Macroeconomie Avancee », « Alex Mercer ») sans lien
--    vers les tables cours et users. Deviner une correspondance par le nom
--    rattacherait des messages au mauvais cours ou au mauvais auteur — un
--    sujet sans cours reste visible de tous, ce qui est le comportement
--    sûr. Le titre conserve le contexte perdu.
--
--    Correspondance des états :
--      RESOLU  -> clos = true
--      URGENT  -> epingle = true
--      NOUVEAU -> ni l'un ni l'autre
-- ---------------------------------------------------------------------
INSERT INTO forum_sujets (titre, cours_id, auteur_id, date_creation, epingle, clos)
SELECT
    CASE
        WHEN s.cours_nom IS NOT NULL AND s.cours_nom <> ''
            THEN s.titre || ' (' || s.cours_nom || ')'
        ELSE s.titre
    END,
    NULL,
    NULL,
    s.date_creation,
    s.statut = 'URGENT',
    s.statut = 'RESOLU'
FROM sujet s
WHERE NOT EXISTS (
    SELECT 1 FROM forum_sujets f
    WHERE f.titre LIKE s.titre || '%' AND f.date_creation = s.date_creation
);

-- ---------------------------------------------------------------------
-- 3. Reprendre les messages, rattachés à leur sujet par la date de
--    création du sujet — seule clé commune après la reprise ci-dessus.
--    L'auteur redevient NULL pour la même raison ; son nom est préservé en
--    tête du contenu, sans quoi l'information disparaîtrait purement et
--    simplement du fil de discussion.
-- ---------------------------------------------------------------------
INSERT INTO forum_messages (sujet_id, auteur_id, contenu, date_creation,
                            piece_jointe_nom_original, piece_jointe_nom_stocke, piece_jointe_type_contenu)
SELECT
    f.id,
    NULL,
    CASE
        WHEN m.auteur_nom IS NOT NULL AND m.auteur_nom <> ''
            THEN m.auteur_nom || ' : ' || m.contenu
        ELSE m.contenu
    END,
    m.date_creation,
    m.piece_jointe_nom_original,
    m.piece_jointe_nom_stocke,
    m.piece_jointe_type_contenu
FROM message m
JOIN sujet s ON s.id = m.sujet_id
JOIN forum_sujets f ON f.date_creation = s.date_creation
WHERE NOT EXISTS (
    SELECT 1 FROM forum_messages fm
    WHERE fm.sujet_id = f.id AND fm.date_creation = m.date_creation
);

-- ---------------------------------------------------------------------
-- 4. Supprimer l'ancien modèle
--    message d'abord : sa clé étrangère pointe vers sujet.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS sujet;

-- =====================================================================
-- Les URL de l'API ne changent pas. EspaceEtudiantController continue
-- d'exposer /sujets et /formateur/sujets/{id}/… avec la même forme de
-- réponse (SujetDto), mais alimentée par forum_sujets : aucun écran
-- Angular n'a besoin d'être modifié, et les deux rôles voient désormais
-- les mêmes discussions.
-- =====================================================================
