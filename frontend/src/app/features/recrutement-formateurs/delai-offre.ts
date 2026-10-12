/** Une clôture à 7 jours ou moins est signalée comme imminente. */
export const SEUIL_CLOTURE_IMMINENTE_JOURS = 7;

/** `AAAA-MM-JJ` du jour *local* : la date limite d'une offre est un jour civil, pas un instant. */
export function jourLocal(date: Date): string {
  const mois = String(date.getMonth() + 1).padStart(2, '0');
  const jour = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${mois}-${jour}`;
}

function enJours(jour: string): number {
  const [annee, mois, j] = jour.split('-').map(Number);
  return Date.UTC(annee, mois - 1, j) / 86_400_000;
}

/**
 * Jours qui séparent aujourd'hui de la date limite ; `null` si l'offre n'a pas de date limite.
 * Le dernier jour compte encore : à la date limite, on peut postuler jusqu'au soir (le serveur
 * ne refuse qu'à partir du lendemain), donc le résultat est 0 et non −1.
 */
export function joursRestants(dateLimite: string | null, aujourdhui: Date): number | null {
  return dateLimite ? enJours(dateLimite) - enJours(jourLocal(aujourdhui)) : null;
}

/** Ouverte aux candidatures : sans date limite, ou date limite non dépassée. Même règle que le serveur. */
export function estOuverte(dateLimite: string | null, aujourdhui: Date): boolean {
  const restants = joursRestants(dateLimite, aujourdhui);
  return restants === null || restants >= 0;
}

export function clotureImminente(dateLimite: string | null, aujourdhui: Date): boolean {
  const restants = joursRestants(dateLimite, aujourdhui);
  return restants !== null && restants >= 0 && restants <= SEUIL_CLOTURE_IMMINENTE_JOURS;
}

/**
 * « 12 décembre 2026 » : l'application n'a pas de langue par défaut française (le pipe `date`
 * donnerait « December »), et une date limite est un jour civil, donc formatée en UTC pour ne
 * jamais glisser d'un jour selon le fuseau du visiteur.
 */
export function dateLongue(jour: string): string {
  const [annee, mois, j] = jour.split('-').map(Number);
  return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' }).format(
    new Date(Date.UTC(annee, mois - 1, j))
  );
}
