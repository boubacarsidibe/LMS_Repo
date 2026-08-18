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
        path: 'promotions',
        loadComponent: () =>
          import('./pages/promotions/pedagogie-promotions.component').then(
            (m) => m.PedagogiePromotionsComponent
          ),
      },
      {
        path: 'etudiants',
        loadComponent: () =>
          import('./pages/etudiants/pedagogie-etudiants.component').then(
            (m) => m.PedagogieEtudiantsComponent
          ),
      },
      {
        path: 'releves',
        loadComponent: () =>
          import('./pages/releves/pedagogie-releves.component').then(
            (m) => m.PedagogieRelevesComponent
          ),
      },
      {
        path: 'certificats',
        loadComponent: () =>
          import('./pages/certificats/pedagogie-certificats.component').then(
            (m) => m.PedagogieCertificatsComponent
          ),
      },
      {
        path: 'qualite',
        loadComponent: () =>
          import('./pages/qualite/pedagogie-qualite.component').then(
            (m) => m.PedagogieQualiteComponent
          ),
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
