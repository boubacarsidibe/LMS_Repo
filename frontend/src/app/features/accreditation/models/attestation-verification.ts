export interface AttestationAccreditationVerification {
  reference: string;
  nomEcole: string;
  intituleProgramme: string | null;
  niveauProgramme: string | null;
  dateDecision: string | null;
  dateExpiration: string | null;
  sousReserve: boolean;
  verifiable: boolean;
  valide: boolean;
}
