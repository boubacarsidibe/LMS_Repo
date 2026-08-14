export interface Evenement {
  id: number;
  titre: string;
  date: Date;
  heure: string;
  type: 'cours' | 'echeance' | 'tutorat';
  lienVisio?: string;
}

export interface EvenementCreation {
  titre: string;
  date: string;
  heure: string;
  type: 'cours' | 'echeance' | 'tutorat';
  lienVisio?: string;
}
