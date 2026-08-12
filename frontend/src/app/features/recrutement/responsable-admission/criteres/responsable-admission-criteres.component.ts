import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CritereSelection, RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-responsable-admission-criteres',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './responsable-admission-criteres.component.html',
})
export class ResponsableAdmissionCriteresComponent implements OnInit {
  readonly niveaux = ['Baccalauréat', 'Licence (L3)', 'Master 1 (M1)', 'Master 2 (M2)'];

  criteres: CritereSelection = { actif: false, niveauEtudeMinimum: null, ageMinimum: null, ageMaximum: null };
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.loading = true;
    this.recrutementService.getCriteres().subscribe({
      next: (data) => {
        this.criteres = data;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger la configuration.';
        this.loading = false;
      },
    });
  }

  enregistrer(): void {
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.recrutementService.modifierCriteres(this.criteres).subscribe({
      next: (data) => {
        this.criteres = data;
        this.saving = false;
        this.successMessage = 'Critères enregistrés avec succès.';
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: () => {
        this.saving = false;
        this.errorMessage = "Échec de l'enregistrement des critères.";
      },
    });
  }
}
