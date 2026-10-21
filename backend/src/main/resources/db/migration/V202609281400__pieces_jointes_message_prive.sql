-- Pièce jointe optionnelle sur un message de la messagerie privée (apprenant ↔
-- formateur), même principe que forum_messages.piece_jointe_* (V1__baseline) :
-- trois colonnes nullables, jamais renseignées ensemble à moitié.
ALTER TABLE message_prive
    ADD COLUMN piece_jointe_nom_original VARCHAR(255),
    ADD COLUMN piece_jointe_nom_stocke VARCHAR(255),
    ADD COLUMN piece_jointe_type_contenu VARCHAR(255);
