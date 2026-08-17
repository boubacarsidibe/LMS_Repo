import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/guards/role-guard';

/**
 * Routes de l'espace étudiant.
 *
 * <p>Toutes les rubriques de l'étudiant sont regroupées sous {@code EtudiantLayout},
 * qui fournit la navigation latérale et la cloche d'annonces.
 *
 * <p>Un second parcours coexistait auparavant hors de ce shell — « Accueil »,
 * « Formations », une liste de cours et un détail de cours — avec sa propre barre
 * horizontale. Il doublait la barre latérale écran pour écran, sur des composants
 * distincts qu'il fallait maintenir en parallèle, et c'est là qu'atterrissait
 * l'étudiant après sa connexion. Seule « Formations » n'avait pas d'équivalent :
 * elle a été déplacée sous le shell plutôt que supprimée.
 *
 * <p>Ce qui reste hors du shell relève du seul formateur, qui dispose du sien.
 */
export const routes: Routes = [
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
                // Catalogue de l'offre de formation. Seule rubrique de l'ancien
                // parcours sans equivalent dans la barre laterale : deplacee ici
                // plutot que retiree.
                path: 'mes-formations',
                loadComponent: () => import('./pages/mes-formations/mes-formations').then(m => m.MesFormations)
            },
            {
                path: 'mes-evaluations',
                loadComponent: () => import('./pages/mes-evaluations/mes-evaluations').then(m => m.MesEvaluations)
            },
            {
                path: 'mes-resultats',
                loadComponent: () => import('./pages/mes-resultats/mes-resultats').then(m => m.MesResultats)
            },
            {
                path: 'mon-profil',
                loadComponent: () => import('./pages/mon-profil-etudiant/mon-profil-etudiant').then(m => m.MonProfilEtudiant)
            }
        ]
    }
];
