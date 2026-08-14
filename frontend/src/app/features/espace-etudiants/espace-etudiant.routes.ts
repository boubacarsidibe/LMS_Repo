import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/guards/role-guard';
export const routes: Routes = [
    {
        path: 'accueil',
        loadComponent: () => import('./pages/accueil/accueil').then(m => m.Accueil),
        canActivate: [roleGuard('ROLE_ETUDIANT', 'ROLE_FORMATEUR')]
    },
    {
        path: 'mes-formations',
        loadComponent: () => import('./pages/mes-formations/mes-formations').then(m => m.MesFormations),
        canActivate: [roleGuard('ROLE_ETUDIANT')]
    },
    {
        path: 'annonces',
        loadComponent: () => import('./pages/annonces/annonces').then(m => m.Annonces),
        canActivate: [roleGuard('ROLE_ETUDIANT', 'ROLE_FORMATEUR')]
    },
    {
        path: 'phototheque',
        loadComponent: () => import('./pages/phototheque/phototheque').then(m => m.Phototheque),
        canActivate: [roleGuard('ROLE_ETUDIANT', 'ROLE_FORMATEUR')]
    },
    {
        path: 'certificats',
        loadComponent: () => import('./pages/certificats/certificats').then(m => m.Certificats),
        canActivate: [roleGuard('ROLE_ETUDIANT', 'ROLE_FORMATEUR')]
    },
    {
        path: 'formateur',
        loadComponent: () => import('./layouts/formateur-layout/formateur-layout').then(m => m.FormateurLayout),
        canActivate: [roleGuard('ROLE_FORMATEUR')],
        children: [
            { path: '', redirectTo: 'dashboard-formateur', pathMatch: 'full' },
            {
                path: 'dashboard-formateur',
                loadComponent: () => import('./pages/dashboard-formateur/dashboard-formateur').then(m => m.DashboardFormateur)
            },
            {
                path: 'mes-cours',
                loadComponent: () => import('./pages/mes-cours/mes-cours').then(m => m.MesCours)
            },
            {
                path: 'creer-cours',
                loadComponent: () => import('./pages/creer-cours/creer-cours').then(m => m.CreerCours)
            },
            {
                path: 'suivi-etudiants',
                loadComponent: () => import('./pages/suivi-etudiants/suivi-etudiants').then(m => m.SuiviEtudiants)
            },
            {
                path: 'profil-etudiant/:id',
                loadComponent: () => import('./pages/profil-etudiant/profil-etudiant').then(m => m.ProfilEtudiant)
            },
            {
                path: 'planning-calendrier',
                loadComponent: () => import('./pages/planning-calendrier/planning-calendrier').then(m => m.PlanningCalendrier)
            },
            {
                path: 'forum',
                loadComponent: () => import('./pages/forum/forum').then(m => m.Forum)
            },
            {
                path: 'retours-evaluations',
                loadComponent: () => import('./pages/retours-evaluations/retours-evaluations').then(m => m.RetoursEvaluations)
            },
            {
                path: 'mon-profil',
                loadComponent: () => import('./pages/mon-profil/mon-profil').then(m => m.MonProfil)
            },
            // Memes composants que les routes partagees ci-dessus (pas de duplication
            // de code ni de donnees) : accessibles ici pour que le formateur reste
            // dans son propre shell (sidebar) plutot que de basculer vers la navbar
            // partagee. Voir Annonces.dansEspaceFormateur pour le detail.
            {
                path: 'annonces',
                loadComponent: () => import('./pages/annonces/annonces').then(m => m.Annonces)
            },
            {
                path: 'phototheque',
                loadComponent: () => import('./pages/phototheque/phototheque').then(m => m.Phototheque)
            },
            {
                path: 'certificats',
                loadComponent: () => import('./pages/certificats/certificats').then(m => m.Certificats)
            }
        ]
    }
];
