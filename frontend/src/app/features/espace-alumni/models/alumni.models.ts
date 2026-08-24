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

export interface SituationProfessionnelle {
  id: number;
  intitulePoste: string;
  entreprise: string;
  dateDebut: string;
  dateFin: string | null;
}

export interface SituationProfessionnelleSaisie {
  intitulePoste: string;
  entreprise: string;
  dateDebut: string;
  dateFin: string | null;
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
