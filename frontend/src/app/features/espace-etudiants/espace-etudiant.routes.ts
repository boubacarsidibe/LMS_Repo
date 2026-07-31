import { Routes } from '@angular/router';
import { Test } from './pages/test/test';
import { Accueil } from './pages/accueil/accueil';
import { DashboardFormateur } from './pages/dashboard-formateur/dashboard-formateur';
import { MesCours } from './pages/mes-cours/mes-cours';
import { FormateurLayout } from './layouts/formateur-layout/formateur-layout';
import { guestGuard } from '../../core/auth/guards/guest-guard';
import { CreerCours } from './pages/creer-cours/creer-cours';
import { ProfilEtudiant } from './pages/profil-etudiant/profil-etudiant';
import { SuiviEtudiants } from './pages/suivi-etudiants/suivi-etudiants';
import { PlanningCalendrier } from './pages/planning-calendrier/planning-calendrier';

export const routes: Routes = [
    {
        path: 'accueil',
        component: Accueil,
        canActivate: [guestGuard]
    },
    {
        path: 'formateur',
        component: FormateurLayout,
        children: [
            { path: '', redirectTo: 'dashboard-formateur', pathMatch: 'full' },
            { path: 'dashboard-formateur', component: DashboardFormateur },
            { path: 'mes-cours', component: MesCours },
            { path: 'creer-cours', component: CreerCours },
            { path: 'suivi-etudiants', component: SuiviEtudiants },
            { path: 'profil-etudiant/:id', component: ProfilEtudiant },
            { path: 'planning-calendrier', component: PlanningCalendrier }
        ]
    },
    {
        path: 'test',
        component: Test
    }
];