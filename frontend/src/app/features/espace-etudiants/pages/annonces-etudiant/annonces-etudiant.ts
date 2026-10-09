import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { Annonce, TypeAnnonce } from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';

/**
 * Annonces, messages et mémos de l'INPG (TDR §3.2).
 *
 * <p>Les trois natures distinguées par le cahier des charges sont filtrables :
 * ce ne sont pas de simples étiquettes décoratives.
 */
@Component({
  selector: 'app-annonces-etudiant',
  standalone: true,
  imports: [CommonModule, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Communications</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Annonces, messages et mémos de l’INPG.
          </p>
        </div>
        <button
          *ngIf="nbNonLues > 0"
          type="button"
          (click)="toutMarquerLu()"
          class="px-4 py-2 rounded-lg border border-outline-variant font-label-md text-label-md text-on-surface-variant hover:border-secondary hover:text-secondary transition-colors"
        >
          Tout marquer comme lu ({{ nbNonLues }})
        </button>
      </header>

      <div class="flex flex-wrap gap-2">
        <button
          type="button"
          (click)="filtre = ''"
          class="px-3 py-1.5 rounded-full font-label-sm text-label-sm border transition-colors"
          [ngClass]="classeFiltre('')"
        >
          Toutes ({{ annonces.length }})
        </button>
        <button
          *ngFor="let t of types"
          type="button"
          (click)="filtre = t.valeur"
          class="px-3 py-1.5 rounded-full font-label-sm text-label-sm border transition-colors"
          [ngClass]="classeFiltre(t.valeur)"
        >
          {{ t.libelle }} ({{ compter(t.valeur) }})
        </button>
      </div>

      <app-ui-spinner *ngIf="chargement" message="Chargement des communications…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && annoncesFiltrees.length === 0"
        icone="campaign"
        titre="Aucune communication"
        description="Les annonces, messages et mémos publiés par l’INPG apparaîtront ici."
      ></app-ui-empty-state>

      <ul *ngIf="!chargement" class="space-y-stack-md">
        <li
          *ngFor="let a of annoncesFiltrees"
          class="bg-surface-container-lowest rounded-xl border p-stack-lg transition-colors"
          [class.border-outline-variant]="a.lue"
          [class.border-secondary]="!a.lue"
        >
          <div class="flex flex-wrap items-center gap-3">
            <span
              class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full font-label-sm text-label-sm"
              [ngClass]="couleur(a.type)"
            >
              <span class="material-symbols-outlined text-[14px]">{{ icone(a.type) }}</span>
              {{ libelle(a.type) }}
            </span>
            <span
              *ngIf="a.epinglee"
              class="inline-flex items-center gap-1 font-label-sm text-label-sm text-on-surface-variant"
            >
              <span class="material-symbols-outlined text-[14px]">push_pin</span> Épinglée
            </span>
            <span class="ml-auto font-body-sm text-body-sm text-on-surface-variant">
              {{ a.datePublication | date: 'd MMMM y, HH:mm' }}
            </span>
          </div>

          <h2 class="font-headline-sm text-headline-sm text-on-surface mt-3">{{ a.titre }}</h2>
          <p class="font-body-md text-body-md text-on-surface-variant mt-2 whitespace-pre-line">
            {{ a.contenu }}
          </p>

          <div class="flex flex-wrap items-center gap-3 mt-4">
            <span class="font-body-sm text-body-sm text-on-surface-variant">
              {{ a.auteur }}<span *ngIf="a.promotionLibelle"> · {{ a.promotionLibelle }}</span>
            </span>
            <button
              *ngIf="!a.lue"
              type="button"
              (click)="marquerLue(a)"
              class="ml-auto px-3 py-1.5 rounded-lg bg-secondary text-on-secondary font-label-sm text-label-sm hover:opacity-90 transition-opacity"
            >
              Marquer comme lu
            </button>
          </div>
        </li>
      </ul>
    </div>
  `,
})
export class AnnoncesEtudiant implements OnInit {
  annonces: Annonce[] = [];
  chargement = true;
  filtre: TypeAnnonce | '' = '';

  readonly types: { valeur: TypeAnnonce; libelle: string }[] = [
    { valeur: 'ANNONCE', libelle: 'Annonces' },
    { valeur: 'MESSAGE', libelle: 'Messages' },
    { valeur: 'MEMO', libelle: 'Mémos' },
  ];

  constructor(private communication: CommunicationService) {}

  ngOnInit(): void {
    this.communication.getAnnonces().subscribe({
      next: (annonces) => {
        this.annonces = annonces;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  get annoncesFiltrees(): Annonce[] {
    return this.filtre ? this.annonces.filter((a) => a.type === this.filtre) : this.annonces;
  }

  get nbNonLues(): number {
    return this.annonces.filter((a) => !a.lue).length;
  }

  compter(type: TypeAnnonce): number {
    return this.annonces.filter((a) => a.type === type).length;
  }

  classeFiltre(valeur: string): string {
    return this.filtre === valeur
      ? 'bg-secondary text-on-secondary border-secondary'
      : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant hover:border-secondary';
  }

  marquerLue(annonce: Annonce): void {
    this.communication.marquerAnnonceLue(annonce.id).subscribe({
      next: () => (annonce.lue = true),
      error: () => undefined,
    });
  }

  toutMarquerLu(): void {
    this.communication.marquerToutesAnnoncesLues().subscribe({
      next: () => this.annonces.forEach((a) => (a.lue = true)),
      error: () => undefined,
    });
  }

  libelle(type: TypeAnnonce): string {
    return this.types.find((t) => t.valeur === type)?.libelle.replace(/s$/, '') ?? type;
  }

  icone(type: TypeAnnonce): string {
    switch (type) {
      case 'MEMO':
        return 'sticky_note_2';
      case 'MESSAGE':
        return 'mail';
      default:
        return 'campaign';
    }
  }

  couleur(type: TypeAnnonce): string {
    switch (type) {
      case 'MEMO':
        return 'bg-surface-container-high text-on-surface-variant';
      case 'MESSAGE':
        return 'bg-primary-fixed text-on-primary-container';
      default:
        return 'bg-secondary-container text-on-secondary-container';
    }
  }
}
