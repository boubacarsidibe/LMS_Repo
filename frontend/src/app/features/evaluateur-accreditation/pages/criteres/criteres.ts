import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CritereAccreditation } from '../../../accreditation/models/accreditation.models';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';

/**
 * Gestion des critères d'accréditation (référentiel consulté librement par les
 * évaluateurs en rédigeant leur commentaire — pas de grille de notation
 * structurée, cf. LOG_BLOQUANT.md : le TDR ne détaille aucun barème).
 */
@Component({
  selector: 'app-evaluateur-criteres',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Critères d'accréditation</h1>
        <p class="font-body-md text-body-md text-on-surface-variant mt-1">
          Référentiel consulté par les évaluateurs lors de l'examen d'une demande.
        </p>
      </header>

      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3">
        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
          <span class="material-symbols-outlined text-secondary">add_circle</span>
          Nouveau critère
        </h2>
        <div class="grid sm:grid-cols-2 gap-3">
          <input
            type="text"
            [(ngModel)]="nouveauLibelle"
            placeholder="Libellé"
            class="p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
          <input
            type="text"
            [(ngModel)]="nouvelleDescription"
            placeholder="Description (facultative)"
            class="p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
        <button
          type="button"
          (click)="creer()"
          [disabled]="!nouveauLibelle.trim() || creationEnCours"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
        >
          Ajouter
        </button>
      </div>

      <app-ui-spinner *ngIf="chargement" message="Chargement des critères…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && criteres.length === 0"
        icone="checklist"
        titre="Aucun critère"
        description="Ajoutez un premier critère pour guider l'évaluation des demandes."
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && criteres.length > 0" class="space-y-base">
        <li
          *ngFor="let c of criteres"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg flex items-start justify-between gap-4"
        >
          <div>
            <p class="font-label-md text-label-md text-on-surface">{{ c.libelle }}</p>
            <p *ngIf="c.description" class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ c.description }}</p>
            <span
              class="inline-block mt-2 px-2 py-0.5 rounded-full font-label-sm text-label-sm"
              [ngClass]="c.actif ? 'bg-success/15 text-success' : 'bg-surface-container text-on-surface-variant'"
            >
              {{ c.actif ? 'Actif' : 'Désactivé' }}
            </span>
          </div>
          <div class="flex gap-2 shrink-0">
            <button
              *ngIf="c.actif"
              type="button"
              (click)="desactiver(c)"
              class="p-2 text-on-surface-variant hover:bg-surface-container rounded-full transition-colors"
              title="Désactiver"
              aria-label="Désactiver"
            >
              <span class="material-symbols-outlined text-[20px]">visibility_off</span>
            </button>
            <button
              type="button"
              (click)="supprimer(c)"
              class="p-2 text-on-surface-variant hover:bg-error-container hover:text-on-error-container rounded-full transition-colors"
              title="Supprimer"
              aria-label="Supprimer"
            >
              <span class="material-symbols-outlined text-[20px]">delete</span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  `,
})
export class EvaluateurCriteresComponent implements OnInit {
  private evaluateurService = inject(EvaluateurAccreditationService);

  criteres: CritereAccreditation[] = [];
  chargement = true;

  nouveauLibelle = '';
  nouvelleDescription = '';
  creationEnCours = false;
  erreur = '';

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement = true;
    this.evaluateurService.getCriteres().subscribe({
      next: (criteres) => {
        this.criteres = criteres;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  creer(): void {
    if (!this.nouveauLibelle.trim()) {
      return;
    }
    this.creationEnCours = true;
    this.erreur = '';
    this.evaluateurService.creerCritere(this.nouveauLibelle.trim(), this.nouvelleDescription.trim() || null).subscribe({
      next: (critere) => {
        this.creationEnCours = false;
        this.criteres = [...this.criteres, critere];
        this.nouveauLibelle = '';
        this.nouvelleDescription = '';
      },
      error: (e) => {
        this.creationEnCours = false;
        this.erreur = e?.error?.message || "La création du critère a échoué.";
      },
    });
  }

  desactiver(critere: CritereAccreditation): void {
    this.evaluateurService.desactiverCritere(critere.id).subscribe({
      next: (mis) => (this.criteres = this.criteres.map((c) => (c.id === mis.id ? mis : c))),
    });
  }

  supprimer(critere: CritereAccreditation): void {
    this.evaluateurService.supprimerCritere(critere.id).subscribe({
      next: () => (this.criteres = this.criteres.filter((c) => c.id !== critere.id)),
    });
  }
}
