-- Permet de décrire une pièce justificative (notamment : à quel diplôme/certificat
-- correspond le document), pour les diplômes ajoutés en nombre libre par le candidat.
ALTER TABLE pieces_justificatives ADD COLUMN description VARCHAR(500);
