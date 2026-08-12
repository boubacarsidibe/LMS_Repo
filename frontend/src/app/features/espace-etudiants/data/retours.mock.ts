export interface RetourEvaluation {
  id: number;
  note: number; // sur 5
  commentaire: string;
  date: string;
  cours: string;
  statut: 'valide' | 'action-requise';
  epingle: boolean;
}

export const RETOURS_MOCK: RetourEvaluation[] = [
  {
    id: 1,
    note: 5,
    commentaire: "Cours extrêmement structuré. Les exemples sur les variables exogènes m'ont beaucoup aidé à comprendre le Module 3.",
    date: 'Il y a 2h',
    cours: 'Macroéconomie Avancée',
    statut: 'valide',
    epingle: false
  },
  {
    id: 2,
    note: 3,
    commentaire: "Le volume de lectures pour le Module 2 est très dense. Il est difficile de tout couvrir avant le test hebdomadaire.",
    date: 'Hier',
    cours: 'Finance Durable',
    statut: 'action-requise',
    epingle: false
  },
  {
    id: 3,
    note: 4,
    commentaire: "Très bonne pédagogie, mais j'aurais aimé plus de travaux pratiques sur les simulations. Le côté théorique est parfait.",
    date: '3 jours',
    cours: 'Macroéconomie Avancée',
    statut: 'valide',
    epingle: false
  }
];