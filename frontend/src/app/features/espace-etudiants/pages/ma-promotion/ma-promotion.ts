import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { PromotionDetail } from '../../models/scolarite.models';
import { ScolariteService } from '../../services/scolarite.service';

/**
 * Fiche de promotion (TDR §3.2) : programme, calendrier, activités
 * parascolaires, et l'identifiant unique de l'étudiant.
 */
@Component({
  selector: 'app-ma-promotion',
  standalone: true,
  imports: [CommonModule, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-4xl mx-auto">
      <header>
        <h1 class="font-display text-display text-on-surface leading-tight">Ma promotion</h1>
        <p class="font-body-md text-body-md text-on-surface-variant mt-1">
          Votre cursus et les informations archivées par l’INPG.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de votre promotion…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && !promotion"
        icone="groups"
        titre="Aucune promotion"
        [description]="message || 'Vous n’êtes rattaché à aucune promotion. Contactez le service de scolarité.'"
      ></app-ui-empty-state>

      <ng-container *ngIf="!chargement && promotion">
        <section
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg shadow-sm"
        >
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="font-label-sm text-label-sm text-on-surface-variant">
                {{ promotion.code }}
              </p>
              <h2 class="font-headline-sm text-headline-sm text-on-surface">
                {{ promotion.libelle }}
              </h2>
            </div>
            <span
              class="px-3 py-1 rounded-full font-label-sm text-label-sm"
              [ngClass]="
                promotion.enCours
                  ? 'bg-secondary-container text-on-secondary-container'
                  : 'bg-surface-container text-on-surface-variant'
              "
            >
              {{ promotion.enCours ? 'En cours' : 'Terminée' }}
            </span>
          </div>

          <dl class="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6">
            <div>
              <dt class="font-label-sm text-label-sm text-on-surface-variant">Mon matricule</dt>
              <dd class="font-label-md text-label-md text-on-surface tabular-nums">
                {{ matricule }}
              </dd>
            </div>
            <div>
              <dt class="font-label-sm text-label-sm text-on-surface-variant">Effectif</dt>
              <dd class="font-label-md text-label-md text-on-surface tabular-nums">
                {{ promotion.effectif }}
              </dd>
            </div>
            <div>
              <dt class="font-label-sm text-label-sm text-on-surface-variant">Cours</dt>
              <dd class="font-label-md text-label-md text-on-surface tabular-nums">
                {{ promotion.nbCours }}
              </dd>
            </div>
            <div>
              <dt class="font-label-sm text-label-sm text-on-surface-variant">Période</dt>
              <dd class="font-label-md text-label-md text-on-surface">
                {{ promotion.dateDebut | date: 'MM/y' }} — {{ promotion.dateFin | date: 'MM/y' }}
              </dd>
            </div>
          </dl>
        </section>

        <section
          *ngIf="promotion.formationIntitule"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg shadow-sm"
        >
          <h2 class="font-headline-sm text-headline-sm text-on-surface">Formation suivie</h2>
          <p class="font-label-md text-label-md text-on-surface mt-2">
            {{ promotion.formationIntitule }}
            <span class="text-on-surface-variant">({{ promotion.formationCode }})</span>
          </p>
          <p
            *ngIf="promotion.formationDescription"
            class="font-body-md text-body-md text-on-surface-variant mt-2"
          >
            {{ promotion.formationDescription }}
          </p>
        </section>

        <section
          *ngIf="promotion.programme"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg shadow-sm"
        >
          <h2 class="font-headline-sm text-headline-sm text-on-surface">Programme</h2>
          <p class="font-body-md text-body-md text-on-surface-variant mt-2 whitespace-pre-line">
            {{ promotion.programme }}
          </p>
        </section>

        <section
          *ngIf="promotion.activitesParascolaires"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg shadow-sm"
        >
          <h2 class="font-headline-sm text-headline-sm text-on-surface">
            Activités, sorties et visites
          </h2>
          <p class="font-body-md text-body-md text-on-surface-variant mt-2 whitespace-pre-line">
            {{ promotion.activitesParascolaires }}
          </p>
        </section>
      </ng-container>
    </div>
  `,
})
export class MaPromotionPage implements OnInit {
  promotion: PromotionDetail | null = null;
  matricule = '';
  message = '';
  chargement = true;

  constructor(private scolarite: ScolariteService) {}

  ngOnInit(): void {
    this.scolarite.getMaPromotion().subscribe({
      next: (reponse) => {
        this.matricule = reponse.matricule;
        this.message = reponse.message ?? '';
        const promotion = reponse.promotion as PromotionDetail;
        this.promotion = promotion && promotion.id ? promotion : null;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }
}
