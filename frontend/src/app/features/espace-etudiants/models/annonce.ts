import { Cohorte } from './cohorte';

export interface Annonce {
  id: number;
  titre: string;
  contenu: string;
  auteur: string;
  date: string;
  epinglee: boolean;
  cohortes: Cohorte[];
}

export interface AnnonceCreation {
  titre: string;
  contenu: string;
  epinglee: boolean;
  cohorteIds?: number[];
}
