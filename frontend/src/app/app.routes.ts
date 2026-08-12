import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth-guard';
import { guestGuard } from './core/auth/guards/guest-guard';
import { roleGuard } from './core/auth/guards/role-guard';

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
  // guestGuard : un utilisateur déjà connecté est redirigé vers son espace
  // plutôt que de revoir le formulaire de connexion.
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/layouts/auth-layout/auth-layout').then((m) => m.AuthLayoutComponent),
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
      },
    ],
  },
  {
    path: 'espace-etudiant',
    loadChildren: () => import('./features/espace-etudiants/espace-etudiant.routes').then(m => m.routes)
  },

  // Dashboard Responsable Admission (layout partagé : sidebar + navbar)
  {
    path: 'responsable-admission',
    canActivate: [authGuard, roleGuard('ROLE_RESPONSABLE_ADMISSION')],
    loadComponent: () =>
      import('./features/recrutement/responsable-admission/layout/responsable-admission-layout.component').then(
        (m) => m.ResponsableAdmissionLayoutComponent
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/overview/responsable-admission.component').then(
            (m) => m.ResponsableAdmissionComponent
          ),
      },
      {
        path: 'sessions',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/sessions/responsable-admission-sessions.component').then(
            (m) => m.ResponsableAdmissionSessionsComponent
          ),
      },
      {
        path: 'candidatures',
        loadComponent: () =>
          import(
            './features/recrutement/responsable-admission/candidatures/responsable-admission-candidatures.component'
          ).then((m) => m.ResponsableAdmissionCandidaturesComponent),
      },
      {
        path: 'entretiens',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/entretiens/responsable-admission-entretiens.component').then(
            (m) => m.ResponsableAdmissionEntretiensComponent
          ),
      },
      {
        path: 'deliberation',
        loadComponent: () =>
          import(
            './features/recrutement/responsable-admission/deliberation/responsable-admission-deliberation.component'
          ).then((m) => m.ResponsableAdmissionDeliberationComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/users/responsable-users.component').then(
            (m) => m.ResponsableUsersComponent
          ),
      },
    ],
  },
  {
    path: 'politique-de-confidentialite',
    loadComponent: () =>
      import('./core/staticPages/politique-confidentialite/politique-confidentialite').then(
        (m) => m.PolitiqueConfidentialite
      ),
  },
  {
    path: 'terms-and-conditions',
    loadComponent: () =>
      import('./core/staticPages/terms-and-conditions/terms-and-conditions').then((m) => m.TermsAndConditions),
  },

  // Module Recrutement & Candidatures (lazy-loaded)
  {
    path: 'recrutement',
    loadChildren: () => import('./features/recrutement/recrutement.routes').then(m => m.RECRUTEMENT_ROUTES)
  },

  // Redirection de sécurité si l'URL saisie n'existe pas
  { path: '**', redirectTo: 'auth/login' },

  


];