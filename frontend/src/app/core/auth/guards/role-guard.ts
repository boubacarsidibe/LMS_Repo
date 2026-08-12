import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of } from 'rxjs';
import { Auth } from '../services/authService';

/**
 * Guard basé sur les rôles.
 * Usage : canActivate: [roleGuard('ROLE_CANDIDAT')]
 *         canActivate: [roleGuard('ROLE_RESPONSABLE_ADMISSION', 'ROLE_ETUDIANT')]
 */
export function roleGuard(...requiredRoles: string[]): CanActivateFn {
  return (route, state) => {
    const router = inject(Router);
    const auth = inject(Auth);

    const evaluate = (user: ReturnType<typeof auth.currentUser>): boolean => {
      if (!user) {
        router.navigate(['/auth/login']);
        return false;
      }

      const userRoles = user.roles || [];
      if (requiredRoles.some(role => userRoles.includes(role))) {
        return true;
      }

      if (userRoles.includes('ROLE_RESPONSABLE_ADMISSION')) {
        router.navigate(['/responsable-admission']);
      } else if (userRoles.includes('ROLE_CANDIDAT')) {
        router.navigate(['/recrutement/catalogue']);
      } else if (userRoles.includes('ROLE_ETUDIANT')) {
        router.navigate(['/espace-etudiant/accueil']);
      } else if (userRoles.includes('ROLE_FORMATEUR')) {
        router.navigate(['/espace-etudiant/formateur/dashboard-formateur']);
      } else {
        router.navigate(['/accueil']);
      }
      return false;
    };

    const user = auth.currentUser();
    return user ? of(evaluate(user)) : auth.getCurrentUser().pipe(map(evaluate));
  };
}
