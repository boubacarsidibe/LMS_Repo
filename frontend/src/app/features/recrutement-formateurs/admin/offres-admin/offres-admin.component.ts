import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import {
  LIBELLES_TYPE_CONTRAT,
  OffreFormateur,
  StatutOffreFormateur,
} from '../../models/recrutement-formateur.models';
import { AdminRecrutementFormateurService } from '../../services/admin-recrutement-formateur.service';

/**
 * Espace admission — onglet Formateurs, sous-page « Offres » : cycle de vie des
 * fiches de poste (QMS-007 §1). Pages dédiées pour la création/édition
 * (patron responsable-admission/sessions).
 */
@Component({
  selector: 'app-offres-admin-formateurs',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
            Recrutement des formateurs — QMS-007
          </p>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Offres de recrutement</h1>
        </div>
        <a routerLink="/responsable-admission/recrutement-formateurs/offres/nouvelle"
           class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity no-underline">
          Nouvelle offre
        </a>
      </header>

      <nav class="flex gap-2 border-b border-outline-variant">
        <a routerLink="/responsable-admission/recrutement-formateurs/offres"
           routerLinkActive="border-secondary text-primary"
           class="px-4 py-2 border-b-2 border-transparent font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors no-underline">
          Offres
        </a>
        <a routerLink="/responsable-admission/recrutement-formateurs/candidatures"
           routerLinkActive="border-secondary text-primary"
           class="px-4 py-2 border-b-2 border-transparent font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors no-underline">
          Candidatures
        </a>
      </nav>

      <app-ui-spinner *ngIf="chargement" message="Chargement des offres…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && offres.length === 0"
        icone="work"
        titre="Aucune offre"
        description="Créez une première fiche de poste pour lancer un recrutement de formateur."
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && offres.length > 0" class="space-y-base">
        <li *ngFor="let o of offres"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg flex flex-wrap items-center justify-between gap-4">
          <div class="min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <h2 class="font-label-md text-label-md text-on-surface">{{ o.intitule }}</h2>
              <span class="px-2 py-0.5 rounded-full font-label-sm text-label-sm"
                [ngClass]="{
                  'bg-surface-container text-on-surface-variant': o.statut === 'BROUILLON',
                  'bg-success/15 text-success-strong': o.statut === 'PUBLIEE',
                  'bg-error-container text-on-error-container': o.statut === 'CLOTUREE'
                }">{{ libelleStatut(o.statut) }}</span>
            </div>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
              {{ libellesContrat[o.typeContrat] }}<span *ngIf="o.domaine"> · {{ o.domaine }}</span>
              · {{ o.nbPostes }} poste(s) · {{ o.nombreCandidatures }} candidature(s)
              <span *ngIf="o.dateLimiteCandidature"> · limite {{ o.dateLimiteCandidature | date: 'd MMM y' }}</span>
            </p>
          </div>
          <div class="flex items-center gap-1 shrink-0">
            <a [routerLink]="['/responsable-admission/recrutement-formateurs/offres', o.id]"
               title="Modifier" class="p-2 text-on-surface-variant hover:bg-surface-container rounded-full transition-colors">
              <span class="material-symbols-outlined text-[20px]">edit</span>
            </a>
            <button *ngIf="o.statut !== 'PUBLIEE'" type="button" (click)="publier(o)"
                    title="Publier" class="p-2 text-success-strong hover:bg-success/10 rounded-full transition-colors">
              <span class="material-symbols-outlined text-[20px]">publish</span>
            </button>
            <button *ngIf="o.statut === 'PUBLIEE'" type="button" (click)="cloturer(o)"
                    title="Clôturer" class="p-2 text-on-surface-variant hover:bg-surface-container rounded-full transition-colors">
              <span class="material-symbols-outlined text-[20px]">lock</span>
            </button>
            <button type="button" (click)="supprimer(o)" title="Supprimer"
                    class="p-2 text-on-surface-variant hover:bg-error-container hover:text-on-error-container rounded-full transition-colors">
              <span class="material-symbols-outlined text-[20px]">delete</span>
            </button>
          </div>
        </li>
      </ul>
      <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
    </div>
  `,
})
export class OffresAdminFormateursComponent implements OnInit {
  private service = inject(AdminRecrutementFormateurService);

  offres: OffreFormateur[] = [];
  chargement = true;
  erreur = '';
  libellesContrat = LIBELLES_TYPE_CONTRAT;

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement = true;
    this.service.getOffres().subscribe({
      next: (offres) => {
        this.offres = offres;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  libelleStatut(s: StatutOffreFormateur): string {
    return { BROUILLON: 'Brouillon', PUBLIEE: 'Publiée', CLOTUREE: 'Clôturée' }[s];
  }

  publier(o: OffreFormateur): void {
    this.erreur = '';
    this.service.publierOffre(o.id).subscribe({
      next: (maj) => (this.offres = this.offres.map((x) => (x.id === maj.id ? maj : x))),
      error: (e) => (this.erreur = e?.error?.message || 'La publication a échoué.'),
    });
  }

  cloturer(o: OffreFormateur): void {
    this.erreur = '';
    this.service.cloturerOffre(o.id).subscribe({
      next: (maj) => (this.offres = this.offres.map((x) => (x.id === maj.id ? maj : x))),
      error: (e) => (this.erreur = e?.error?.message || 'La clôture a échoué.'),
    });
  }

  supprimer(o: OffreFormateur): void {
    if (!confirm(`Supprimer l'offre « ${o.intitule} » ?`)) {
      return;
    }
    this.erreur = '';
    this.service.supprimerOffre(o.id).subscribe({
      next: () => (this.offres = this.offres.filter((x) => x.id !== o.id)),
      error: (e) => (this.erreur = e?.error?.message || 'La suppression a échoué (offre avec candidatures ?).'),
    });
  }
}
