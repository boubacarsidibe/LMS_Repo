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
      } else if (userRoles.includes('ROLE_RESPONSABLE_PEDAGOGIQUE')) {
        router.navigate(['/pedagogie']);
      } else if (userRoles.includes('ROLE_CANDIDAT')) {
        router.navigate(['/recrutement/suivi']);
      } else if (userRoles.includes('ROLE_ETUDIANT')) {
        router.navigate(['/espace-etudiant/dashboard']);
      } else if (userRoles.includes('ROLE_FORMATEUR')) {
        router.navigate(['/espace-etudiant/formateur/dashboard-formateur']);
      } else if (userRoles.includes('ROLE_ALUMNI')) {
        router.navigate(['/espace-alumni/mon-profil']);
      } else if (userRoles.includes('ROLE_EVALUATEUR_ACCREDITATION')) {
        router.navigate(['/evaluateur-accreditation']);
      } else if (userRoles.includes('ROLE_ECOLE_FORMATION')) {
        router.navigate(['/accreditation']);
      } else if (userRoles.includes('ROLE_ENTREPRISE')) {
        router.navigate(['/espace-entreprise']);
      } else {
        router.navigate(['/accueil']);
      }
      return false;
    };

    const user = auth.currentUser();
    return user ? of(evaluate(user)) : auth.getCurrentUser().pipe(map(evaluate));
  };
}
