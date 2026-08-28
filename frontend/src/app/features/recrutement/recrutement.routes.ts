import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/guards/auth-guard';
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
  {
    path: 'sessions',
    loadComponent: () =>
      import('./sessions-publiques/sessions-publiques.component').then(
        (m) => m.SessionsPubliquesComponent
      ),
  },

  // Tout utilisateur connecté peut candidater : ROLE_CANDIDAT n'est accordé
  // qu'après une soumission réussie (voir CandidatController côté backend),
  // donc pas encore présent quand quelqu'un arrive sur ces pages.
  {
    path: 'candidature',
    loadComponent: () => import('./candidature/formulaire/formulaire.component').then(m => m.FormulaireComponent),
    canActivate: [authGuard]
  },
  {
    path: 'candidature/paiement',
    loadComponent: () => import('./candidature/paiement/paiement.component').then(m => m.PaiementComponent),
    canActivate: [authGuard]
  },

  // Candidat - Suivi : nécessite une candidature réellement soumise, donc ROLE_CANDIDAT.
  {
    path: 'suivi',
    loadComponent: () => import('./suivi/suivi.component').then(m => m.SuiviComponent),
    canActivate: [candidatGuard]
  },

  // Rendez-vous (TDR §J) : « chaque utilisateur » peut en demander un — donc
  // authGuard seul, pas candidatGuard : quelqu'un qui n'a pas encore soumis de
  // candidature a justement des raisons de vouloir rencontrer le personnel.
  {
    path: 'rendez-vous',
    loadComponent: () =>
      import('./rendez-vous/candidat-rendez-vous.component').then(m => m.CandidatRendezVousComponent),
    canActivate: [authGuard]
  }
];
