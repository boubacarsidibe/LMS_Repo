export interface Certificat {
  id: number;
  reference: string;
  etudiantId: number;
  etudiantNom: string;
  etudiantMatricule: string | null;
  intituleFormation: string;
  promotionLibelle: string | null;
  dateEmission: string;
  formateurNom: string;
  civilite: string | null;
  dateNaissance: string | null;
  dateDebut: string | null;
  dateFin: string | null;
  codeCertificat: string | null;
}
