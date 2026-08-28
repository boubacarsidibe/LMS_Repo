export interface AlumniProfil {
  id: number;
  nom: string | null;
  prenom: string | null;
  matricule: string | null;
  promotionLibelle: string | null;
  posteActuel: string | null;
  entreprise: string | null;
  secteurActivite: string | null;
  visible: boolean;
  dateCreation: string;
}

export interface AlumniProfilUpdate {
  posteActuel: string | null;
  entreprise: string | null;
  secteurActivite: string | null;
  visible: boolean;
}

/** Temps forts du parcours de l'alumni connecté — profil enrichi. */
export interface AlumniMetriques {
  anneesDepuisDiplome: number;
  promotionLibelle: string | null;
  moyenneGeneraleDernierReleve: number | null;
  periodeDernierReleve: string | null;
  nombreRelevesDelivres: number;
  nombrePostesParcours: number;
  posteActuelRenseigne: boolean;
  nombreSujetsForumAlumni: number;
  nombreMessagesForumAlumni: number;
  nombreDocumentsPartages: number;
  nombreEnquetesRepondues: number;
  nombreEnquetesActives: number;
}

/** Nature de la situation, distincte de dateFin === null qui ne dit que « en cours ». */
export type StatutEmploi = 'EN_EMPLOI' | 'RECHERCHE_EMPLOI' | 'ENTREPRENARIAT' | 'POURSUITE_ETUDES' | 'AUTRE';

export interface SituationProfessionnelle {
  id: number;
  intitulePoste: string;
  entreprise: string;
  dateDebut: string;
  dateFin: string | null;
  statutEmploi: StatutEmploi;
}

export interface SituationProfessionnelleSaisie {
  intitulePoste: string;
  entreprise: string;
  dateDebut: string;
  dateFin: string | null;
  statutEmploi: StatutEmploi;
}

/** Identique au ForumSujet de l'espace étudiant : même entité, même DTO, portée alumni. */
export interface ForumSujet {
  id: number;
  titre: string;
  coursId: number | null;
  coursTitre: string | null;
  auteur: string;
  dateCreation: string;
  epingle: boolean;
  clos: boolean;
  nbMessages: number;
  dateDernierMessage: string;
}

export interface ForumMessage {
  id: number;
  auteur: string;
  contenu: string;
  dateCreation: string;
  estAuteur: boolean;
}

export interface FilForum {
  sujet: ForumSujet;
  messages: ForumMessage[];
}

/** Personne citable dans un fil du forum alumni. */
export interface Mentionnable {
  id: number;
  nom: string;
  role: string;
}

/** Document partagé entre alumni. */
export interface DocumentPartage {
  id: number;
  titre: string;
  description: string | null;
  auteur: string;
  nomFichierOriginal: string;
  dateAjout: string;
  estAuteur: boolean;
}

/** Campagne d'enquête de suivi (TDR §G) : mesure l'impact de la formation sur la carrière. */
export interface EnqueteSuivi {
  id: number;
  titre: string;
  description: string | null;
  dateCreation: string;
  active: boolean;
  dejaRepondu: boolean;
}

export interface ReponseEnqueteSuiviSaisie {
  noteApplicationCompetences: number;
  noteImpactCarriere: number;
  commentaire: string | null;
}

/** Identique au relevé délivré de l'espace étudiant : même document, même DTO. */
export interface ReleveDelivre {
  id: number;
  reference: string;
  etudiantNom: string;
  matricule: string | null;
  promotionLibelle: string | null;
  periode: string;
  dateEmission: string;
  delivrePar: string;
  moyenneGenerale: number | null;
  nombreNotes: number;
  lignes: { coursCode: string; coursTitre: string; coefficient: number; moyenne: number | null }[];
}
