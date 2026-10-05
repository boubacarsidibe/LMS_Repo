import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Auth } from '../services/authService';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const auth = inject(Auth);
  return auth.checkToken().pipe(map(valid => {
    if (valid) return true;
    // Une panne réseau ne prouve pas que la session est expirée. Annuler la
    // navigation évite une boucle login -> espace -> login via guestGuard.
    return auth.getToken() ? false : router.createUrlTree(['/auth/login']);
  }));
};
