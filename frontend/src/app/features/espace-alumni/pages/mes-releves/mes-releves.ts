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
}
