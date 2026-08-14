export interface Cours {
  id: number;
  code: string;
  titre: string;
  description?: string;
  semestre?: string;
  volumeHoraire?: number;
  nbEtudiants: number;
  nbRessources: number;
  progression: number;
  couleurBandeau: string;
  imageUrl?: string;
}

export interface CoursCreation {
  code: string;
  titre: string;
  semestre: string;
  description?: string;
  volumeHoraire?: number;
}
