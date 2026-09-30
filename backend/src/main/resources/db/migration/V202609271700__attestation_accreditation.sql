-- Attestation d'accréditation téléchargeable et vérifiable publiquement, sur le même principe que le
-- certificat de formation (V202609251300) : une référence opaque (imprimée + encodée dans un QR) et une
-- signature HMAC calculée sur le contenu figé de la décision, pour détecter une donnée modifiée après coup.
--
-- Générées seulement pour une demande ACCREDITE ou ACCREDITE_SOUS_RESERVE (jamais pour SOUMISE,
-- EN_COURS_EVALUATION ou REJETE) : nullables, remplies par le serveur au moment de la décision.
--
-- La référence doit rester imprévisible : contrairement à `id` (entier séquentiel), elle ne doit pas
-- permettre d'énumérer les demandes via l'endpoint de vérification publique — c'est cette contrainte qui
-- impose une colonne dédiée plutôt que de vérifier directement par id.

ALTER TABLE public.demandes_accreditation
    ADD COLUMN reference_attestation VARCHAR(60) UNIQUE,
    ADD COLUMN signature_attestation VARCHAR(64);
