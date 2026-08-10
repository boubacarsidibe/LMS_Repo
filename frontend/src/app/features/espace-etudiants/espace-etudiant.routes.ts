import { Routes } from '@angular/router';
import { Test } from './pages/test/test';
import { Accueil } from './pages/accueil/accueil';
import { MesFormations } from './pages/mes-formations/mes-formations';
import { DashboardFormateur } from './pages/dashboard-formateur/dashboard-formateur';
import { MesCours } from './pages/mes-cours/mes-cours';
import { FormateurLayout } from './layouts/formateur-layout/formateur-layout';
import { roleGuard } from '../../core/auth/guards/role-guard';
import { CreerCours } from './pages/creer-cours/creer-cours';
import { ProfilEtudiant } from './pages/profil-etudiant/profil-etudiant';
import { SuiviEtudiants } from './pages/suivi-etudiants/suivi-etudiants';
import { PlanningCalendrier } from './pages/planning-calendrier/planning-calendrier';

export const routes: Routes = [
    {
        path: 'accueil',
        component: Accueil,
        canActivate: [roleGuard('ROLE_ETUDIANT', 'ROLE_FORMATEUR')]
    },
    {
        path: 'mes-formations',
        component: MesFormations,
        // canActivate: [roleGuard('ROLE_ETUDIANT')]
    },
    {
        path: 'formateur',
        component: FormateurLayout,
        canActivate: [roleGuard('ROLE_FORMATEUR')],
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
