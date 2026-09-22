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

  // Vérification publique d'un certificat (page vers laquelle pointe le QR code
  // imprimé sur le document) : accessible sans compte, donc sans guard.
  {
    path: 'verification-certificat/:reference',
    loadComponent: () =>
      import('./features/certificats/verification/verification-certificat.component').then(
        (m) => m.VerificationCertificatComponent
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
  {
    path: 'espace-alumni',
    loadChildren: () => import('./features/espace-alumni/espace-alumni.routes').then(m => m.routes)
  },

  // Espace Entreprise (TDR §H) : tout utilisateur connecté peut soumettre un
  // besoin ou une demande de devis (ROLE_ENTREPRISE n'est accordé qu'à la
  // première soumission réussie, cf. EntrepriseController) — même patron que
  // /accreditation, authGuard seul, pas de roleGuard.
  {
    path: 'espace-entreprise',
    canActivate: [authGuard],
    loadChildren: () => import('./features/espace-entreprise/espace-entreprise.routes').then(m => m.routes)
  },

  // Liste des écoles accréditées (TDR §F) : réservée aux comptes connectés,
  // tout rôle confondu — décision produit du 2026-08-25 (LOG_BLOQUANT.md),
  // le TDR demandait initialement une liste réellement publique.
  {
    path: 'ecoles-accreditees',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/accreditation/pages/ecoles-accreditees/ecoles-accreditees').then(
        (m) => m.EcolesAccrediteesComponent
      ),
  },

  // Espace École de formation : tout utilisateur connecté peut déposer une demande
  // d'accréditation (le rôle ROLE_ECOLE_FORMATION n'est accordé qu'à la première
  // soumission réussie, cf. EcoleAccreditationController) — même patron que la
  // candidature de recrutement, authGuard seul, pas de roleGuard. Cadre commun
  // (EcoleLayout) ajouté le 2026-08-25 : la page de dépôt s'affichait seule, sans
  // en-tête ni cercle de profil (LOG_BLOQUANT.md).
  {
    path: 'accreditation',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/accreditation/layouts/ecole-layout/ecole-layout').then((m) => m.EcoleLayout),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/accreditation/pages/demande-accreditation/demande-accreditation').then(
            (m) => m.DemandeAccreditationComponent
          ),
      },
      {
        path: 'rendez-vous',
        loadComponent: () =>
          import('./features/rendez-vous/pages/rendez-vous/rendez-vous.component').then(
            (m) => m.RendezVousComponent
          ),
      },
      {
        path: 'mon-profil',
        loadComponent: () =>
          import('./features/accreditation/pages/mon-profil/ecole-mon-profil.component').then(
            (m) => m.EcoleMonProfilComponent
          ),
      },
    ],
  },

  // Espace Évaluateur Accréditation
  {
    path: 'evaluateur-accreditation',
    canActivate: [authGuard, roleGuard('ROLE_EVALUATEUR_ACCREDITATION')],
    loadChildren: () =>
      import('./features/evaluateur-accreditation/evaluateur-accreditation.routes').then((m) => m.routes),
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
        // Route littérale déclarée avant 'sessions/:id' : sinon Angular la
        // capturerait comme un id de session.
        path: 'sessions/nouvelle',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/sessions/session-form/session-form.component').then(
            (m) => m.SessionFormComponent
          ),
      },
      {
        path: 'sessions/:id/criteres/nouveau',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/sessions/critere-form/critere-form.component').then(
            (m) => m.CritereFormComponent
          ),
      },
      {
        path: 'sessions/:id/criteres/:critereId',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/sessions/critere-form/critere-form.component').then(
            (m) => m.CritereFormComponent
          ),
      },
      {
        path: 'sessions/:id/criteres',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/sessions/session-criteres/session-criteres.component').then(
            (m) => m.SessionCriteresComponent
          ),
      },
      {
        path: 'sessions/:id',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/sessions/session-form/session-form.component').then(
            (m) => m.SessionFormComponent
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
        path: 'candidatures/:id',
        loadComponent: () =>
          import(
            './features/recrutement/responsable-admission/candidatures/candidature-detail/candidature-detail.component'
          ).then((m) => m.CandidatureDetailComponent),
      },
      {
        path: 'tests',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/tests/responsable-admission-tests.component').then(
            (m) => m.ResponsableAdmissionTestsComponent
          ),
      },
      {
        path: 'tests/nouveau',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/tests/test-form/test-form.component').then(
            (m) => m.TestFormComponent
          ),
      },
      {
        path: 'tests/:id/notes',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/tests/test-notes/test-notes.component').then(
            (m) => m.TestNotesComponent
          ),
      },
      {
        path: 'tests/:id',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/tests/test-detail/test-detail.component').then(
            (m) => m.TestDetailComponent
          ),
      },
      {
        path: 'entretiens/:id/notes',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/entretiens/entretien-notes').then(
            (m) => m.EntretienNotesComponent
          ),
      },
      {
        path: 'entretiens/:id',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/entretiens/entretien-detail/entretien-detail.component').then(
            (m) => m.EntretienDetailComponent
          ),
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
        path: 'statistiques',
        loadComponent: () =>
          import(
            './features/recrutement/responsable-admission/statistiques/responsable-admission-statistiques.component'
          ).then((m) => m.ResponsableAdmissionStatistiquesComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/users/responsable-users.component').then(
            (m) => m.ResponsableUsersComponent
          ),
      },
      {
        path: 'paiements',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/paiements/responsable-admission-paiements.component').then(
            (m) => m.ResponsableAdmissionPaiementsComponent
          ),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/notifications/responsable-admission-notifications.component').then(
            (m) => m.ResponsableAdmissionNotificationsComponent
          ),
      },
      {
        path: 'parametres',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/parametres/responsable-admission-parametres.component').then(
            (m) => m.ResponsableAdmissionParametresComponent
          ),
      },
      {
        path: 'aide',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/aide/responsable-admission-aide.component').then(
            (m) => m.ResponsableAdmissionAideComponent
          ),
      },
      {
        path: 'rendez-vous',
        loadComponent: () =>
          import('./features/rendez-vous/pages/rendez-vous/rendez-vous.component').then(
            (m) => m.RendezVousComponent
          ),
      },
    ],
  },
  // Espace Responsable Pédagogique (offre d'enseignement : cours et supports)
  {
    path: 'pedagogie',
    canActivate: [authGuard, roleGuard('ROLE_RESPONSABLE_PEDAGOGIQUE')],
    loadChildren: () => import('./features/pedagogie/pedagogie.routes').then((m) => m.PEDAGOGIE_ROUTES),
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
