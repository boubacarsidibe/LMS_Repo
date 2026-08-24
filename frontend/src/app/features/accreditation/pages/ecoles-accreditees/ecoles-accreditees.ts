import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { EcoleAccreditee } from '../../models/accreditation.models';
import { AccreditationService } from '../../services/accreditation.service';

/**
 * Liste publique des écoles accréditées (TDR §F : « processus transparent »).
 * Accessible sans compte — endpoint {@code /api/accreditation/public/**}.
 */
@Component({
  selector: 'app-ecoles-accreditees',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Accréditation
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Écoles accréditées</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Ces écoles de formation ont été accréditées par l'INPG pour dispenser des formations dans
          le domaine du pétrole et du gaz.
        </p>
      </header>

      <div class="bg-primary-container rounded-xl p-stack-lg flex flex-wrap items-center justify-between gap-4">
        <p class="font-body-md text-body-md text-on-primary-container max-w-xl">
          Vous représentez une école de formation et souhaitez être accréditée par l'INPG ?
        </p>
        <a
          routerLink="/accreditation"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity no-underline shrink-0"
        >
          Déposer une demande
        </a>
      </div>

      <app-ui-spinner *ngIf="chargement" message="Chargement de la liste…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && ecoles.length === 0"
        icone="school"
        titre="Aucune école accréditée pour l'instant"
        description="Cette liste s'enrichira au fur et à mesure des accréditations délivrées par l'INPG."
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && ecoles.length > 0" class="grid sm:grid-cols-2 gap-base">
        <li
          *ngFor="let e of ecoles"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg flex items-center gap-3"
        >
          <span class="material-symbols-outlined text-secondary text-[28px]">verified</span>
          <div>
            <p class="font-label-md text-label-md text-on-surface">{{ e.nomEcole }}</p>
            <p class="font-body-sm text-body-sm text-on-surface-variant">
              Accréditée le {{ e.dateAccreditation | date: 'd MMMM y' }}
            </p>
          </div>
        </li>
      </ul>
    </div>
  `,
})
export class EcolesAccrediteesComponent implements OnInit {
  private accreditationService = inject(AccreditationService);

  ecoles: EcoleAccreditee[] = [];
  chargement = true;

  ngOnInit(): void {
    this.accreditationService.getEcolesAccreditees().subscribe({
      next: (ecoles) => {
        this.ecoles = ecoles;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }
}
