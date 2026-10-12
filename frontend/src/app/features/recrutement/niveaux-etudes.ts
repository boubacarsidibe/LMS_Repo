/**
 * Niveaux d'études, du plus bas au plus élevé : l'ordre compte, c'est lui qui fait qu'un candidat « atteint »
 * le niveau minimum d'une session (critères de présélection automatique).
 *
 * <p>Liste de l'INPG (questionnaire de collecte du 2026-09-30) : un CPS s'ouvre dès la 4ᵉ secondaire, une Licence
 * à partir du Bac+2 (DUT ou BTS), un Mastère ou un MBA à partir du Bac+5 — c'est-à-dire « Master 2 (M2) ».
 *
 * <p>Les libellés sont stockés tels quels en base (niveau du candidat, niveau minimum d'une session) : ne jamais
 * renommer une valeur existante, seulement en ajouter. À garder identique à `NIVEAUX_ORDONNES` de
 * `SessionCandidature` côté serveur.
 */
export const NIVEAUX_ETUDES: readonly string[] = [
  '4e secondaire',
  'Baccalauréat',
  'Bac+2 (DUT / BTS)',
  'Licence (L3)',
  'Master 1 (M1)',
  'Master 2 (M2)',
];

/** Rang d'un niveau, ou -1 s'il n'est pas dans la liste (« Autre », valeur libre) : on ne bloque alors pas dessus. */
export function rangNiveau(niveau: string | null | undefined): number {
  return niveau ? NIVEAUX_ETUDES.indexOf(niveau) : -1;
}

/** Vrai si le niveau du candidat est connu, celui de la session aussi, et que le premier est inférieur au second. */
export function niveauInsuffisant(niveauCandidat: string | null | undefined, niveauMinimum: string | null | undefined): boolean {
  const actuel = rangNiveau(niveauCandidat);
  const seuil = rangNiveau(niveauMinimum);
  return seuil >= 0 && actuel >= 0 && actuel < seuil;
}
