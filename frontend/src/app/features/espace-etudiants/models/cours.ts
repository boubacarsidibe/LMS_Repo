import { Cohorte } from './cohorte';

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
  /** Promotions rattachées, telles que nommées par CoursDto. Le champ s'appelait
   *  « cohortes » et ne correspondait donc a rien : il valait toujours undefined. */
  promotions: Cohorte[];
  imageUrl?: string;
}

export interface CoursCreation {
  code: string;
  titre: string;
  semestre: string;
  description?: string;
  volumeHoraire?: number;
  cohorteIds?: number[];
}
