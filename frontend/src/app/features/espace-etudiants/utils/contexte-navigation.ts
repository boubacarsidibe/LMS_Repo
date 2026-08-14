import { Router } from '@angular/router';

/**
 * Vrai si la page est affichee a l'interieur du shell formateur (route imbriquee
 * sous /formateur/), auquel cas la sidebar tient lieu de navigation et la navbar
 * partagee doit etre masquee.
 *
 * Annonces, Photothèque et Certificats sont accessibles par deux chemins (route
 * partagee pour les etudiants, route imbriquee pour le formateur) : centraliser
 * ce test evite d'avoir a le corriger dans trois composants si la structure des
 * routes change.
 */
export function estDansEspaceFormateur(router: Router): boolean {
  return router.url.includes('/formateur/');
}
