import { roleGuard } from './role-guard';

/**
 * Réservé aux candidats.
 *
 * Délègue à roleGuard : l'implémentation précédente lisait le signal currentUser()
 * de façon synchrone et renvoyait donc false à chaque rechargement de page, avant
 * que le profil n'ait été récupéré — et ne redirigeait nulle part en cas de refus.
 */
export const candidatGuard = roleGuard('ROLE_CANDIDAT');
