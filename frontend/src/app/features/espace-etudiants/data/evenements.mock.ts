export interface EvenementCalendrier {
  id: number;
  titre: string;
  date: Date;
  heure: string;
  type: 'cours' | 'echeance' | 'tutorat';
}

export const EVENEMENTS_MOCK: EvenementCalendrier[] = [
  { id: 1, titre: 'Macroéconomie', date: new Date(2026, 6, 4), heure: '09:00', type: 'cours' },
  { id: 2, titre: 'Tutorat M. Petit', date: new Date(2026, 6, 4), heure: '14:30', type: 'tutorat' },
  { id: 3, titre: 'Remise Essai', date: new Date(2026, 6, 6), heure: '23:59', type: 'echeance' },
  { id: 4, titre: 'Finance Durable', date: new Date(2026, 6, 13), heure: '10:00', type: 'cours' }
];