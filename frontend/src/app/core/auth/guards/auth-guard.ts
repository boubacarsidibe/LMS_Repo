import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const auth = inject(Auth);
  
  const token = localStorage.getItem('token');

  return auth.checkToken().toPromise().then(isAuthenticated => {
    if (isAuthenticated) {
      return true;
    } else {
      router.navigate(['/login']);
      return false;
    }
  });
};
