import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { ReleveDelivre } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/**
 * Relevés officiels délivrés à l'alumni durant sa scolarité (TDR §C, « accès à
 * leurs relevés »). Figés à leur date d'émission — voir ScolariteController.
 */
@Component({
  selector: 'app-alumni-mes-releves',
  standalone: true,
  imports: [CommonModule, UiSpinnerComponent, UiEmptyStateComponent],
  templateUrl: './mes-releves.html',
})
export class MesReleves implements OnInit {
  chargement = true;
  releves: ReleveDelivre[] = [];
  releveEnCoursId: number | null = null;
  erreur: string | null = null;

  constructor(private alumniService: AlumniService) {}

  ngOnInit(): void {
    this.alumniService.getMesReleves().subscribe({
      next: (releves) => {
        this.releves = releves || [];
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  telechargerReleve(releve: ReleveDelivre): void {
    this.releveEnCoursId = releve.id;
    this.erreur = null;
    this.alumniService.telechargerRelevePdf(releve.id).subscribe({
      next: (blob) => {
        this.releveEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `INPG-releve-${releve.reference}.pdf`;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.releveEnCoursId = null;
        this.erreur = "Le relevé n'a pas pu être téléchargé. Réessayez dans un instant.";
      },
    });
  }
}
