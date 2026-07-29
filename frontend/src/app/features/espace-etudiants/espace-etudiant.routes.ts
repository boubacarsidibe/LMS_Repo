import { Routes } from '@angular/router';
import { Test } from './pages/test/test';
import { Accueil } from './pages/accueil/accueil';
import { guestGuard } from '../../core/auth/guards/guest-guard';

export const routes: Routes = [
    {
        path: 'accueil',
        component: Accueil,
        canActivate: [guestGuard]
    },
    {
        path: 'test',
        component: Test
    }
];