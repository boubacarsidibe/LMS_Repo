import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { CoursEtudiant, EvaluationCours } from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';
import { ScolariteService } from '../../services/scolarite.service';

/**
 * Évaluations de satisfaction de l'étudiant (TDR §3.7).
 *
 * <p>Distingue explicitement les cours déjà évalués de ceux qui restent à
 * évaluer, pour que la relance soit visible sans notification intrusive.
 */
@Component({
  selector: 'app-mes-evaluations',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <h1 class="font-display text-display text-on-surface leading-tight">Mes évaluations</h1>
        <p class="font-body-md text-body-md text-on-surface-variant mt-1">
          Votre appréciation des cours suivis alimente le suivi qualité de l’INPG.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de vos évaluations…"></app-ui-spinner>

      <ng-container *ngIf="!chargement">
        <section *ngIf="aEvaluer.length > 0" class="space-y-stack-md">
          <h2 class="font-headline-sm text-headline-sm text-on-surface">
            À évaluer ({{ aEvaluer.length }})
          </h2>
          <ul class="grid grid-cols-1 md:grid-cols-2 gap-gutter">
            <li
              *ngFor="let c of aEvaluer"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg flex items-center justify-between gap-4 shadow-sm"
            >
              <div class="min-w-0">
                <p class="font-label-sm text-label-sm text-on-surface-variant">{{ c.code }}</p>
                <p class="font-label-md text-label-md text-on-surface truncate">{{ c.titre }}</p>
              </div>
              <a
                [routerLink]="['/espace-etudiant/mes-cours', c.id]"
                class="px-3 py-1.5 rounded-lg bg-secondary text-on-secondary font-label-sm text-label-sm hover:opacity-90 transition-opacity shrink-0"
                >Évaluer</a
              >
            </li>
          </ul>
        </section>

        <section class="space-y-stack-md">
          <h2 class="font-headline-sm text-headline-sm text-on-surface">
            Évaluations déposées ({{ evaluations.length }})
          </h2>

          <app-ui-empty-state
            *ngIf="evaluations.length === 0"
            icone="reviews"
            titre="Aucune évaluation déposée"
            description="Évaluez vos cours depuis leur fiche pour contribuer à l’amélioration des formations."
          ></app-ui-empty-state>

          <ul *ngIf="evaluations.length > 0" class="space-y-base">
            <li
              *ngFor="let e of evaluations"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg shadow-sm"
            >
              <div class="flex flex-wrap items-center gap-3">
                <a
                  [routerLink]="['/espace-etudiant/mes-cours', e.coursId]"
                  class="font-label-md text-label-md text-on-surface hover:text-secondary transition-colors"
                  >{{ e.coursTitre }}</a
                >
                <span class="flex items-center gap-0.5" [attr.aria-label]="e.note + ' sur 5'">
                  <span
                    *ngFor="let etoile of [1, 2, 3, 4, 5]"
                    class="material-symbols-outlined text-[16px]"
                    [class.text-secondary]="e.note >= etoile"
                    [class.text-outline-variant]="e.note < etoile"
                    [style.font-variation-settings]="
                      e.note >= etoile ? &quot;'FILL' 1&quot; : &quot;'FILL' 0&quot;
                    "
                    >star</span
                  >
                </span>
                <span class="ml-auto font-body-sm text-body-sm text-on-surface-variant">
                  {{ e.dateSoumission | date: 'd MMMM y' }}
                </span>
              </div>
              <p
                *ngIf="e.commentaire"
                class="font-body-md text-body-md text-on-surface-variant mt-2 whitespace-pre-line"
              >
                {{ e.commentaire }}
              </p>
            </li>
          </ul>
        </section>
      </ng-container>
    </div>
  `,
})
export class MesEvaluations implements OnInit {
  evaluations: EvaluationCours[] = [];
  cours: CoursEtudiant[] = [];
  chargement = true;

  constructor(
    private communication: CommunicationService,
    private scolarite: ScolariteService
  ) {}

  ngOnInit(): void {
    forkJoin({
      evaluations: this.communication.getMesEvaluations(),
      cours: this.scolarite.getMesCours(),
    }).subscribe({
      next: ({ evaluations, cours }) => {
        this.evaluations = evaluations;
        this.cours = cours;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  /** Cours du cursus pour lesquels aucune note n'a encore été déposée. */
  get aEvaluer(): CoursEtudiant[] {
    return this.cours.filter((c) => c.maNote === null || c.maNote === undefined);
  }
}
