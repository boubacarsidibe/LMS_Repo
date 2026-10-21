import { Routes } from '@angular/router';

/**
 * Routes de l'espace Entreprise (TDR §H), groupées sous {@code EntrepriseLayout}.
 *
 * <p>Pas de {@code roleGuard} ici : {@code ROLE_ENTREPRISE} n'est accordé qu'à
 * la première soumission réussie (voir {@code EntrepriseController}), même
 * patron que l'espace École de formation — seul {@code authGuard} (posé dans
 * {@code app.routes.ts}) protège ce chemin.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layouts/entreprise-layout/entreprise-layout').then(m => m.EntrepriseLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/accueil/entreprise-accueil').then(m => m.EntrepriseAccueil)
      },
      {
        path: 'nouvelle-demande',
        loadComponent: () => import('./pages/nouvelle-demande/nouvelle-demande').then(m => m.NouvelleDemande)
      },
      {
        path: 'catalogue',
        loadComponent: () => import('./pages/catalogue/catalogue').then(m => m.CatalogueEntreprise)
      },
      {
        path: 'messagerie',
        loadComponent: () => import('./pages/messagerie/messagerie').then(m => m.MessagerieEntreprise)
      },
      {
        path: 'rendez-vous',
        loadComponent: () =>
          import('../rendez-vous/pages/rendez-vous/rendez-vous.component').then(m => m.RendezVousComponent)
      },
      {
        path: 'mon-profil',
        loadComponent: () =>
          import('./pages/mon-profil/entreprise-mon-profil.component').then(m => m.EntrepriseMonProfilComponent)
      }
    ]
  }
];
