import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/authService';

/**
 * Guest guard : protège les pages réservées aux utilisateurs connectés.
 * Si l'utilisateur n'est PAS authentifié, on le redirige vers la page d'accueil.
 */
export const guestGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const auth = inject(Auth);

  return auth.checkToken().toPromise().then(isAuthenticated => {
    if (isAuthenticated) {
      return true;
    } else {
      router.navigate(['/accueil']);
      return false;
    }
  });
};
