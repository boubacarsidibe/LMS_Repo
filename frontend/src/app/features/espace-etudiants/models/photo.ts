export interface Photo {
  id: number;
  titre: string;
  url: string;
  auteur: string;
  date: string;
  cohorteId: number | null;
  cohorteLibelle: string | null;
}
