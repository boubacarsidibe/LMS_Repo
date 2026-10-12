import { NIVEAUX_ETUDES, diplomeRequis, niveauInsuffisant, rangNiveau } from './niveaux-etudes';

describe('niveaux d’études', () => {
  it('couvre les niveaux d’accès de l’INPG, du CPS (4e secondaire) au Mastère/MBA (Bac+5 = M2)', () => {
    expect(NIVEAUX_ETUDES[0]).toBe('4e secondaire');
    expect(NIVEAUX_ETUDES).toContain('Bac+2 (DUT / BTS)');
    expect(NIVEAUX_ETUDES[NIVEAUX_ETUDES.length - 1]).toBe('Master 2 (M2)');
  });

  it('conserve les libellés déjà stockés en base et ne contient aucun doublon', () => {
    for (const ancien of ['Baccalauréat', 'Licence (L3)', 'Master 1 (M1)', 'Master 2 (M2)']) {
      expect(NIVEAUX_ETUDES).toContain(ancien);
    }
    expect(new Set(NIVEAUX_ETUDES).size).toBe(NIVEAUX_ETUDES.length);
  });

  it('ordonne du plus bas au plus haut', () => {
    expect(rangNiveau('4e secondaire')).toBeLessThan(rangNiveau('Baccalauréat'));
    expect(rangNiveau('Baccalauréat')).toBeLessThan(rangNiveau('Bac+2 (DUT / BTS)'));
    expect(rangNiveau('Bac+2 (DUT / BTS)')).toBeLessThan(rangNiveau('Licence (L3)'));
    expect(rangNiveau('Licence (L3)')).toBeLessThan(rangNiveau('Master 2 (M2)'));
  });

  it('un niveau inférieur au minimum de la session est insuffisant', () => {
    expect(niveauInsuffisant('Baccalauréat', 'Bac+2 (DUT / BTS)')).toBe(true); // Licence INPG
    expect(niveauInsuffisant('Master 1 (M1)', 'Master 2 (M2)')).toBe(true); // Mastère / MBA
  });

  it('un niveau égal ou supérieur est suffisant', () => {
    expect(niveauInsuffisant('Bac+2 (DUT / BTS)', 'Bac+2 (DUT / BTS)')).toBe(false);
    expect(niveauInsuffisant('Licence (L3)', 'Bac+2 (DUT / BTS)')).toBe(false);
    expect(niveauInsuffisant('4e secondaire', '4e secondaire')).toBe(false); // CPS
  });

  it('ne bloque jamais sur un niveau inconnu ou une session sans minimum', () => {
    expect(niveauInsuffisant('Autre', 'Master 2 (M2)')).toBe(false);
    expect(niveauInsuffisant('Baccalauréat', null)).toBe(false);
    expect(niveauInsuffisant(undefined, 'Master 2 (M2)')).toBe(false);
  });

  it('exige une attestation de diplôme de tout candidat, sauf de niveau 4e secondaire (CPS)', () => {
    expect(diplomeRequis('4e secondaire')).toBe(false);
    for (const niveau of ['Baccalauréat', 'Bac+2 (DUT / BTS)', 'Licence (L3)', 'Master 1 (M1)', 'Master 2 (M2)', 'Autre']) {
      expect(diplomeRequis(niveau)).toBe(true);
    }
  });

  it('exige le diplôme tant que le niveau n’est pas renseigné : l’absence de réponse n’exonère personne', () => {
    expect(diplomeRequis('')).toBe(true);
    expect(diplomeRequis(null)).toBe(true);
    expect(diplomeRequis(undefined)).toBe(true);
  });
});
