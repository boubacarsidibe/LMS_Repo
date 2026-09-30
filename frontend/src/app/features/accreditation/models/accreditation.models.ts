export type StatutDemandeAccreditation =
  | 'SOUMISE'
  | 'EN_COURS_EVALUATION'
  | 'ACCREDITE'
  | 'ACCREDITE_SOUS_RESERVE'
  | 'REJETE';

export const LIBELLES_STATUT_DEMANDE: Record<StatutDemandeAccreditation, string> = {
  SOUMISE: 'Soumise',
  EN_COURS_EVALUATION: "En cours d'évaluation",
  ACCREDITE: 'Accréditée',
  ACCREDITE_SOUS_RESERVE: 'Accréditée sous réserve',
  REJETE: 'Rejetée',
};

/** Décisions favorables (Cadre de référence DRCQ §IV.8 : accréditation pleine ou conditionnelle). */
export const STATUTS_FAVORABLES: StatutDemandeAccreditation[] = ['ACCREDITE', 'ACCREDITE_SOUS_RESERVE'];

export type TypeDocumentAccreditation =
  | 'CURRICULUM'
  | 'POLITIQUE_QUALITE'
  | 'PROGRAMME_FORMATION'
  | 'QUALIFICATIONS_FORMATEURS'
  | 'EQUIPEMENTS_DISPONIBLES'
  | 'ENGAGEMENT_REFERENTIEL'
  | 'PREUVE_RECONNAISSANCE'
  | 'RAPPORT_AUTO_EVALUATION'
  | 'ORGANIGRAMME'
  | 'CV_CERTIFICATIONS_FORMATEURS'
  | 'RESULTATS_COHORTES_PRECEDENTES'
  | 'REGLEMENT_INTERIEUR'
  | 'AUTRE';

export const LIBELLES_TYPE_DOCUMENT: Record<TypeDocumentAccreditation, string> = {
  CURRICULUM: 'Curriculum',
  POLITIQUE_QUALITE: 'Politique qualité',
  PROGRAMME_FORMATION: 'Programme de formation',
  QUALIFICATIONS_FORMATEURS: 'Qualifications des formateurs',
  EQUIPEMENTS_DISPONIBLES: 'Équipements disponibles',
  ENGAGEMENT_REFERENTIEL: 'Engagement à respecter le référentiel INPG',
  PREUVE_RECONNAISSANCE: "Preuve de reconnaissance officielle (statut légal / ANAQ-SUP)",
  RAPPORT_AUTO_EVALUATION: "Rapport d'auto-évaluation du programme",
  ORGANIGRAMME: 'Organigramme de l’établissement',
  CV_CERTIFICATIONS_FORMATEURS: 'CV et certifications des formateurs',
  RESULTATS_COHORTES_PRECEDENTES: 'Résultats des cohortes précédentes',
  REGLEMENT_INTERIEUR: 'Règlement intérieur',
  AUTRE: 'Autre document',
};

/**
 * Pièces exigées de tout dossier (TDR §F + Cadre de référence DRCQ §IV.1).
 * « AUTRE » reste facultatif et multiple.
 */
export const TYPES_DOCUMENT_REQUIS: TypeDocumentAccreditation[] = [
  'CURRICULUM',
  'POLITIQUE_QUALITE',
  'PROGRAMME_FORMATION',
  'QUALIFICATIONS_FORMATEURS',
  'EQUIPEMENTS_DISPONIBLES',
  'ENGAGEMENT_REFERENTIEL',
  'PREUVE_RECONNAISSANCE',
  'RAPPORT_AUTO_EVALUATION',
];

/** Pièces attendues à la visite du comité d'experts (Cadre §IV.4), non bloquantes au dépôt. */
export const TYPES_DOCUMENT_RECOMMANDES: TypeDocumentAccreditation[] = [
  'ORGANIGRAMME',
  'CV_CERTIFICATIONS_FORMATEURS',
  'RESULTATS_COHORTES_PRECEDENTES',
  'REGLEMENT_INTERIEUR',
];

export type NiveauProgramme =
  | 'TECHNICIEN'
  | 'INGENIEUR'
  | 'LICENCE'
  | 'MASTER'
  | 'MASTERE_SPECIALISE'
  | 'CERTIFICAT'
  | 'AUTRE';

