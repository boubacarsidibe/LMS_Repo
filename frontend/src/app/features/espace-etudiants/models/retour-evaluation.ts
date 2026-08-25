export interface RetourEvaluation {
  id: number;
  note: number; // sur 5
  commentaire: string;
  /** Date déjà mise en forme pour l'affichage. */
  date: string;
  /**
   * Date brute ISO, conservée pour pouvoir trier.
   * `date` étant localisée (« 13 août 2026 »), elle ne se compare pas.
   */
  dateIso: string;
  /** Identifiant du cours : sert au filtrage, deux cours pouvant porter le même titre. */
  coursId: number;
  cours: string;
  statut: 'valide' | 'action-requise';
  epingle: boolean;
}
