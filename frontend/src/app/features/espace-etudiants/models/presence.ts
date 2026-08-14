export interface PresenceEtudiant {
  etudiantId: number;
  nom: string;
  initiales: string;
  couleurAvatar: string;
  /** null = pas encore pointé pour cette séance. */
  present: boolean | null;
}

export interface PresenceHistorique {
  id: number;
  date: string;
  coursCode: string;
  coursTitre: string;
  seanceTitre: string;
  present: boolean;
}
