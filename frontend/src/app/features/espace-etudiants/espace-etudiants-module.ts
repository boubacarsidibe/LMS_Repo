import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Accueil } from './pages/accueil/accueil';
import { Test } from './pages/test/test';
import { DashboardFormateur } from './pages/dashboard-formateur/dashboard-formateur';
import { MesCours } from './pages/mes-cours/mes-cours';
import { MesFormations } from './pages/mes-formations/mes-formations';
import { FormationCard } from './pages/mes-formations/formation-card';
import { CreerCours } from './pages/creer-cours/creer-cours';
import { SuiviEtudiants } from './pages/suivi-etudiants/suivi-etudiants';
import { ProfilEtudiant } from './pages/profil-etudiant/profil-etudiant';
import { FormateurLayout } from './layouts/formateur-layout/formateur-layout';
import { routes } from './espace-etudiant.routes';
import { PlanningCalendrier } from './pages/planning-calendrier/planning-calendrier';
import { Forum } from './pages/forum/forum';
import { RetoursEvaluations } from './pages/retours-evaluations/retours-evaluations';

@NgModule({
  declarations: [
    Accueil, Test, DashboardFormateur, MesCours, MesFormations, FormationCard, CreerCours,
    SuiviEtudiants, ProfilEtudiant, FormateurLayout, PlanningCalendrier, Forum, RetoursEvaluations
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    ReactiveFormsModule,
    FormsModule
  ]
})
export class EspaceEtudiantsModule { }