import { Routes } from '@angular/router';
import { Test } from './pages/test/test';
import { Accueil } from './pages/accueil/accueil';

export const routes: Routes = [
    {
        path: 'accueil',
        component: Accueil
    },
    {
        path: 'test',
        component: Test
    }
];