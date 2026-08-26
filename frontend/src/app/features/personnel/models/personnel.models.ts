export type TypeActivite = 'FORMATION' | 'PROJET' | 'RECHERCHE' | 'AUTRE';

export const LIBELLES_TYPE_ACTIVITE: Record<TypeActivite, string> = {
  FORMATION: 'Formation',
  PROJET: 'Projet',
  RECHERCHE: 'Recherche',
  AUTRE: 'Autre',
};

export interface Departement {
  id: number;
  code: string;
  libelle: string;
  actif: boolean;
}

export interface Activite {
  id: number;
  departementId: number;
  departementLibelle: string;
  auteurNom: string;
  typeActivite: TypeActivite;
  titre: string;
  description: string | null;
  mois: number;
  annee: number;
  dateCreation: string;
}

export interface ActiviteSaisie {
  departementId: number | null;
  typeActivite: TypeActivite;
  titre: string;
  description: string;
  mois: number;
  annee: number;
}
