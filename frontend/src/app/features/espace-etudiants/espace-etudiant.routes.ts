import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/guards/role-guard';

/**
 * Routes de l'espace étudiant.
 *
 * <p>Les rubriques de l'étudiant sont regroupées sous {@code EtudiantLayout}, qui
 * fournit la navigation latérale et la cloche d'annonces. « Accueil » et les
 * pages historiques restent en dehors : elles portent leur propre barre
 * horizontale et sont référencées par la navigation publique.
 */
export const routes: Routes = [
    {
        path: 'accueil',
        loadComponent: () => import('./pages/accueil/accueil').then(m => m.Accueil),
        canActivate: [roleGuard('ROLE_ETUDIANT')]
    },
    {
        path: 'mes-formations',
        loadComponent: () => import('./pages/mes-formations/mes-formations').then(m => m.MesFormations),
        canActivate: [roleGuard('ROLE_ETUDIANT')]
    },
    {
        path: 'cours',
        loadComponent: () => import('./pages/mes-cours-etudiant/mes-cours-etudiant').then(m => m.MesCoursEtudiant),
        canActivate: [roleGuard('ROLE_ETUDIANT')]
    },
    {
        path: 'cours/:id',
        loadComponent: () => import('./pages/cours-detail/cours-detail').then(m => m.CoursDetail),
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
            },
            {
                path: 'mon-profil',
                loadComponent: () => import('./pages/mon-profil/mon-profil').then(m => m.MonProfil)
            }
        ]
    },

    // ===================== Espace étudiant (navigation latérale) =====================
    {
        path: '',
        loadComponent: () => import('./layouts/etudiant-layout/etudiant-layout').then(m => m.EtudiantLayout),
        canActivate: [roleGuard('ROLE_ETUDIANT')],
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            {
                path: 'dashboard',
                loadComponent: () => import('./pages/tableau-bord/tableau-bord').then(m => m.TableauBord)
            },
            {
                path: 'mes-cours',
                loadComponent: () => import('./pages/mes-cours-autorises/mes-cours-autorises').then(m => m.MesCoursAutorises)
            },
            {
                path: 'mes-cours/:id',
                loadComponent: () => import('./pages/cours-detail-etudiant/cours-detail-etudiant').then(m => m.CoursDetailEtudiant)
            },
            {
                path: 'calendrier',
                loadComponent: () => import('./pages/calendrier-etudiant/calendrier-etudiant').then(m => m.CalendrierEtudiant)
            },
            {
                path: 'annonces',
                loadComponent: () => import('./pages/annonces-etudiant/annonces-etudiant').then(m => m.AnnoncesEtudiant)
            },
            {
                path: 'forum',
                loadComponent: () => import('./pages/forum-etudiant/forum-etudiant').then(m => m.ForumEtudiant)
            },
            {
                path: 'forum/:id',
                loadComponent: () => import('./pages/fil-forum/fil-forum').then(m => m.FilForum)
            },
            {
                path: 'photheque',
                loadComponent: () => import('./pages/photheque/photheque').then(m => m.Photheque)
            },
            {
                path: 'ma-promotion',
                loadComponent: () => import('./pages/ma-promotion/ma-promotion').then(m => m.MaPromotionPage)
            },
            {
                path: 'mes-evaluations',
                loadComponent: () => import('./pages/mes-evaluations/mes-evaluations').then(m => m.MesEvaluations)
            },
            {
                path: 'mon-profil',
                loadComponent: () => import('./pages/mon-profil-etudiant/mon-profil-etudiant').then(m => m.MonProfilEtudiant)
            }
        ]
    }
];
