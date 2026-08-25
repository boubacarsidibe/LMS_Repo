export interface NoteModule {
  id: number;
  module: string;
  note: number;
  commentaire?: string | null;
  dateEvaluation: string;
  coursId: number;
  coursCode: string;
  coursTitre: string;
  formateurNom: string;
}
