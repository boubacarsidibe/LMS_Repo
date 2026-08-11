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
            }
        ]
    }
];
