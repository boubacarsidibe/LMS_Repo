import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Accueil } from './pages/accueil/accueil';
import { Test } from './pages/test/test';
import { DashboardFormateur } from './pages/dashboard-formateur/dashboard-formateur';
import { MesCours } from './pages/mes-cours/mes-cours';
import { CreerCours } from './pages/creer-cours/creer-cours';
import { SuiviEtudiants } from './pages/suivi-etudiants/suivi-etudiants';
import { ProfilEtudiant } from './pages/profil-etudiant/profil-etudiant';
import { FormateurLayout } from './layouts/formateur-layout/formateur-layout';
import { routes } from './espace-etudiant.routes';
import { PlanningCalendrier } from './pages/planning-calendrier/planning-calendrier';

@NgModule({
  declarations: [
    Accueil, Test, DashboardFormateur, MesCours, CreerCours,
    SuiviEtudiants, ProfilEtudiant, FormateurLayout, PlanningCalendrier
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    ReactiveFormsModule,
    FormsModule
  ]
})
export class EspaceEtudiantsModule { }