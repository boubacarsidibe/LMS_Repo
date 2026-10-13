import { entreesRenseignees } from './dossier-enrichi';

describe('entreesRenseignees', () => {
  it('écarte une entrée dont tous les champs sont vides, nuls ou blancs', () => {
    const vide = { etablissement: null, intituleDiplome: '', ville: '   ', dateDebut: null };

    expect(entreesRenseignees([vide])).toEqual([]);
  });

  it("garde une entrée dès qu'un seul champ est renseigné", () => {
    const partielle = { etablissement: 'UCAD', intituleDiplome: null, ville: null };
    const complete = { etablissement: 'UCAD', intituleDiplome: 'Master', ville: 'Dakar' };

    expect(entreesRenseignees([partielle, complete])).toEqual([partielle, complete]);
  });

  it('un booléen seul ne rend pas une entrée « renseignée » (poste actuel coché sans rien d’autre)', () => {
    const casee = { employeur: null, intitulePoste: '', posteActuel: true };
    const renseignee = { employeur: 'INPG', intitulePoste: '', posteActuel: false };

    expect(entreesRenseignees([casee, renseignee])).toEqual([renseignee]);
  });

  it('garde les entrées remplies dans leur ordre, et ne modifie pas la liste d’origine', () => {
    const a = { x: 'a' };
    const vide = { x: null };
    const b = { x: 'b' };
    const liste = [a, vide, b];

    expect(entreesRenseignees(liste)).toEqual([a, b]);
    expect(liste).toEqual([a, vide, b]);
  });

  it('un nombre, même 0, est une valeur renseignée', () => {
    expect(entreesRenseignees([{ annees: 0, nom: null }])).toHaveLength(1);
  });
});
