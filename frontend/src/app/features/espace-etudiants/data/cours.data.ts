import { Cours } from '../models/cours';

export const mesCours: Cours[] = [
    {
        id: 1,
        code: 'ECO-402',
        titre: 'Théorie et Pratique Économiques',
        nbEtudiants: 42,
        nbRessources: 12,
        progression: 68,
        couleurBandeau: 'bg-secondary-container'
    },
    {
        id: 2,
        code: 'MGT-301',
        titre: 'Management des Organisations',
        nbEtudiants: 38,
        nbRessources: 8,
        progression: 45,
        couleurBandeau: 'bg-primary-fixed-dim'
    }
];
