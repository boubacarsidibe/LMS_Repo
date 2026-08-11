import { Routes } from '@angular/router';
import { candidatGuard } from '../../core/auth/guards/candidat-guard';
import { CatalogueComponent } from './catalogue/catalogue.component';

export const RECRUTEMENT_ROUTES: Routes = [
  // Public
  { path: 'catalogue', component: CatalogueComponent },
  {
    path: 'catalogue/:id',
    loadComponent: () =>
      import('./catalogue/formation-detail/formation-detail.component').then(
        (m) => m.FormationDetailComponent
      ),
  },

  // Candidat - Candidature
  {
    path: 'candidature',
    loadComponent: () => import('./candidature/formulaire/formulaire.component').then(m => m.FormulaireComponent),
    canActivate: [candidatGuard]
  },
  {
    path: 'candidature/paiement',
    loadComponent: () => import('./candidature/paiement/paiement.component').then(m => m.PaiementComponent),
    canActivate: [candidatGuard]
  },

  // Candidat - Suivi
  {
    path: 'suivi',
    loadComponent: () => import('./suivi/suivi.component').then(m => m.SuiviComponent),
    canActivate: [candidatGuard]
  }
];