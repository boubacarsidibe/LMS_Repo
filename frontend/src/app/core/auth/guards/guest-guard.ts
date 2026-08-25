import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of } from 'rxjs';
import { Auth } from '../services/authService';

/**
 * Guest guard : réserve une route aux visiteurs NON authentifiés (login, register...).
 * Si l'utilisateur est déjà connecté, on le redirige vers son espace au lieu de
 * lui remontrer le formulaire de connexion.
 */
export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const auth = inject(Auth);

  const evaluate = (user: ReturnType<typeof auth.currentUser>): boolean => {
    if (user) {
      auth.redirectByRole(router);
      return false;
    }
    return true;
  };

  const user = auth.currentUser();
  return user ? of(evaluate(user)) : auth.getCurrentUser().pipe(map(evaluate));
};
