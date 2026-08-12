import { Routes } from '@angular/router';
import { Test1 } from './features/dashboard/test/test1/test1';
import { authGuard } from './core/auth/guards/auth-guard';
import { roleGuard } from './core/auth/guards/role-guard';
import { ResponsableAdmissionComponent } from './features/recrutement/responsable-admission/overview/responsable-admission.component';
import { ResponsableAdmissionSessionsComponent } from './features/recrutement/responsable-admission/sessions/responsable-admission-sessions.component';
import { ResponsableAdmissionCandidaturesComponent } from './features/recrutement/responsable-admission/candidatures/responsable-admission-candidatures.component';
import { ResponsableAdmissionEntretiensComponent } from './features/recrutement/responsable-admission/entretiens/responsable-admission-entretiens.component';
import { ResponsableAdmissionDeliberationComponent } from './features/recrutement/responsable-admission/deliberation/responsable-admission-deliberation.component';
import { ResponsableAdmissionLayoutComponent } from './features/recrutement/responsable-admission/layout/responsable-admission-layout.component';
import { ResponsableAdmissionTestsComponent } from './features/recrutement/responsable-admission/tests/responsable-admission-tests.component';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { VerifyOtp } from './features/auth/verify-otp/verify-otp';
import { AuthLayoutComponent } from './features/auth/layouts/auth-layout/auth-layout';
import { PolitiqueConfidentialite } from './core/staticPages/politique-confidentialite/politique-confidentialite';
import { ForgotPassword } from './features/auth/forgot-password/forgot-password';

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
      { path: 'verify-otp', component: VerifyOtp },
      { path: 'forgot-password', component: ForgotPassword }
    ]
  },
  {
    path:"espace-etudiant",
    loadChildren: () => import('./features/espace-etudiants/espace-etudiants-module').then(m => m.EspaceEtudiantsModule)
  },

  // Page protégée
  { path: 'test1', component: Test1, canActivate: [authGuard] },

  // Dashboard redirection par rôle
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/test/test1/test1').then(m => m.Test1),
    // Note : ce composant est temporaire, la vraie redirection est gérée par redirectByRole()
  },

  // Dashboard Responsable Admission (layout partagé : sidebar + navbar)
  {
    path: 'responsable-admission',
    component: ResponsableAdmissionLayoutComponent,
    canActivate: [authGuard, roleGuard('ROLE_RESPONSABLE_ADMISSION')],
    children: [
      { path: '', component: ResponsableAdmissionComponent },
      { path: 'sessions', component: ResponsableAdmissionSessionsComponent },
      { path: 'candidatures', component: ResponsableAdmissionCandidaturesComponent },
      { path: 'entretiens', component: ResponsableAdmissionEntretiensComponent },
      { path: 'deliberation', component: ResponsableAdmissionDeliberationComponent },
      { path: 'tests', component: ResponsableAdmissionTestsComponent },
      { path: 'statistiques', loadComponent: () => import('./features/recrutement/responsable-admission/statistiques/responsable-admission-statistiques.component').then(m => m.ResponsableAdmissionStatistiquesComponent) },
      { path: 'settings', loadComponent: () => import('./features/recrutement/responsable-admission/settings/responsable-admission-settings.component').then(m => m.ResponsableAdmissionSettingsComponent) },
    ]
  },
  {path:"politique-de-confidentialite", component:PolitiqueConfidentialite},

  // Module Recrutement & Candidatures (lazy-loaded)
  {
    path: 'recrutement',
    loadChildren: () => import('./features/recrutement/recrutement.routes').then(m => m.RECRUTEMENT_ROUTES)
  },

  // Redirection de sécurité si l'URL saisie n'existe pas
  { path: '**', redirectTo: 'auth/login' },

  


];