export const LIBELLES_NIVEAU_PROGRAMME: Record<NiveauProgramme, string> = {
  TECHNICIEN: 'Technicien',
  INGENIEUR: 'Ingénieur',
  LICENCE: 'Licence',
  MASTER: 'Master',
  MASTERE_SPECIALISE: 'Mastère spécialisé',
  CERTIFICAT: 'Certificat / qualification',
  AUTRE: 'Autre',
};

export type TypeStructure =
  | 'UNIVERSITE_PUBLIQUE'
  | 'ECOLE_INGENIEURS'
  | 'ECOLE_PRIVEE_RECONNUE'
  | 'CENTRE_TECHNIQUE_PRO';

export const LIBELLES_TYPE_STRUCTURE: Record<TypeStructure, string> = {
  UNIVERSITE_PUBLIQUE: 'Université publique',
  ECOLE_INGENIEURS: "École d'ingénieurs",
  ECOLE_PRIVEE_RECONNUE: 'École privée reconnue (référencée ANAQ-SUP)',
  CENTRE_TECHNIQUE_PRO: 'Centre de formation technique et professionnel',
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
  dateExpiration: string | null;
  motifRejet: string | null;
  commentaireEvaluation: string | null;
  planActions: string | null;
  ecoleId: number;
  nomEcole: string;
  intituleProgramme: string | null;
  niveauProgramme: NiveauProgramme | null;
  typeStructure: TypeStructure | null;
  effectifCohorte: number | null;
  dateDebutCohorte: string | null;
  pieces: PieceAccreditation[];
}

export interface DemandeAccreditationSaisie {
  nomEcole: string;
  adresse: string;
  telephone: string;
  siteWeb: string | null;
  intituleProgramme: string;
  niveauProgramme: NiveauProgramme;
  typeStructure: TypeStructure;
  effectifCohorte: number | null;
  dateDebutCohorte: string | null;
}

export interface EcoleAccreditee {
  nomEcole: string;
  intituleProgramme: string | null;
  niveauProgramme: NiveauProgramme | null;
  dateAccreditation: string;
  dateExpiration: string | null;
  sousReserve: boolean;
  /** Nul seulement pour une accréditation décidée avant l'attestation téléchargeable. */
  referenceAttestation: string | null;
}

export interface CritereAccreditation {
  id: number;
  code: string | null;
  section: string | null;
  libelle: string;
  description: string | null;
  ordre: number;
  actif: boolean;
}

export type AvisValeur = 'FAVORABLE' | 'DEFAVORABLE';

export const LIBELLES_AVIS: Record<AvisValeur, string> = {
  FAVORABLE: 'Favorable',
  DEFAVORABLE: 'Défavorable',
};

export type AppreciationCritere = 'CONFORME' | 'NON_CONFORME' | 'SANS_OBJET';

export const LIBELLES_APPRECIATION: Record<AppreciationCritere, string> = {
  CONFORME: 'Conforme',
  NON_CONFORME: 'Non conforme',
  SANS_OBJET: 'Sans objet',
};

/** Jugement de l'évaluateur connecté sur un critère du référentiel, pour un dossier. */
export interface EvaluationCritere {
  critereId: number;
  critereLibelle: string;
  critereDescription: string | null;
  appreciation: AppreciationCritere;
  commentaire: string | null;
  dateEvaluation: string;
}

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
 * déposés par l'ensemble des évaluateurs et le référentiel appliqué au dossier.
 * Distincte de {@link DemandeAccreditation}, partagée avec l'espace école — une
 * école ne doit pas voir les avis individuels avant la décision finale.
 */
export interface DemandeAccreditationEvaluateur extends DemandeAccreditation {
  adresse: string;
  telephone: string;
  siteWeb: string | null;
  emailEcole: string | null;
  avis: AvisAccreditation[];
  /** Référentiel de critères actifs, à appliquer au dossier (trié par ordre). */
  criteresActifs: CritereAccreditation[];
  /** Grille de l'évaluateur connecté (une entrée par critère déjà jugé). */
  maGrille: EvaluationCritere[];
}
