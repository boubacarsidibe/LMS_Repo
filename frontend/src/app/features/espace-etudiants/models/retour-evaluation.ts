export interface RetourEvaluation {
  id: number;
  note: number; // sur 5
  commentaire: string;
  date: string;
  cours: string;
  statut: 'valide' | 'action-requise';
  epingle: boolean;
}
