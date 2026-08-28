-- Permet de repondre a un message precis du forum (citation façon WhatsApp),
-- au lieu de ne pouvoir repondre qu'au fil entier.
ALTER TABLE forum_messages ADD COLUMN reponse_a_id BIGINT;
ALTER TABLE forum_messages
    ADD CONSTRAINT fk_forum_messages_reponse_a
    FOREIGN KEY (reponse_a_id) REFERENCES forum_messages (id) ON DELETE SET NULL;
