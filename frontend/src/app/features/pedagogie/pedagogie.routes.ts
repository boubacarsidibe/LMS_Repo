import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/guards/role-guard';

export const PEDAGOGIE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/cours/pedagogie-cours.component').then(m => m.PedagogieCoursComponent),
    canActivate: [roleGuard('ROLE_RESPONSABLE_PEDAGOGIQUE')],
  },
  {
    path: 'cours/:id',
    loadComponent: () =>
      import('./pages/cours-supports/pedagogie-cours-supports.component').then(
        m => m.PedagogieCoursSupportsComponent
      ),
    canActivate: [roleGuard('ROLE_RESPONSABLE_PEDAGOGIQUE')],
  },
];
