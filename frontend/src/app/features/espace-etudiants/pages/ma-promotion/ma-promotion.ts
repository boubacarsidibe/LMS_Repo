import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { Camarade, PromotionDetail } from '../../models/scolarite.models';
import { ScolariteService } from '../../services/scolarite.service';

/**
 * Fiche de promotion (TDR §3.2) : programme, calendrier, activités
 * parascolaires, et l'identifiant unique de l'étudiant.
 */
@Component({
  selector: 'app-ma-promotion',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-4xl mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Ma promotion</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Votre cursus et les informations archivées par l’INPG.
          </p>
        </div>
        <a
          *ngIf="!chargement && promotion"
          [routerLink]="['/espace-etudiant/forum']"
          class="px-4 py-2 rounded-lg border border-outline-variant text-on-surface font-label-md text-label-md hover:border-secondary hover:text-secondary transition-colors flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-[18px]">forum</span>Forum de la promotion
        </a>
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

        <section class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm overflow-hidden">
          <button
            type="button"
            (click)="camaradesOuvert = !camaradesOuvert; chargerCamaradesSiBesoin()"
            class="w-full flex items-center justify-between p-stack-lg text-left"
          >
            <h2 class="font-headline-sm text-headline-sm text-on-surface">
              Camarades de promotion
              <span *ngIf="camarades.length > 0" class="text-on-surface-variant font-body-md text-body-md">({{ camarades.length }})</span>
            </h2>
            <span class="material-symbols-outlined text-on-surface-variant">{{ camaradesOuvert ? 'expand_less' : 'expand_more' }}</span>
          </button>
          <div *ngIf="camaradesOuvert" class="px-stack-lg pb-stack-lg">
            <p *ngIf="chargementCamarades" class="font-body-sm text-body-sm text-on-surface-variant">Chargement…</p>
            <ul *ngIf="!chargementCamarades && camarades.length > 0" class="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <li
                *ngFor="let c of camarades"
                class="flex items-center justify-between gap-2 px-4 py-2 rounded-lg bg-surface-container-low"
              >
                <span class="font-body-sm text-body-sm text-on-surface">{{ c.nom }}</span>
                <span class="font-body-sm text-body-sm text-on-surface-variant tabular-nums">{{ c.matricule || '—' }}</span>
              </li>
            </ul>
            <p *ngIf="!chargementCamarades && camarades.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">
              Aucun autre étudiant dans cette promotion pour l’instant.
            </p>
          </div>
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

  // Divulgation progressive : le répertoire ne se charge qu'à l'ouverture,
  // pour ne pas alourdir un écran déjà chargé d'informations.
  camaradesOuvert = false;
  camarades: Camarade[] = [];
  chargementCamarades = false;
  private camaradesCharges = false;

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

  chargerCamaradesSiBesoin(): void {
    if (this.camaradesCharges || !this.camaradesOuvert) {
      return;
    }
    this.camaradesCharges = true;
    this.chargementCamarades = true;
    this.scolarite.getCamarades().subscribe({
      next: (camarades) => {
        this.camarades = camarades || [];
        this.chargementCamarades = false;
      },
      error: () => (this.chargementCamarades = false),
    });
  }
}
