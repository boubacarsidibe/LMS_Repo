export interface Formation {
  id?: number | string;
  code?: string;
  intitule?: string;
  progression?: number; // pour l'affichage de la barre
  status?: 'EN_COURS' | 'TERMINE' | 'FAVORI';
  favorite?: boolean;
  image?: string;
  categorie?: string;
  enseignant?: string;
}
