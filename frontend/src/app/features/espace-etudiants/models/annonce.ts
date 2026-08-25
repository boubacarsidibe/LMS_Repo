/**
 * Communication de l'INPG, telle que renvoyee par AnnonceDto.
 *
 * <p>Les noms suivent exactement ceux du DTO. Ils en divergeaient auparavant —
 * « date » pour « datePublication », un tableau « cohortes » pour la chaine
 * « promotionLibelle » — si bien que la date restait vide et que la lecture de
 * cohortes.length echouait : la carte n'affichait plus que ses separateurs.
 */
export interface Annonce {
  id: number;
  titre: string;
  contenu: string;
  type: 'ANNONCE' | 'MESSAGE' | 'MEMO';
  datePublication: string;
  auteur: string | null;
  /** Promotion visee, ou null lorsque l'annonce s'adresse a tout l'institut. */
  promotionLibelle: string | null;
  epinglee: boolean;
  lue: boolean;
}

export interface AnnonceCreation {
  titre: string;
  contenu: string;
  epinglee: boolean;
  cohorteIds?: number[];
}
