import { AbstractControl } from '@angular/forms';

/** Vrai dès que l'utilisateur a saisi au moins un caractère (ou choisi une valeur). */
export function aSaisiQuelqueChose(valeur: unknown): boolean {
  if (valeur === null || valeur === undefined) {
    return false;
  }
  return typeof valeur === 'string' ? valeur.length > 0 : true;
}

/**
 * Règle d'affichage de l'erreur d'un champ, commune aux formulaires de
 * connexion, d'inscription et de contact :
 *
 * <ul>
 *   <li>jamais avant que l'utilisateur ait agi — un champ vide, même quitté,
 *       ne rougit pas ;</li>
 *   <li>dès qu'il a saisi au moins un caractère, l'erreur s'affiche tant que la
 *       valeur est invalide (« a » dans un e-mail) ;</li>
 *   <li>après une tentative d'envoi, tout champ invalide l'affiche, y compris
 *       vide : c'est le retour au clic sur le bouton.</li>
 * </ul>
 */
export function erreurVisible(controle: AbstractControl | null | undefined, tentativeEnvoi: boolean): boolean {
  if (!controle || !controle.invalid) {
    return false;
  }
  return tentativeEnvoi || aSaisiQuelqueChose(controle.value);
}
