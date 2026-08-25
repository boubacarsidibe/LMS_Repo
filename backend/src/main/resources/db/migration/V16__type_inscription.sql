-- Distinguer, des l'inscription publique, un futur candidat d'un representant
-- d'ecole venant deposer une demande d'accreditation (demande utilisateur du
-- 2026-08-25 : les deux profils passaient par un formulaire d'inscription
-- identique, puis atterrissaient tous deux sur l'accueil generique, sans
-- aucune indication sur la suite a donner).
--
-- Ce champ n'attribue aucun role : ROLE_CANDIDAT et ROLE_ECOLE_FORMATION
-- restent accordes uniquement a la soumission reelle (CandidatController,
-- EcoleAccreditationController) - decision deja actee et non remise en cause
-- ici. type_inscription ne sert qu'a orienter l'utilisateur juste apres son
-- inscription (voir Register côté frontend).
--
-- DEFAULT 'CANDIDAT' pour les comptes deja crees : c'est le cas majoritaire
-- historique, et un compte ecole existant a de toute facon deja son role
-- ROLE_ECOLE_FORMATION reel, non affecte par cette colonne.
ALTER TABLE public.users
    ADD COLUMN type_inscription VARCHAR(20) NOT NULL DEFAULT 'CANDIDAT';
