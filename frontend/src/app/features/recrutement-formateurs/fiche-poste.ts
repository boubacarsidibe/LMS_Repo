/** Taille maximale d'une fiche de poste : au-delà, ce n'est plus une fiche mais un dossier. */
export const TAILLE_MAX_FICHE_POSTE = 10 * 1024 * 1024;

/**
 * Contrôle d'un fichier avant envoi : la fiche de poste est un PDF de 10 Mo au plus.
 * Renvoie le message à montrer, ou `null` si le fichier convient. Le serveur refait le même
 * contrôle (extension, signature du contenu, taille) : celui-ci évite seulement un envoi inutile.
 */
export function erreurFichePoste(fichier: { name: string; size: number }): string | null {
  if (!fichier.name.toLowerCase().endsWith('.pdf')) {
    return 'La fiche de poste doit être un fichier PDF.';
  }
  if (fichier.size === 0) {
    return 'Le fichier est vide.';
  }
  if (fichier.size > TAILLE_MAX_FICHE_POSTE) {
    return 'La fiche de poste ne doit pas dépasser 10 Mo.';
  }
  return null;
}
