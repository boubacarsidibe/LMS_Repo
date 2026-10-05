export interface CertificatVerification {
  reference: string;
  etudiantNomComplet: string;
  intituleFormation: string;
  /** « Du 02 au 06 février 2026 » ; absente des certificats délivrés avant le modèle INPG. */
  periode: string | null;
  dateEmission: string | null;
  formateurNom: string;
  /** Code imprimé en bas du certificat ; absent des certificats délivrés avant le modèle INPG. */
  codeCertificat: string | null;
  verifiable: boolean;
  valide: boolean;
}
