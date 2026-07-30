import { Routes } from '@angular/router';
import { Test1 } from './features/dashboard/test/test1/test1';
import { authGuard } from './core/auth/guards/auth-guard';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { VerifyOtp } from './features/auth/verify-otp/verify-otp';
import { AuthLayoutComponent } from './features/auth/layouts/auth-layout/auth-layout';

export const routes: Routes = [
  // Redirection par défaut → Page d'accueil
  { path: '', redirectTo: 'accueil', pathMatch: 'full' },

  // Accueil
  {
    path: 'accueil',
    loadComponent: () =>
      import('./features/recrutement/catalogue/landing/landing.component').then(
        (m) => m.LandingComponent
      ),
  },

  // Contact
  {
    path: 'contact',
    loadComponent: () =>
      import('./features/recrutement/catalogue/contact/contact.component').then(
        (m) => m.ContactComponent
      ),
  },

  // Institut (À propos)
  {
    path: 'institut',
    loadComponent: () =>
      import('./features/recrutement/catalogue/institut/institut.component').then(
        (m) => m.InstitutComponent
      ),
  },

  // Groupe des pages d'authentification partageant la même mise en page
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      { path: 'login', component: Login },
      { path: 'register', component: Register },
      { path: 'verify-otp', component: VerifyOtp }
    ]
  },

  // Page protégée
  { path: 'test1', component: Test1, canActivate: [authGuard] },

  // Module Recrutement & Candidatures (lazy-loaded)
  {
    path: 'recrutement',
    loadChildren: () => import('./features/recrutement/recrutement.routes').then(m => m.RECRUTEMENT_ROUTES)
  },

  // Redirection de sécurité si l'URL saisie n'existe pas
  { path: '**', redirectTo: 'auth/login' }
];