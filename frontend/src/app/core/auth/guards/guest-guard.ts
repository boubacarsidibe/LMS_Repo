import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/authService';

export const guestGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const auth = inject(Auth);

  return auth.checkToken().toPromise().then(isAuthenticated => {
    if (!isAuthenticated) {
      router.navigate(['/test1']); // TODO: remplacer par la vraie route dashboard une fois développée
      return false;
    } else {
      return true;
    }
  });
};