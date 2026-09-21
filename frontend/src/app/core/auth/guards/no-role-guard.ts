import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of } from 'rxjs';
import { Auth } from '../services/authService';

/**
 * Guard d'exclusion : bloque les comptes qui possèdent déjà un des rôles
 * donnés, plutôt que d'exiger un rôle précis comme {@link roleGuard}.
 *
 * <p>Sert sur `/accreditation` et `/espace-entreprise`, où seul `authGuard`
 * protégeait la route (délibérément, pour laisser un compte tout juste créé
 * — sans rôle métier — déposer son premier dossier). Le point aveugle : rien
 * n'empêchait un compte qui a DÉJÀ un rôle interne établi (formateur,
 * étudiant, responsable, alumni…) de naviguer sur la même URL et d'obtenir
 * ROLE_ECOLE_FORMATION / ROLE_ENTREPRISE en plus du sien à la première
 * soumission — un formateur pouvait ainsi se retrouver avec les
 * fonctionnalités de demande d'accréditation, qui ne le concernent pas.
 * Usage : canActivate: [authGuard, noRoleGuard('ROLE_FORMATEUR', 'ROLE_ETUDIANT', ...)]
 */
export function noRoleGuard(...excludedRoles: string[]): CanActivateFn {
  return (route, state) => {
    const router = inject(Router);
    const auth = inject(Auth);

    const evaluate = (user: ReturnType<typeof auth.currentUser>): boolean => {
      if (!user) {
        router.navigate(['/auth/login']);
        return false;
      }
      const userRoles = user.roles || [];
      if (excludedRoles.some((role) => userRoles.includes(role))) {
        router.navigate([auth.getHomeUrl() ?? '/accueil']);
        return false;
      }
      return true;
    };

    const user = auth.currentUser();
    return user ? of(evaluate(user)) : auth.getCurrentUser().pipe(map(evaluate));
  };
}
