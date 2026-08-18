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
  /** Programmes du cours. Vide = aucune promotion ne le reçoit, aucun étudiant ne le voit. */
  formations?: { id: number; code: string; intitule: string }[];
  /** Équipe pédagogique. Vide = le cours n'apparaît dans aucun espace formateur. */
  formateurs?: { id: number; nomComplet: string }[];
  imageUrl?: string;
}

export interface CoursCreation {
  code: string;
  titre: string;
  semestre: string;
  description?: string;
  volumeHoraire?: number;
  /** Voie de l'espace formateur, qui raisonne en promotions. */
  cohorteIds?: number[];
  /**
   * Voie de l'espace pédagogique, qui pilote l'offre et désigne la formation.
   * Absent = ne pas toucher au programme ; tableau vide = le retirer partout.
   */
  formationIds?: number[];
}
