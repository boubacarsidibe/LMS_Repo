import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/authService';

export const candidatGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const auth = inject(Auth);

    return auth.isCandidate()
    }
