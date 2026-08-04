import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/authService';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const auth = inject(Auth);

  return auth.checkToken().toPromise().then(isAuthenticated => {
    if (isAuthenticated) {
      return true;
    } else {
      router.navigate(['/auth/login']);
      return false;
    }
  });
};
