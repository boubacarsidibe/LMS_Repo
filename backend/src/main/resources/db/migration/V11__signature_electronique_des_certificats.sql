-- Signature electronique des certificats (Epic 3 - Certificats securises).
--
-- Le certificat portait deja un QR code et un filigrane, mais le QR encodait un
-- simple texte et le "cachet" appose sur le PDF n'etait qu'un dessin vectoriel :
-- rien ne permettait de verifier qu'un certificat presente n'avait pas ete
-- modifie. Cette colonne stocke un HMAC-SHA256 (hexadecimal, 64 caracteres) du
-- contenu du certificat, calcule a la delivrance par CertificatSignatureService.
--
-- Nullable : les certificats emis avant cette migration n'ont pas de signature
-- retroactive (ils ne seraient pas reconstructibles a l'identique) et la
-- verification publique les traite comme "non verifiables", pas comme invalides.

ALTER TABLE public.certificat
    ADD COLUMN signature VARCHAR(64);
