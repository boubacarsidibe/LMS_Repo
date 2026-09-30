import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import {
  DemandeEntreprise,
  LIBELLES_STATUT_DEMANDE_ENTREPRISE,
  LIBELLES_TYPE_DEMANDE,
} from '../../models/entreprise.models';
import { EntrepriseService } from '../../services/entreprise.service';

/**
 * Page d'accueil de l'espace Entreprise (TDR §H).
 *
 * <p>L'onglet « Espace entreprise » du navbar menait auparavant directement au
 * formulaire de nouvelle demande (`redirectTo: 'nouvelle-demande'`) : une
 * entreprise qui revient consulter le suivi de ses dossiers tombait donc sur
 * un formulaire vide plutôt que sur une vue d'ensemble. Cette page reprend le
 * patron des « vue d'ensemble » des autres espaces (tuiles de chiffres clés,
 * dernières demandes) et laisse `nouvelle-demande` intact pour le dépôt et le
 * suivi détaillé (fiche technique, devis, convention).
 */
@Component({
  selector: 'app-entreprise-accueil',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Espace Entreprise
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">
          Bienvenue{{ raisonSociale ? ', ' + raisonSociale : '' }}
        </h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Suivez vos demandes de formation, consultez le catalogue et échangez avec l'équipe pédagogique.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de votre espace…"></app-ui-spinner>

      <ng-container *ngIf="!chargement">
        <section class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-gutter" aria-label="Indicateurs">
          <a
            routerLink="/espace-entreprise/nouvelle-demande"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg no-underline hover:border-secondary transition-colors"
          >
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined">folder_shared</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">Demandes déposées</span>
            </div>
            <p class="font-display text-headline-lg text-on-surface mt-5 tabular-nums">{{ demandes.length }}</p>
          </a>
          <a
            routerLink="/espace-entreprise/nouvelle-demande"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg no-underline hover:border-secondary transition-colors"
          >
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined">hourglass_top</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">En attente de réponse</span>
            </div>
            <p class="font-display text-headline-lg text-on-surface mt-5 tabular-nums">{{ enAttente }}</p>
          </a>
          <a
            routerLink="/espace-entreprise/nouvelle-demande"
            class="rounded-xl border shadow-sm p-stack-lg no-underline transition-colors"
            [ngClass]="
              actionRequise > 0
                ? 'bg-error-container/20 border-error-container hover:border-error'
                : 'bg-surface-container-lowest border-outline-variant hover:border-secondary'
            "
          >
            <div class="flex items-center gap-2" [ngClass]="actionRequise > 0 ? 'text-on-error-container' : 'text-on-surface-variant'">
              <span class="material-symbols-outlined">flag</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">Fiches à valider</span>
            </div>
            <p class="font-display text-headline-lg mt-5 tabular-nums" [ngClass]="actionRequise > 0 ? 'text-on-error-container' : 'text-on-surface'">
              {{ actionRequise }}
            </p>
          </a>
          <a
            routerLink="/espace-entreprise/catalogue"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg no-underline hover:border-secondary transition-colors"
          >
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined">menu_book</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">Formations au catalogue</span>
            </div>
            <p class="font-display text-headline-lg text-on-surface mt-5 tabular-nums">{{ catalogueCount }}</p>
          </a>
        </section>

        <section class="grid grid-cols-1 sm:grid-cols-3 gap-gutter" aria-label="Actions rapides">
          <a
            routerLink="/espace-entreprise/nouvelle-demande"
            class="flex items-center gap-3 bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg no-underline hover:border-secondary transition-colors"
          >
            <span class="material-symbols-outlined text-secondary text-[28px]">add_business</span>
            <span class="font-label-md text-label-md text-on-surface">Déposer une nouvelle demande</span>
          </a>
          <a
            routerLink="/espace-entreprise/rendez-vous"
            class="flex items-center gap-3 bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg no-underline hover:border-secondary transition-colors"
          >
            <span class="material-symbols-outlined text-secondary text-[28px]">event</span>
            <span class="font-label-md text-label-md text-on-surface">Prendre rendez-vous</span>
          </a>
          <a
            routerLink="/espace-entreprise/messagerie"
            class="flex items-center gap-3 bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg no-underline hover:border-secondary transition-colors"
          >
            <span class="material-symbols-outlined text-secondary text-[28px]">forum</span>
            <span class="font-label-md text-label-md text-on-surface">Messagerie avec la pédagogie</span>
          </a>
        </section>

        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm overflow-hidden">
          <div class="px-6 py-5 border-b border-outline-variant flex justify-between items-center">
            <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
              <span class="material-symbols-outlined text-secondary">history</span>
              Dernières demandes
            </h2>
            <a
              routerLink="/espace-entreprise/nouvelle-demande"
              class="text-secondary font-label-md text-label-md hover:underline"
            >
              Voir tout
            </a>
          </div>
          <p *ngIf="demandes.length === 0" class="p-10 text-center text-on-surface-variant font-body-md text-body-md">
            Aucune demande déposée pour l'instant.
          </p>
          <ul *ngIf="demandes.length > 0" class="divide-y divide-outline-variant">
            <li *ngFor="let d of demandesRecentes" class="px-6 py-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="font-label-md text-label-md text-on-surface">{{ libellesType[d.type] }}</p>
                <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                  Déposée le {{ d.dateSoumission | date: 'd MMMM y' }}
                  <span *ngIf="d.intituleFormation"> · {{ d.intituleFormation }}</span>
                </p>
              </div>
              <span
                class="px-3 py-1.5 rounded-full font-label-sm text-label-sm"
                [ngClass]="{
                  'bg-surface-container text-on-surface-variant': d.statut === 'SOUMISE',
                  'bg-secondary-container text-on-secondary-container': d.statut === 'EN_COURS',
                  'bg-success/15 text-success': d.statut === 'REPONDUE',
                  'bg-outline-variant/40 text-on-surface-variant': d.statut === 'CLOTUREE'
                }"
              >
                {{ libellesStatut[d.statut] }}
              </span>
            </li>
          </ul>
        </div>
      </ng-container>
    </div>
  `,
})
export class EntrepriseAccueil implements OnInit {
  private entrepriseService = inject(EntrepriseService);

  chargement = true;
  demandes: DemandeEntreprise[] = [];
  catalogueCount = 0;
  raisonSociale = '';
  libellesStatut = LIBELLES_STATUT_DEMANDE_ENTREPRISE;
  libellesType = LIBELLES_TYPE_DEMANDE;

  ngOnInit(): void {
    this.entrepriseService.getMesDemandes().subscribe({
      next: (demandes) => {
        this.demandes = demandes || [];
        if (this.demandes.length > 0) {
          this.raisonSociale = this.demandes[0].raisonSociale;
        }
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
    this.entrepriseService.getCatalogue().subscribe({
      next: (formations) => (this.catalogueCount = (formations || []).length),
      error: () => (this.catalogueCount = 0),
    });
  }

  /** Demandes qui attendent encore une réaction de l'équipe pédagogique. */
  get enAttente(): number {
    return this.demandes.filter((d) => d.statut === 'SOUMISE' || d.statut === 'EN_COURS').length;
  }

  /** Fiches techniques transmises par la pédagogie et qui attendent une décision de l'entreprise. */
  get actionRequise(): number {
    return this.demandes.filter((d) => d.ficheStatut === 'TRANSMISE').length;
  }

  /** Les 5 demandes les plus récentes, pour un coup d'œil sans quitter l'accueil. */
  get demandesRecentes(): DemandeEntreprise[] {
    return this.demandes
      .slice()
      .sort((a, b) => new Date(b.dateSoumission).getTime() - new Date(a.dateSoumission).getTime())
      .slice(0, 5);
  }
}
