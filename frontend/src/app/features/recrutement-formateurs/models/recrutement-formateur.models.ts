export type StatutOffreFormateur = 'BROUILLON' | 'PUBLIEE' | 'CLOTUREE';

export type TypeContratFormateur = 'VACATAIRE' | 'CDD' | 'CDI' | 'PRESTATION';

export const LIBELLES_TYPE_CONTRAT: Record<TypeContratFormateur, string> = {
  VACATAIRE: 'Vacation',
  CDD: 'CDD',
  CDI: 'CDI',
  PRESTATION: 'Prestation',
};

export type StatutCandidatureFormateur = 'DOSSIER_RECU' | 'EN_COURS_DE_TRAITEMENT' | 'TERMINE';

export const LIBELLES_STATUT_CANDIDATURE: Record<StatutCandidatureFormateur, string> = {
  DOSSIER_RECU: 'Dossier reçu',
  EN_COURS_DE_TRAITEMENT: 'En cours de traitement',
  TERMINE: 'Terminé',
};

export type DecisionCandidatureFormateur = 'RETENU' | 'NON_RETENU';

export const LIBELLES_DECISION: Record<DecisionCandidatureFormateur, string> = {
  RETENU: 'Retenu',
  NON_RETENU: 'Non retenu',
};

export type TypeDocumentCandidatureFormateur =
  | 'CV'
  | 'DIPLOME'
  | 'ATTESTATION_EXPERIENCE'
  | 'PIECE_IDENTITE'
  | 'AUTRE';

export const LIBELLES_TYPE_DOCUMENT: Record<TypeDocumentCandidatureFormateur, string> = {
  CV: 'Curriculum vitae',
  DIPLOME: 'Diplôme',
  ATTESTATION_EXPERIENCE: "Attestation d'expérience",
  PIECE_IDENTITE: "Pièce d'identité",
  AUTRE: 'Autre document',
};

/** Pièces exigées de tout dossier. */
export const TYPES_DOCUMENT_REQUIS: TypeDocumentCandidatureFormateur[] = ['CV', 'DIPLOME'];

/** Pièces recommandées, non bloquantes au dépôt. */
export const TYPES_DOCUMENT_RECOMMANDES: TypeDocumentCandidatureFormateur[] = [
  'ATTESTATION_EXPERIENCE',
  'PIECE_IDENTITE',
];

export interface OffreFormateur {
  id: number;
  intitule: string;
  domaine: string | null;
  descriptionMissions: string | null;
  profilRecherche: string | null;
  typeContrat: TypeContratFormateur;
  lieu: string | null;
  nbPostes: number;
  formationId: number | null;
  intituleFormation: string | null;
  datePublication: string | null;
  dateLimiteCandidature: string | null;
  statut: StatutOffreFormateur;
  dateCreation: string;
  nombreCandidatures: number;
}

export interface PieceCandidatureFormateur {
  id: number;
  typeDocument: TypeDocumentCandidatureFormateur;
  nomFichierOriginal: string | null;
}

export interface CandidatureFormateur {
  id: number;
  offreId: number | null;
  intituleOffre: string | null;
  typeContratOffre: TypeContratFormateur | null;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  diplomePrincipal: string | null;
  anneesExperience: number | null;
  lettreMotivation: string | null;
  statut: StatutCandidatureFormateur;
  decision: DecisionCandidatureFormateur | null;
  motifDecision: string | null;
  dateSoumission: string;
  dateDecision: string | null;
  formateurCree: boolean;
  pieces: PieceCandidatureFormateur[];
}

export interface CandidatureFormateurSaisie {
  offreId: number;
  nom: string;
  prenom: string;
  telephone: string;
  diplomePrincipal: string | null;
  anneesExperience: number | null;
  lettreMotivation: string | null;
}
