import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { CoursEtudiant } from '../../models/scolarite.models';
import { ScolariteService } from '../../services/scolarite.service';

/**
 * « Les cours autorisés » du cahier des charges §3.2.
 *
 * <p>Contrairement à l'ancienne page « Mes formations », qui listait le catalogue
 * complet avec des compteurs écrits en dur, cette page n'affiche que les cours au
 * programme de la formation suivie par la promotion de l'étudiant, et tous ses
 * compteurs sont dérivés de la réponse du serveur.
 */
@Component({
  selector: 'app-mes-cours-autorises',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Mes cours</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Les cours au programme de votre formation.
          </p>
        </div>
        <div
          class="flex items-center gap-2 bg-surface-container-low border border-outline-variant rounded-lg px-3 py-2 w-full sm:w-80 focus-within:border-secondary transition-colors"
        >
          <span class="material-symbols-outlined text-outline text-[20px]">search</span>
          <input
            type="text"
            [(ngModel)]="recherche"
            placeholder="Rechercher un cours"
            aria-label="Rechercher un cours"
            class="bg-transparent border border-outline-variant outline-none w-full font-body-sm text-body-sm text-on-surface"
          />
        </div>
      </header>

      <div *ngIf="semestres.length > 1" class="flex flex-wrap gap-2">
        <button
          type="button"
          (click)="semestreActif = ''"
          class="px-3 py-1.5 rounded-full font-label-sm text-label-sm border transition-colors"
          [ngClass]="
            semestreActif === ''
              ? 'bg-secondary text-on-secondary border-secondary'
              : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant hover:border-secondary'
          "
        >
          Tous ({{ cours.length }})
        </button>
        <button
          *ngFor="let s of semestres"
          type="button"
          (click)="semestreActif = s"
          class="px-3 py-1.5 rounded-full font-label-sm text-label-sm border transition-colors"
          [ngClass]="
            semestreActif === s
              ? 'bg-secondary text-on-secondary border-secondary'
              : 'bg-surface-container-lowest text-on-surface-variant border-outline-variant hover:border-secondary'
          "
        >
          {{ s }} ({{ compter(s) }})
        </button>
      </div>

      <app-ui-spinner *ngIf="chargement" message="Chargement de vos cours…"></app-ui-spinner>

      <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>

      <app-ui-empty-state
        *ngIf="!chargement && !erreur && cours.length === 0"
        icone="school"
        titre="Aucun cours autorisé"
        description="Vous n’êtes rattaché à aucune promotion, ou le programme de votre formation n’a pas encore été défini. Contactez le service de scolarité."
      ></app-ui-empty-state>

      <app-ui-empty-state
        *ngIf="!chargement && cours.length > 0 && coursFiltres.length === 0"
        icone="search_off"
        titre="Aucun résultat"
        description="Aucun cours ne correspond à votre recherche."
      ></app-ui-empty-state>

      <section
        *ngIf="coursFiltres.length > 0"
        class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-gutter"
      >
        <a
          *ngFor="let c of coursFiltres"
          [routerLink]="['/espace-etudiant/mes-cours', c.id]"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden hover:border-secondary transition-colors flex flex-col shadow-sm"
        >
          <div class="h-28 bg-primary-container flex items-center justify-center">
            <img
              *ngIf="c.imageUrl; else symbole"
              [src]="c.imageUrl"
              [alt]="'Illustration du cours ' + c.titre"
              class="w-full h-full object-cover"
            />
            <ng-template #symbole>
              <span class="material-symbols-outlined text-on-primary-container text-[36px]">menu_book</span>
            </ng-template>
          </div>

          <div class="p-stack-md flex-1 flex flex-col gap-2">
            <div class="flex items-center justify-between gap-2">
              <span class="font-label-sm text-label-sm text-on-surface-variant">{{ c.code }}</span>
              <span
                *ngIf="c.semestre"
                class="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm"
                >{{ c.semestre }}</span
              >
            </div>
            <p class="font-label-md text-label-md text-on-surface">{{ c.titre }}</p>
            <p class="font-body-sm text-body-sm text-on-surface-variant line-clamp-2 flex-1">
              {{ c.description }}
            </p>
            <p *ngIf="c.formateurs.length" class="font-body-sm text-body-sm text-on-surface-variant">
              {{ c.formateurs.join(', ') }}
            </p>

            <div class="mt-1">
              <div class="flex items-center justify-between font-label-sm text-label-sm mb-1">
                <span class="text-on-surface-variant"
                  >{{ c.nbSupportsConsultes }} / {{ c.nbSupports }} support(s)</span
                >
                <span class="text-secondary tabular-nums">{{ c.progression }}%</span>
              </div>
              <div class="h-1.5 rounded-full bg-surface-container overflow-hidden">
                <div class="h-full bg-secondary rounded-full" [style.width.%]="c.progression"></div>
              </div>
            </div>
          </div>
        </a>
      </section>
    </div>
  `,
})
export class MesCoursAutorises implements OnInit {
  cours: CoursEtudiant[] = [];
  chargement = true;
  erreur = '';
  recherche = '';
  semestreActif = '';

  constructor(private scolarite: ScolariteService) {}

  ngOnInit(): void {
    this.scolarite.getMesCours().subscribe({
      next: (cours) => {
        this.cours = cours;
        this.chargement = false;
      },
      error: (e) => {
        // Pas de repli sur des données fictives : mieux vaut dire que le
        // chargement a échoué que d'afficher un cursus inventé.
        this.erreur = e?.error?.message || 'Le chargement de vos cours a échoué.';
        this.chargement = false;
      },
    });
  }

  get semestres(): string[] {
    return [...new Set(this.cours.map((c) => c.semestre).filter((s) => !!s))].sort();
  }

  compter(semestre: string): number {
    return this.cours.filter((c) => c.semestre === semestre).length;
  }

  get coursFiltres(): CoursEtudiant[] {
    const terme = this.recherche.trim().toLowerCase();
    return this.cours.filter((c) => {
      const correspondSemestre = !this.semestreActif || c.semestre === this.semestreActif;
      const correspondTerme =
        !terme ||
        c.titre.toLowerCase().includes(terme) ||
        c.code.toLowerCase().includes(terme) ||
        c.description.toLowerCase().includes(terme);
      return correspondSemestre && correspondTerme;
    });
  }
}
