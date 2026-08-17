import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/guards/role-guard';

/**
 * Espace du responsable pédagogique.
 *
 * <p>Toutes les rubriques vivent sous {@code PedagogieLayoutComponent}, qui fournit
 * la barre latérale et l'en-tête. Chaque écran portait auparavant sa propre barre
 * horizontale, dupliquée et bornée à 1280 px.
 */
export const PEDAGOGIE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layout/pedagogie-layout.component').then((m) => m.PedagogieLayoutComponent),
    canActivate: [roleGuard('ROLE_RESPONSABLE_PEDAGOGIQUE')],
    children: [
      { path: '', redirectTo: 'vue-ensemble', pathMatch: 'full' },
      {
        path: 'vue-ensemble',
        loadComponent: () =>
          import('./pages/vue-ensemble/pedagogie-vue-ensemble.component').then(
            (m) => m.PedagogieVueEnsembleComponent
          ),
      },
      {
        path: 'cours',
        loadComponent: () =>
          import('./pages/cours/pedagogie-cours.component').then((m) => m.PedagogieCoursComponent),
      },
      {
        path: 'cours/:id',
        loadComponent: () =>
          import('./pages/cours-supports/pedagogie-cours-supports.component').then(
            (m) => m.PedagogieCoursSupportsComponent
          ),
      },
    ],
  },
];
