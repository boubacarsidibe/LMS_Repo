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
}

export const ETUDIANTS_MOCK: Etudiant[] = [
  {
    id: 1,
    nom: 'Marc-Antoine Dupont',
    cohorte: "Master 2 - Éco. Énergie",
    derniereConnexion: 'Il y a 6 jours',
    progression: 34,
    noteMoyenne: 10.2,
    statut: 'En retard',
    initiales: 'MD',
    couleurAvatar: 'bg-primary-container'
  },
  {
    id: 2,
    nom: 'Sophie Lefebvre',
    cohorte: "Master 2 - Éco. Énergie",
    derniereConnexion: "Aujourd'hui",
    progression: 82,
    noteMoyenne: 16.8,
    statut: 'Actif',
    initiales: 'SL',
    couleurAvatar: 'bg-secondary'
  },
  {
    id: 3,
    nom: 'Jean-Luc Moreau',
    cohorte: 'Licence 3 - Gestion',
    derniereConnexion: 'Hier',
    progression: 65,
    noteMoyenne: 13.5,
    statut: 'À risque',
    initiales: 'JL',
    couleurAvatar: 'bg-primary-container'
  }
];