export type TypeDemandeEntreprise = 'BESOIN_FORMATION' | 'DEVIS_SESSION';

export type StatutDemandeEntreprise = 'SOUMISE' | 'EN_COURS' | 'REPONDUE' | 'CLOTUREE';

export type StatutFicheModule = 'BROUILLON' | 'TRANSMISE' | 'ACCEPTEE' | 'AJUSTEMENTS_DEMANDES';

export const LIBELLES_TYPE_DEMANDE: Record<TypeDemandeEntreprise, string> = {
  BESOIN_FORMATION: 'Besoin de formation',
  DEVIS_SESSION: 'Devis pour une session sur mesure',
};

export const LIBELLES_STATUT_DEMANDE_ENTREPRISE: Record<StatutDemandeEntreprise, string> = {
  SOUMISE: 'Soumise',
  EN_COURS: 'En cours de traitement',
  REPONDUE: 'Répondue',
  CLOTUREE: 'Clôturée',
};

export const LIBELLES_STATUT_FICHE_MODULE: Record<StatutFicheModule, string> = {
  BROUILLON: 'Brouillon',
  TRANSMISE: 'En attente de votre décision',
  ACCEPTEE: 'Acceptée',
  AJUSTEMENTS_DEMANDES: 'Ajustements demandés',
};

export interface FormationCatalogue {
  id: number;
  intitule: string;
  description: string;
  domaine: string;
}

export interface EntrepriseCoordonnees {
  raisonSociale: string;
  secteur: string | null;
  adresse: string;
  telephone: string;
}

export interface DemandeEntrepriseSaisie {
  entreprise: EntrepriseCoordonnees;
  type: TypeDemandeEntreprise;
  formationId: number | null;
  description: string;
  effectifConcerne: number | null;
  datesSouhaitees: string | null;
}

export interface DemandeEntreprise {
  id: number;
  type: TypeDemandeEntreprise;
  statut: StatutDemandeEntreprise;
  entrepriseId: number;
  raisonSociale: string;
  formationId: number | null;
  intituleFormation: string | null;
  description: string;
  effectifConcerne: number | null;
  datesSouhaitees: string | null;
  dateSoumission: string;
  dateReponse: string | null;
  reponseEquipe: string | null;
  /** Statut de la fiche technique rattachée, ou null si aucune n'a encore été transmise. */
  ficheStatut: StatutFicheModule | null;
}

/** Une journée du planning (§4 du gabarit). */
export interface FicheModuleJour {
  id: number | null;
  numero: number;
  contenus: string | null;
  dureeHeures: number | null;
  typeEnseignement: string | null;
  modalites: string | null;
}

/** Fiche Technique de Module transmise par la pédagogie (jamais un brouillon, côté entreprise). */
export interface FicheTechniqueModule {
  id: number;
  demandeId: number;
  statut: StatutFicheModule;
  dateTransmission: string | null;
  commentaireEntreprise: string | null;
  dateDecisionEntreprise: string | null;
  code: string | null;
  intitule: string | null;
  version: string | null;
  dateRevision: string | null;
  responsableModule: string | null;
  departement: string | null;
  atelierService: string | null;
  fonctionsCibles: string | null;
  thematiques: string | null;
  prerequis: string | null;
  langue: string | null;
  niveau: string | null;
  effectif: number | null;
  dureeHeures: number | null;
  dureeJours: number | null;
  periodeProbable: string | null;
  objectifGeneral: string | null;
  objectifsSpecifiques: string | null;
  modalitesTestPrerequis: string | null;
  formationSurSite: string | null;
  typesEvaluation: string | null;
  ressourcesPedagogiques: string | null;
  bibliographie: string | null;
  profilFormateur: string | null;
  coutTotal: string | null;
  modalitesFinancieres: string | null;
  jours: FicheModuleJour[];
}

export interface MessageEntreprise {
  id: number;
  entrepriseId: number;
  auteurId: number;
  auteurNom: string;
  auteurEntreprise: boolean;
  contenu: string;
  dateCreation: string;
}
