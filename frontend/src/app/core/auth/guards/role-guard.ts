import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
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

    const user = auth.currentUser();
    if (!user) {
      router.navigate(['/auth/login']);
      return false;
    }

    const userRoles: string[] = (user as any)?.roles || [];
    const hasRole = requiredRoles.some(role => userRoles.includes(role));

    if (hasRole) {
      return true;
    }

    // Rediriger vers la page par défaut selon le rôle
    if (userRoles.includes('ROLE_RESPONSABLE_ADMISSION')) {
      router.navigate(['/responsable-admission']);
    } else if (userRoles.includes('ROLE_CANDIDAT')) {
      router.navigate(['/recrutement/catalogue']);
    } else if (userRoles.includes('ROLE_ETUDIANT')) {
      router.navigate(['/espace-etudiant/accueil']);
    } else {
      router.navigate(['/accueil']);
    }
    return false;
  };
}