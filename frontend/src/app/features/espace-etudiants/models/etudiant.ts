export interface Etudiant {
  id: number;
  nom: string;
  cohorte: string;
  derniereConnexion: string;
  progression: number;
  /** null tant qu'aucune note n'est saisie — a ne pas confondre avec un zero. */
  noteMoyenne: number | null;
  /** Renseigné uniquement côté pédagogique : le formateur enseigne, il n'inscrit pas. */
  origine?: string | null;
  statut: 'Actif' | 'À risque' | 'En retard';
  initiales: string;
  couleurAvatar: string;
  matricule?: string;
}
