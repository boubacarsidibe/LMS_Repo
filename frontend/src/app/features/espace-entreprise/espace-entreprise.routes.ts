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
      { path: '', redirectTo: 'nouvelle-demande', pathMatch: 'full' },
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
        path: 'mon-profil',
        loadComponent: () =>
          import('./pages/mon-profil/entreprise-mon-profil.component').then(m => m.EntrepriseMonProfilComponent)
      }
    ]
  }
];
