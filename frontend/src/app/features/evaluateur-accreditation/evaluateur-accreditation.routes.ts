import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layouts/evaluateur-layout/evaluateur-layout').then((m) => m.EvaluateurLayout),
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      {
        path: 'tableau-de-bord',
        loadComponent: () =>
          import('./pages/tableau-de-bord/tableau-de-bord').then((m) => m.EvaluateurTableauDeBordComponent),
      },
      {
        path: 'demandes',
        loadComponent: () => import('./pages/demandes/demandes').then((m) => m.EvaluateurDemandesComponent),
      },
      {
        path: 'criteres',
        loadComponent: () => import('./pages/criteres/criteres').then((m) => m.EvaluateurCriteresComponent),
      },
      {
        path: 'rendez-vous',
        loadComponent: () =>
          import('../rendez-vous/pages/rendez-vous/rendez-vous.component').then(
            (m) => m.RendezVousComponent
          ),
      },
      {
        path: 'mon-profil',
        loadComponent: () =>
          import('./pages/mon-profil/evaluateur-mon-profil.component').then(
            (m) => m.EvaluateurMonProfilComponent
          ),
      },
    ],
  },
];
