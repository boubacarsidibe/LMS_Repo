-- Lieu / lien de connexion d'un test académique (LOG_BLOQUANT.md, point 63 :
-- retour utilisateur sur les 9 demandes du 2026-09-02, point 7 "page dédiée
-- à la création de test"). Entretien porte déjà ce champ (lieu_ou_lien) ;
-- TestAcademique ne l'a jamais eu, alors que le TDR exige que lieu, date,
-- heure et session soient précisés clairement à la convocation.
ALTER TABLE public.tests_academiques ADD COLUMN lieu_ou_lien VARCHAR(255);
