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

  // Contact, Institut, offres formateurs, et tout /recrutement/* : regroupés
  // sous un même layout (PublicNavbarLayoutComponent) portant UN SEUL
  // app-navbar persistant. Avant ce regroupement, chacune de ces pages
  // embarquait son propre app-navbar : naviguer de l'une à l'autre détruisait
  // et recréait le composant à chaque clic (relançant notifications et
  // vérification de candidature à chaque fois) — signalé en recette comme un
  // navbar qui « clignote », alors même que son CSS `position: fixed` le
  // gardait immobile pendant un défilement (deux choses différentes). Les
  // chemins ('contact', 'institut'...) restent inchangés : ce parent ne porte
  // pas de segment d'URL propre (`path: ''`), donc aucune route existante ne
  // bouge.
  {
    path: '',
    loadComponent: () =>
      import('./shared/components/navbar/public-navbar-layout.component').then(
        (m) => m.PublicNavbarLayoutComponent
      ),
    children: [
      {
        path: 'contact',
        loadComponent: () =>
          import('./features/recrutement/catalogue/contact/contact.component').then(
            (m) => m.ContactComponent
          ),
      },
      {
        path: 'institut',
        loadComponent: () =>
          import('./features/recrutement/catalogue/institut/institut.component').then(
            (m) => m.InstitutComponent
          ),
      },
      // Recrutement des formateurs (procédure QMS-007). La LISTE des offres
      // est publique — sans compte, comme n'importe quel site d'offres
      // d'emploi : l'endpoint correspondant est déjà permitAll
      // (RecrutementFormateurPublicController). Seul « Postuler » exige la
      // connexion, le composant renvoyant alors vers /auth/login avec un
      // returnUrl pour revenir sur l'offre choisie.
      {
        path: 'offres-formateurs',
        loadComponent: () =>
          import('./features/recrutement-formateurs/pages/offres-publiques/offres-publiques').then(
            (m) => m.OffresPubliquesComponent
          ),
      },
      {
        path: 'recrutement',
        loadChildren: () => import('./features/recrutement/recrutement.routes').then(m => m.RECRUTEMENT_ROUTES),
      },
    ],
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

  // Recrutement des formateurs (procédure QMS-007) : « Postuler » exige la
  // connexion, le composant renvoyant alors vers /auth/login avec un
  // returnUrl pour revenir sur l'offre choisie. Le rôle ROLE_CANDIDAT_FORMATEUR
  // n'est accordé qu'au premier dépôt (cf. CandidatFormateurController).
  // La vitrine publique elle-même (/offres-formateurs) est déclarée plus haut,
  // sous PublicNavbarLayoutComponent.
  {
    path: 'candidature-formateur',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recrutement-formateurs/layouts/candidat-formateur-layout/candidat-formateur-layout').then(
        (m) => m.CandidatFormateurLayout
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/recrutement-formateurs/pages/ma-candidature/ma-candidature').then(
            (m) => m.MaCandidatureComponent
          ),
      },
      {
        path: 'mon-profil',
        loadComponent: () =>
          import('./features/recrutement-formateurs/pages/mon-profil/candidat-formateur-mon-profil').then(
            (m) => m.CandidatFormateurMonProfilComponent
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

      // Recrutement des formateurs (QMS-007) — onglet Formateurs de l'espace admission.
      {
        path: 'recrutement-formateurs',
        pathMatch: 'full',
        redirectTo: 'recrutement-formateurs/offres',
      },
      {
        // Route littérale avant 'offres/:id' : sinon Angular la capterait comme un id.
        path: 'recrutement-formateurs/offres/nouvelle',
        loadComponent: () =>
          import('./features/recrutement-formateurs/admin/offre-form/offre-form.component').then(
            (m) => m.OffreFormFormateursComponent
          ),
      },
      {
        path: 'recrutement-formateurs/offres/:id',
        loadComponent: () =>
          import('./features/recrutement-formateurs/admin/offre-form/offre-form.component').then(
            (m) => m.OffreFormFormateursComponent
          ),
      },
      {
        path: 'recrutement-formateurs/offres',
        loadComponent: () =>
          import('./features/recrutement-formateurs/admin/offres-admin/offres-admin.component').then(
            (m) => m.OffresAdminFormateursComponent
          ),
      },
      {
        path: 'recrutement-formateurs/candidatures',
        loadComponent: () =>
          import('./features/recrutement-formateurs/admin/candidatures-admin/candidatures-admin.component').then(
            (m) => m.CandidaturesAdminFormateursComponent
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
        path: 'tests/:id',
        loadComponent: () =>
          import('./features/recrutement/responsable-admission/tests/test-form/test-form.component').then(
            (m) => m.TestFormComponent
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

  // Module Recrutement & Candidatures : déclaré plus haut, sous
  // PublicNavbarLayoutComponent (avec /contact, /institut, /offres-formateurs).

  // Redirection de sécurité si l'URL saisie n'existe pas
  { path: '**', redirectTo: 'auth/login' },




];
