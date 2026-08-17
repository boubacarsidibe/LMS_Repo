import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import {
  Annonce,
  CoursEtudiant,
  MaPromotion,
  Reprise,
  SeanceEtudiant,
} from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';
import { ScolariteService } from '../../services/scolarite.service';

/**
 * Tableau de bord de l'étudiant : une vue d'ensemble de sa scolarité.
 *
 * <p>Tous les chiffres affichés sont dérivés des données réelles — aucun
 * indicateur n'est écrit en dur, et une base vide produit des états vides
 * explicites plutôt que des valeurs de remplissage.
 */
@Component({
  selector: 'app-tableau-bord',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <h1 class="font-headline-lg text-headline-lg text-on-surface">Tableau de bord</h1>
        <p class="font-body-md text-body-md text-on-surface-variant mt-1">
          {{ libellePromotion || 'Votre scolarité en un coup d’œil' }}
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de votre scolarité…"></app-ui-spinner>

      <ng-container *ngIf="!chargement">
        <!-- Indicateurs, tous calculés -->
        <section class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-gutter">
          <article
            *ngFor="let tuile of tuiles"
            class="bg-surface-container-lowest rounded-xl p-stack-lg border border-outline-variant"
          >
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined text-[20px]">{{ tuile.icone }}</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">{{
                tuile.libelle
              }}</span>
            </div>
            <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">
              {{ tuile.valeur }}
            </p>
          </article>
        </section>

        <!-- Reprendre ou l'etudiant s'est arrete -->
        <section *ngIf="reprises.length > 0" class="space-y-stack-md">
          <h2 class="font-headline-sm text-headline-sm text-on-surface">Reprendre où vous en étiez</h2>
          <ul class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-gutter">
            <li *ngFor="let reprise of reprises">
              <a
                [routerLink]="['/espace-etudiant/mes-cours', reprise.coursId]"
                class="h-full bg-surface-container-lowest rounded-xl p-stack-lg border border-outline-variant hover:border-secondary transition-colors flex items-start gap-3 no-underline"
              >
                <span class="material-symbols-outlined text-on-surface-variant shrink-0">{{
                  icone(reprise)
                }}</span>
                <div class="min-w-0">
                  <p class="font-label-md text-label-md text-on-surface truncate">
                    {{ reprise.supportTitre }}
                  </p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant truncate">
                    {{ reprise.coursTitre }}
                  </p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                    Ouvert le {{ reprise.dateConsultation | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
              </a>
            </li>
          </ul>
        </section>

        <!-- Progression par cours -->
        <section class="space-y-stack-md">
          <div class="flex items-center justify-between">
            <h2 class="font-headline-sm text-headline-sm text-on-surface">Ma progression</h2>
            <a
              routerLink="/espace-etudiant/mes-cours"
              class="font-label-md text-label-md text-secondary hover:underline"
              >Tous mes cours</a
            >
          </div>

          <app-ui-empty-state
            *ngIf="cours.length === 0"
            icone="menu_book"
            titre="Aucun cours pour l’instant"
            description="Vous n’êtes rattaché à aucune formation, ou son programme n’a pas encore été renseigné par le service pédagogique."
          ></app-ui-empty-state>

          <div *ngIf="cours.length > 0" class="grid grid-cols-1 lg:grid-cols-2 gap-gutter">
            <a
              *ngFor="let c of cours"
              [routerLink]="['/espace-etudiant/mes-cours', c.id]"
              class="bg-surface-container-lowest rounded-xl p-stack-lg border border-outline-variant hover:border-secondary transition-colors block"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="font-label-sm text-label-sm text-on-surface-variant">{{ c.code }}</p>
                  <p class="font-label-md text-label-md text-on-surface">{{ c.titre }}</p>
                </div>
                <span class="font-label-md text-label-md text-secondary tabular-nums"
                  >{{ c.progression }}%</span
                >
              </div>
              <div class="mt-3 h-2 rounded-full bg-surface-container overflow-hidden">
                <div
                  class="h-full bg-secondary rounded-full transition-all"
                  [style.width.%]="c.progression"
                ></div>
              </div>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-2">
                {{ c.nbSupportsConsultes }} / {{ c.nbSupports }} support(s) consulté(s)
              </p>
            </a>
          </div>
        </section>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-gutter">
          <!-- Prochaines séances -->
          <section class="space-y-stack-md">
            <h2 class="font-headline-sm text-headline-sm text-on-surface">Prochaines séances</h2>
            <app-ui-empty-state
              *ngIf="prochainesSeances.length === 0"
              icone="event_busy"
              titre="Aucune séance à venir"
              description="Votre calendrier académique ne comporte pas de séance planifiée."
            ></app-ui-empty-state>
            <ul *ngIf="prochainesSeances.length > 0" class="space-y-base">
              <li
                *ngFor="let s of prochainesSeances"
                class="bg-surface-container-lowest rounded-xl p-stack-md border border-outline-variant flex gap-4"
              >
                <div
                  class="w-12 shrink-0 text-center rounded-lg bg-primary-container text-on-primary-container py-1"
                >
                  <p class="text-label-sm uppercase">{{ s.dateHeure | date: 'MMM' }}</p>
                  <p class="font-label-md tabular-nums">{{ s.dateHeure | date: 'dd' }}</p>
                </div>
                <div class="min-w-0">
                  <p class="font-label-md text-label-md text-on-surface truncate">{{ s.titre }}</p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant">
                    {{ s.dateHeure | date: 'HH:mm' }} · {{ s.dureeMinutes }} min ·
                    {{ s.lieuOuLien || 'Lieu à préciser' }}
                  </p>
                </div>
              </li>
            </ul>
          </section>

          <!-- Dernières communications -->
          <section class="space-y-stack-md">
            <div class="flex items-center justify-between">
              <h2 class="font-headline-sm text-headline-sm text-on-surface">Communications</h2>
              <a
                routerLink="/espace-etudiant/annonces"
                class="font-label-md text-label-md text-secondary hover:underline"
                >Tout voir</a
              >
            </div>
            <app-ui-empty-state
              *ngIf="dernieresAnnonces.length === 0"
              icone="campaign"
              titre="Aucune communication"
              description="Les annonces, messages et mémos de l’INPG s’afficheront ici."
            ></app-ui-empty-state>
            <ul *ngIf="dernieresAnnonces.length > 0" class="space-y-base">
              <li
                *ngFor="let a of dernieresAnnonces"
                class="bg-surface-container-lowest rounded-xl p-stack-md border border-outline-variant"
                [class.border-secondary]="!a.lue"
              >
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-[18px] text-on-surface-variant">{{
                    icone(a)
                  }}</span>
                  <p class="font-label-md text-label-md text-on-surface truncate">{{ a.titre }}</p>
                  <span
                    *ngIf="!a.lue"
                    class="ml-auto w-2 h-2 rounded-full bg-secondary shrink-0"
                    aria-label="Non lue"
                  ></span>
                </div>
                <p class="font-body-sm text-body-sm text-on-surface-variant mt-1 line-clamp-2">
                  {{ a.contenu }}
                </p>
              </li>
            </ul>
          </section>
        </div>
      </ng-container>
    </div>
  `,
})
export class TableauBord implements OnInit {
  chargement = true;
  libellePromotion = '';
  cours: CoursEtudiant[] = [];
  prochainesSeances: SeanceEtudiant[] = [];
  dernieresAnnonces: Annonce[] = [];
  reprises: Reprise[] = [];
  private nbNonLues = 0;

  constructor(
    private scolarite: ScolariteService,
    private communication: CommunicationService
  ) {}

  ngOnInit(): void {
    forkJoin({
      promotion: this.scolarite.getMaPromotion(),
      cours: this.scolarite.getMesCours(),
      calendrier: this.scolarite.getMonCalendrier(),
      annonces: this.communication.getAnnonces(),
      reprises: this.scolarite.getReprendre(),
    }).subscribe({
      next: ({ promotion, cours, calendrier, annonces, reprises }) => {
        this.libellePromotion = (promotion as MaPromotion).promotion
          ? ((promotion as MaPromotion).promotion as { libelle?: string }).libelle ?? ''
          : '';
        this.cours = cours;
        const maintenant = Date.now();
        this.prochainesSeances = calendrier
          .filter((s) => new Date(s.dateHeure).getTime() >= maintenant)
          .slice(0, 4);
        this.dernieresAnnonces = annonces.slice(0, 4);
        // Trois suffisent : au-dela, la reprise cesse d'etre un raccourci.
        this.reprises = (reprises ?? []).slice(0, 3);
        this.nbNonLues = annonces.filter((a) => !a.lue).length;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  get tuiles(): { libelle: string; valeur: string | number; icone: string }[] {
    const totalSupports = this.cours.reduce((somme, c) => somme + c.nbSupports, 0);
    const consultes = this.cours.reduce((somme, c) => somme + c.nbSupportsConsultes, 0);
    const moyenne =
      this.cours.length === 0
        ? 0
        : Math.round(this.cours.reduce((s, c) => s + c.progression, 0) / this.cours.length);
    return [
      { libelle: 'Cours suivis', valeur: this.cours.length, icone: 'menu_book' },
      { libelle: 'Progression', valeur: moyenne + '%', icone: 'trending_up' },
      { libelle: 'Supports lus', valeur: `${consultes} / ${totalSupports}`, icone: 'description' },
      { libelle: 'Non lues', valeur: this.nbNonLues, icone: 'campaign' },
    ];
  }

  /** Pictogramme d'une ressource, selon son type declare. */
  icone(element: Annonce | Reprise): string {
    if ('supportId' in element) {
      const type = (element.typeRessource || '').toUpperCase();
      if (type.includes('VIDEO')) return 'play_circle';
      if (type.includes('PDF') || type.includes('DOCUMENT')) return 'picture_as_pdf';
      if (type.includes('LIEN') || type.includes('URL')) return 'link';
      return 'description';
    }
    return this.iconeAnnonce(element);
  }

  private iconeAnnonce(annonce: Annonce): string {
    switch (annonce.type) {
      case 'MEMO':
        return 'sticky_note_2';
      case 'MESSAGE':
        return 'mail';
      default:
        return 'campaign';
    }
  }
}
