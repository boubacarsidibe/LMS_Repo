export interface Etudiant {
  id: number;
  nom: string;
  cohorte: string;
  derniereConnexion: string;
  progression: number;
  noteMoyenne: number;
  statut: 'Actif' | 'À risque' | 'En retard';
  initiales: string;
  couleurAvatar: string;
  matricule?: string;
}
