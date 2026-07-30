import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { Test } from './pages/test/test';
import { DashboardFormateur } from './pages/dashboard-formateur/dashboard-formateur';
import { MesCours } from './pages/mes-cours/mes-cours';
import { FormateurLayout } from './layouts/formateur-layout/formateur-layout';
import { routes } from './espace-etudiant.routes';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
  declarations: [Accueil, Test, DashboardFormateur, MesCours, FormateurLayout],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    ReactiveFormsModule,
  ]
})
export class EspaceEtudiantsModule { }