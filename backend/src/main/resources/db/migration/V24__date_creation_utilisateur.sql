-- Isolation temporelle : un compte cree apres la publication d'une annonce ou
-- l'ouverture d'un fil de forum ne doit pas se retrouver submerge par tout
-- l'historique anterieur a son inscription. Sans cette colonne, il n'existe
-- aucun moyen de distinguer "publie avant que j'existe" de "publie apres".
--
-- Les comptes deja presents recoivent NOW() par defaut : ils voient donc tout
-- l'historique existant, ce qui est le comportement actuel et correct pour eux.
ALTER TABLE users ADD COLUMN date_creation TIMESTAMP NOT NULL DEFAULT NOW();
