import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/guards/role-guard';

/** Routes de l'espace Alumni, groupées sous {@code AlumniLayout} (TDR §C). */
export const routes: Routes = [
    {
        path: '',
        loadComponent: () => import('./layouts/alumni-layout/alumni-layout').then(m => m.AlumniLayout),
        canActivate: [roleGuard('ROLE_ALUMNI')],
        children: [
            { path: '', redirectTo: 'mon-profil', pathMatch: 'full' },
            {
                path: 'mon-profil',
                loadComponent: () => import('./pages/mon-profil/mon-profil').then(m => m.MonProfil)
            },
            {
                path: 'mon-parcours',
                loadComponent: () => import('./pages/mon-parcours/mon-parcours').then(m => m.MonParcours)
            },
            {
                path: 'mes-releves',
                loadComponent: () => import('./pages/mes-releves/mes-releves').then(m => m.MesReleves)
            },
            {
                path: 'forum',
                loadComponent: () => import('./pages/forum-alumni/forum-alumni').then(m => m.ForumAlumni)
            },
            {
                path: 'forum/:id',
                loadComponent: () => import('./pages/fil-alumni/fil-alumni').then(m => m.FilAlumni)
            },
            {
                path: 'documents',
                loadComponent: () => import('./pages/documents-alumni/documents-alumni').then(m => m.DocumentsAlumni)
            }
        ]
    }
];
