export type ModaliteRendezVous = 'EN_LIGNE' | 'PRESENTIEL';

export const LIBELLES_MODALITE: Record<ModaliteRendezVous, string> = {
  EN_LIGNE: 'En ligne',
  PRESENTIEL: 'En présentiel',
};

export type StatutRendezVous = 'EN_ATTENTE' | 'ACCEPTE' | 'REFUSE' | 'PROPOSITION_AUTRE_DATE' | 'ANNULE';

export const LIBELLES_STATUT: Record<StatutRendezVous, string> = {
  EN_ATTENTE: 'En attente de réponse',
  ACCEPTE: 'Accepté',
  REFUSE: 'Refusé',
  PROPOSITION_AUTRE_DATE: 'Autre date proposée',
  ANNULE: 'Annulé',
};

export interface PersonnelProfil {
  id: number;
  nomAffiche: string;
  role: string;
}

export interface Creneau {
  id: number;
  personnelId: number;
  personnelNom: string;
  dateHeureDebut: string;
  dateHeureFin: string;
  reserve: boolean;
}

export interface RendezVous {
  id: number;
  creneauId: number;
  dateHeureDebut: string;
  dateHeureFin: string;
  personnelId: number;
  personnelNom: string;
  demandeurId: number;
  demandeurNom: string;
  modalite: ModaliteRendezVous;
  lieuOuLien: string | null;
  motif: string | null;
  statut: StatutRendezVous;
  dateHeureProposeeDebut: string | null;
  dateHeureProposeeFin: string | null;
  commentaireReponse: string | null;
  dateCreation: string;
  dateReponse: string | null;
}

export interface DemandeRendezVousSaisie {
  creneauId: number | null;
  modalite: ModaliteRendezVous;
  lieuOuLien: string;
  motif: string;
}
