export interface Seance {
  id: number;
  coursId: number;
  coursTitre: string;
  titre: string;
  type: string;
  dateHeure: string;
  dureeMinutes: number;
  lieuOuLien: string | null;
}
