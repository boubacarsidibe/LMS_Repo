export interface Etudiant {
  id: number;
  nom: string;
  promotion: string;
  /** Date du dernier support ouvert, ou « Aucune activité » : la connexion elle-même n'est pas tracée. */
  derniereActivite: string;
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
