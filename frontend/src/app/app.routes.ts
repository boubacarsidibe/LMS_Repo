import { Routes } from '@angular/router';
import { Test1 } from './features/dashboard/test/test1/test1';
import { authGuard } from './core/auth/guards/auth-guard';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { VerifyOtp } from './features/auth/verify-otp/verify-otp';
import { AuthLayoutComponent } from './features/auth/layouts/auth-layout/auth-layout';
import { PolitiqueConfidentialite } from './core/staticPages/politique-confidentialite/politique-confidentialite';
import { ForgotPassword } from './features/auth/forgot-password/forgot-password';

export const routes: Routes = [
  // Redirection par défaut au lancement de l'application
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },

  // Groupe des pages d'authentification partageant la même mise en page
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      { path: 'login', component: Login },
      { path: 'register', component: Register },
      { path: 'verify-otp', component: VerifyOtp },
      { path: 'forgot-password', component: ForgotPassword }
    ]
  },
  {
    path:"espace-etudiants/",
    loadChildren: () => import('./features/espace-etudiants/espace-etudiant.routes').then(m => m.routes)
  },

  // Page protégée
  { path: 'test1', component: Test1, canActivate: [authGuard] },
  {path:"politique-de-confidentialite", component:PolitiqueConfidentialite},

  // Redirection de sécurité si l'URL saisie n'existe pas
  { path: '**', redirectTo: 'auth/login' },

  


];