-- Retrait de l'entite Evenement : aucune colonne cours/formateur/promotion ne la
-- rattachait a quoi que ce soit, si bien que n'importe quel formateur pouvait
-- consulter, modifier ou supprimer le calendrier de n'importe quel autre
-- (GET/POST/PATCH /api/espace-etudiant/evenements, sans controle de perimetre).
--
-- Le systeme "seance" (rattache a cours, migration anterieure) la remplace deja
-- entierement : creation/modification/suppression cote formateur bornees a ses
-- propres cours, et un calendrier etudiant qui derive ses seances des cours
-- autorises (GET /api/scolarite/mon-calendrier).
DROP TABLE IF EXISTS evenement;
