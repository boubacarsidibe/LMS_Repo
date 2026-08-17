import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { SeanceEtudiant } from '../../models/scolarite.models';
import { ScolariteService } from '../../services/scolarite.service';

interface CaseCalendrier {
  date: Date | null;
  seances: SeanceEtudiant[];
}

/**
 * Calendrier académique de l'étudiant (TDR §3.2).
 *
 * <p>Alimenté par les séances réelles des cours autorisés — l'ancienne page
 * équivalente côté formateur reposait sur un tableau de démonstration.
 */
@Component({
  selector: 'app-calendrier-etudiant',
  standalone: true,
  imports: [CommonModule, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface">Calendrier académique</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Les séances de vos cours.
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button
            type="button"
            (click)="moisPrecedent()"
            aria-label="Mois précédent"
            class="p-2 rounded-lg border border-outline-variant text-on-surface-variant hover:border-secondary hover:text-secondary transition-colors"
          >
            <span class="material-symbols-outlined text-[20px]">chevron_left</span>
          </button>
          <span class="font-label-md text-label-md text-on-surface min-w-[10rem] text-center">
            {{ nomMois }} {{ annee }}
          </span>
          <button
            type="button"
            (click)="moisSuivant()"
            aria-label="Mois suivant"
            class="p-2 rounded-lg border border-outline-variant text-on-surface-variant hover:border-secondary hover:text-secondary transition-colors"
          >
            <span class="material-symbols-outlined text-[20px]">chevron_right</span>
          </button>
        </div>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement du calendrier…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && seances.length === 0"
        icone="event_busy"
        titre="Aucune séance planifiée"
        description="Votre formation ne comporte pas encore de séance au calendrier."
      ></app-ui-empty-state>

      <section
        *ngIf="!chargement && seances.length > 0"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden"
      >
        <div class="grid grid-cols-7 bg-surface-container-low border-b border-outline-variant">
          <div
            *ngFor="let jour of joursSemaine"
            class="p-2 text-center font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider"
          >
            {{ jour }}
          </div>
        </div>
        <div class="grid grid-cols-7">
          <div
            *ngFor="let c of cases"
            class="min-h-[7rem] border-b border-r border-outline-variant p-2 last:border-r-0"
            [class.bg-surface-container-low]="!c.date"
          >
            <ng-container *ngIf="c.date">
              <span
                class="font-label-sm text-label-sm tabular-nums"
                [class.text-secondary]="estAujourdhui(c.date)"
                [class.font-bold]="estAujourdhui(c.date)"
                [class.text-on-surface-variant]="!estAujourdhui(c.date)"
                >{{ c.date.getDate() }}</span
              >
              <ul class="mt-1 space-y-1">
                <li
                  *ngFor="let s of c.seances"
                  class="px-1.5 py-1 rounded text-[11px] leading-tight"
                  [ngClass]="couleur(s.type)"
                  [title]="s.titre + ' — ' + (s.lieuOuLien || 'lieu à préciser')"
                >
                  <span class="tabular-nums">{{ s.dateHeure | date: 'HH:mm' }}</span>
                  <span class="block truncate">{{ s.coursTitre || s.titre }}</span>
                </li>
              </ul>
            </ng-container>
          </div>
        </div>
      </section>

      <section *ngIf="!chargement && seancesDuMois.length > 0" class="space-y-stack-md">
        <h2 class="font-headline-sm text-headline-sm text-on-surface">Détail du mois</h2>
        <ul class="space-y-base">
          <li
            *ngFor="let s of seancesDuMois"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-md flex flex-wrap items-center gap-4"
          >
            <span
              class="px-2 py-0.5 rounded-full font-label-sm text-label-sm"
              [ngClass]="couleur(s.type)"
              >{{ s.type }}</span
            >
            <div class="min-w-0 flex-1">
              <p class="font-label-md text-label-md text-on-surface">{{ s.titre }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant">
                {{ s.dateHeure | date: 'EEEE d MMMM, HH:mm' }} · {{ s.dureeMinutes }} min ·
                {{ s.lieuOuLien || 'Lieu à préciser' }}
              </p>
            </div>
          </li>
        </ul>
      </section>
    </div>
  `,
})
export class CalendrierEtudiant implements OnInit {
  seances: SeanceEtudiant[] = [];
  chargement = true;
  annee = new Date().getFullYear();
  mois = new Date().getMonth();

  readonly joursSemaine = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  private readonly nomsMois = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
  ];

  constructor(private scolarite: ScolariteService) {}

  ngOnInit(): void {
    this.scolarite.getMonCalendrier().subscribe({
      next: (seances) => {
        this.seances = seances;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  get nomMois(): string {
    return this.nomsMois[this.mois];
  }

  get seancesDuMois(): SeanceEtudiant[] {
    return this.seances.filter((s) => {
      const d = new Date(s.dateHeure);
      return d.getFullYear() === this.annee && d.getMonth() === this.mois;
    });
  }

  /** Grille du mois, semaine commençant le lundi. */
  get cases(): CaseCalendrier[] {
    const premier = new Date(this.annee, this.mois, 1);
    const nbJours = new Date(this.annee, this.mois + 1, 0).getDate();
    const decalage = (premier.getDay() + 6) % 7;

    const cases: CaseCalendrier[] = [];
    for (let i = 0; i < decalage; i++) {
      cases.push({ date: null, seances: [] });
    }
    for (let jour = 1; jour <= nbJours; jour++) {
      const date = new Date(this.annee, this.mois, jour);
      cases.push({
        date,
        seances: this.seancesDuMois.filter((s) => new Date(s.dateHeure).getDate() === jour),
      });
    }
    while (cases.length % 7 !== 0) {
      cases.push({ date: null, seances: [] });
    }
    return cases;
  }

  estAujourdhui(date: Date): boolean {
    const now = new Date();
    return (
      date.getDate() === now.getDate() &&
      date.getMonth() === now.getMonth() &&
      date.getFullYear() === now.getFullYear()
    );
  }

  couleur(type: string): string {
    switch ((type || '').toLowerCase()) {
      case 'examen':
        return 'bg-error-container text-on-error-container';
      case 'tp':
        return 'bg-secondary-container text-on-secondary-container';
      case 'td':
        return 'bg-primary-fixed text-on-primary-container';
      default:
        return 'bg-surface-container-high text-on-surface-variant';
    }
  }

  moisPrecedent(): void {
    if (this.mois === 0) {
      this.mois = 11;
      this.annee--;
    } else {
      this.mois--;
    }
  }

  moisSuivant(): void {
    if (this.mois === 11) {
      this.mois = 0;
      this.annee++;
    } else {
      this.mois++;
    }
  }
}
