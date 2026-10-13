/**
 * Une entrée du dossier (formation, expérience…) dont aucun champ n'est renseigné ne dit rien : elle n'est pas
 * envoyée. Un bloc répétable s'ouvre par « Ajouter », et on peut l'ajouter sans le remplir.
 *
 * <p>Les booléens ne comptent pas : « poste actuel » vaut `false` dès l'ajout, il ne prouve pas que l'entrée
 * ait été remplie, et coché seul il ne décrit aucune expérience.
 */
export function entreesRenseignees<T extends object>(entrees: T[]): T[] {
  return entrees.filter((entree) =>
    Object.values(entree).some((valeur) => {
      if (typeof valeur === 'string') {
        return valeur.trim() !== '';
      }
      return valeur !== null && valeur !== undefined && typeof valeur !== 'boolean';
    })
  );
}
