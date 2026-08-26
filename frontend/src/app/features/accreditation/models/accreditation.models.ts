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

export type AvisValeur = 'FAVORABLE' | 'DEFAVORABLE';

export const LIBELLES_AVIS: Record<AvisValeur, string> = {
  FAVORABLE: 'Favorable',
  DEFAVORABLE: 'Défavorable',
};

/** Avis d'un évaluateur sur une demande (TDR §F, décision collégiale). */
export interface AvisAccreditation {
  id: number;
  evaluateurId: number;
  evaluateurNom: string;
  avis: AvisValeur;
  commentaire: string | null;
  dateAvis: string;
}

/**
 * Demande telle que vue par l'espace évaluateur : porte en plus les avis déjà
 * déposés par l'ensemble des évaluateurs. Distincte de {@link DemandeAccreditation},
 * partagée avec l'espace école — une école ne doit pas voir les avis individuels
 * avant la décision finale.
 */
export interface DemandeAccreditationEvaluateur extends DemandeAccreditation {
  avis: AvisAccreditation[];
}
