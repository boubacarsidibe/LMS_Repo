export type StatutDemandeAccreditation = 'SOUMISE' | 'EN_COURS_EVALUATION' | 'ACCREDITE' | 'REJETE';

export type TypeDocumentAccreditation =
  | 'CURRICULUM'
  | 'POLITIQUE_QUALITE'
  | 'PROGRAMME_FORMATION'
  | 'QUALIFICATIONS_FORMATEURS'
  | 'EQUIPEMENTS_DISPONIBLES'
  | 'AUTRE';

export const LIBELLES_TYPE_DOCUMENT: Record<TypeDocumentAccreditation, string> = {
  CURRICULUM: 'Curriculum',
  POLITIQUE_QUALITE: 'Politique qualité',
  PROGRAMME_FORMATION: 'Programme de formation',
  QUALIFICATIONS_FORMATEURS: 'Qualifications des formateurs',
  EQUIPEMENTS_DISPONIBLES: 'Équipements disponibles',
  AUTRE: 'Autre document',
};

export const LIBELLES_STATUT_DEMANDE: Record<StatutDemandeAccreditation, string> = {
  SOUMISE: 'Soumise',
  EN_COURS_EVALUATION: "En cours d'évaluation",
  ACCREDITE: 'Accréditée',
  REJETE: 'Rejetée',
};

export interface PieceAccreditation {
  id: number;
  typeDocument: TypeDocumentAccreditation;
  nomFichierOriginal: string | null;
}

export interface DemandeAccreditation {
  id: number;
  statut: StatutDemandeAccreditation;
  dateSoumission: string;
  dateDecision: string | null;
  motifRejet: string | null;
  commentaireEvaluation: string | null;
  ecoleId: number;
  nomEcole: string;
  pieces: PieceAccreditation[];
}

export interface DemandeAccreditationSaisie {
  nomEcole: string;
  adresse: string;
  telephone: string;
  siteWeb: string | null;
}

export interface EcoleAccreditee {
  nomEcole: string;
  dateAccreditation: string;
}

export interface CritereAccreditation {
  id: number;
  libelle: string;
  description: string | null;
  actif: boolean;
}
