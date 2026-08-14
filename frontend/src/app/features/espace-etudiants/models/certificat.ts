export interface Certificat {
  id: number;
  reference: string;
  etudiantId: number;
  etudiantNom: string;
  etudiantMatricule: string | null;
  intituleFormation: string;
  cohorteLibelle: string | null;
  dateEmission: string;
  formateurNom: string;
}

export interface CertificatCreation {
  etudiantId: number;
  intituleFormation: string;
  cohorteId: number | null;
}
