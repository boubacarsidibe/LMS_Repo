export interface CertificatVerification {
  reference: string;
  etudiantNomComplet: string;
  intituleFormation: string;
  dateEmission: string | null;
  formateurNom: string;
  verifiable: boolean;
  valide: boolean;
}
