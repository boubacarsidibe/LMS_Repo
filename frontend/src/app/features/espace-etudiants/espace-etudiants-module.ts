import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { Test } from './pages/test/test';
import { routes } from './espace-etudiant.routes';

@NgModule({
  declarations: [Accueil, Test],
  imports: [
    CommonModule,
    RouterModule.forChild(routes)
  ]
})
export class EspaceEtudiantsModule { }