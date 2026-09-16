export interface ActiviteCours {
  id: number;
  coursId: number;
  module: string;
  titre: string;
  description?: string | null;
  type: 'DEVOIR' | 'EVALUATION';
  modalite: 'EN_LIGNE' | 'PRESENTIEL';
  datePublication: string;
  dateLimite?: string | null;
  bareme: number;
}

export interface ActiviteCreation {
  module: string;
  titre: string;
  description?: string;
  type: 'DEVOIR' | 'EVALUATION';
  modalite: 'EN_LIGNE' | 'PRESENTIEL';
  datePublication: string;
  dateLimite?: string | null;
  bareme?: number;
}

export interface RemiseActivite {
  id: number;
  activiteId: number;
  etudiantId: number;
  etudiant: string;
  contenu: string;
  remiseLe: string;
  note?: number | null;
  commentaireCorrection?: string | null;
  corrigeLe?: string | null;
}
