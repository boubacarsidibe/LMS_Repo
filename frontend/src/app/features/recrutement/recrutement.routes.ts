import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/guards/auth-guard';
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

  // VisiteurPublic (connecté) - Candidature
  {
    path: 'candidature',
    loadComponent: () => import('./candidature/formulaire/formulaire.component').then(m => m.FormulaireComponent),
    canActivate: [authGuard]
  },
  {
    path: 'candidature/pieces',
    loadComponent: () => import('./candidature/pieces/pieces.component').then(m => m.PiecesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'candidature/paiement',
    loadComponent: () => import('./candidature/paiement/paiement.component').then(m => m.PaiementComponent),
    canActivate: [authGuard]
  },

  // Candidat - Suivi
  {
    path: 'suivi',
    loadComponent: () => import('./suivi/suivi.component').then(m => m.SuiviComponent),
    canActivate: [authGuard]
  },

  // Admin
  {
    path: 'admin/sessions',
    loadComponent: () => import('./admin/admin-sessions/admin-sessions.component').then(m => m.AdminSessionsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin/candidatures',
    loadComponent: () => import('./admin/admin-candidatures/admin-candidatures.component').then(m => m.AdminCandidaturesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin/tests',
    loadComponent: () => import('./admin/admin-tests/admin-tests.component').then(m => m.AdminTestsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin/entretiens',
    loadComponent: () => import('./admin/admin-entretiens/admin-entretiens.component').then(m => m.AdminEntretiensComponent),
    canActivate: [authGuard]
  }
];