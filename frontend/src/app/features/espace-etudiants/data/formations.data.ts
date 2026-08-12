import { Formation } from '../models/formation';

export const mesFormationsMock: Formation[] = [
  {
    id: 1,
    code: 'ECO-402',
    intitule: "Économie du Pétrole et Gaz",
    progression: 65,
    status: 'EN_COURS',
    favorite: false,
    image: 'images/image_lms_accueil.jpeg'
    // additional UI hints
  },
  {
    id: 2,
    code: 'MGT-301',
    intitule: "Gestion de Projet Raffinerie",
    progression: 22,
    status: 'EN_COURS',
    favorite: true,
    image: 'images/image_lms_accueil.jpeg'
  },
  {
    id: 3,
    code: 'LAW-210',
    intitule: "Droit de l'Énergie en Afrique",
    progression: 88,
    status: 'TERMINE',
    favorite: false,
    image: 'images/image_lms_accueil.jpeg'
  },
  {
    id: 4,
    code: 'GEO-110',
    intitule: "Géopolitique des Ressources",
    progression: 10,
    status: 'EN_COURS',
    favorite: false,
    image: 'images/image_lms_accueil.jpeg'
  }
];
