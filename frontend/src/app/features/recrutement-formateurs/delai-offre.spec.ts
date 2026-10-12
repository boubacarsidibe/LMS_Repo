import { clotureImminente, dateLongue, estOuverte, joursRestants, jourLocal } from './delai-offre';

describe('délai d’une offre', () => {
  // 12 oct. 2026, 23 h 30 en heure locale : le piège classique est de comparer des instants.
  const tardLe12 = new Date(2026, 9, 12, 23, 30);
  const totLe12 = new Date(2026, 9, 12, 0, 5);

  it('jourLocal garde le jour civil, même tard le soir', () => {
    expect(jourLocal(tardLe12)).toBe('2026-10-12');
    expect(jourLocal(totLe12)).toBe('2026-10-12');
  });

  it('compte les jours entiers, quelle que soit l’heure', () => {
    expect(joursRestants('2026-10-13', tardLe12)).toBe(1);
    expect(joursRestants('2026-10-13', totLe12)).toBe(1);
    expect(joursRestants('2026-12-12', totLe12)).toBe(61);
  });

  it('le jour de la date limite, l’offre est encore ouverte (0 jour restant)', () => {
    expect(joursRestants('2026-10-12', tardLe12)).toBe(0);
    expect(estOuverte('2026-10-12', tardLe12)).toBe(true);
  });

  it('le lendemain de la date limite, l’offre est fermée', () => {
    expect(estOuverte('2026-10-11', totLe12)).toBe(false);
    expect(joursRestants('2026-10-11', totLe12)).toBe(-1);
  });

  it('sans date limite, l’offre reste ouverte et aucun délai n’est annoncé', () => {
    expect(joursRestants(null, totLe12)).toBeNull();
    expect(estOuverte(null, totLe12)).toBe(true);
    expect(clotureImminente(null, totLe12)).toBe(false);
  });

  it('la clôture est imminente à 7 jours ou moins, jamais une fois passée', () => {
    expect(clotureImminente('2026-10-19', totLe12)).toBe(true); // J-7
    expect(clotureImminente('2026-10-20', totLe12)).toBe(false); // J-8
    expect(clotureImminente('2026-10-12', totLe12)).toBe(true); // dernier jour
    expect(clotureImminente('2026-10-11', totLe12)).toBe(false); // passée
  });

  it('traverse un changement de mois et d’année', () => {
    expect(joursRestants('2027-01-02', new Date(2026, 11, 30, 10, 0))).toBe(3);
  });

  it('écrit la date limite en français, sans décalage de fuseau', () => {
    expect(dateLongue('2026-12-12')).toBe('12 décembre 2026');
    expect(dateLongue('2026-03-31')).toBe('31 mars 2026');
  });
});
