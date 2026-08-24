import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  DemandeAccreditation,
  LIBELLES_STATUT_DEMANDE,
  LIBELLES_TYPE_DOCUMENT,
  StatutDemandeAccreditation,
} from '../../../accreditation/models/accreditation.models';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';

/**
 * Liste des demandes d'accréditation, avec décision (TDR §F : « espace réservé
 * aux évaluateurs afin qu'ils puissent consulter les informations soumises et
 * rendre leurs décisions »).
 */
@Component({
  selector: 'app-evaluateur-demandes',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Demandes d'accréditation</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Consultez les dossiers déposés par les écoles de formation et rendez votre décision.
          </p>
        </div>
        <select
          [(ngModel)]="filtreStatut"
          (ngModelChange)="charger()"
          class="p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
        >
          <option [ngValue]="undefined">Tous les statuts</option>
          <option *ngFor="let s of statuts" [ngValue]="s">{{ libellesStatut[s] }}</option>
        </select>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement des demandes…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && demandes.length === 0"
        icone="fact_check"
        titre="Aucune demande"
        description="Aucune demande d'accréditation ne correspond à ce filtre."
      ></app-ui-empty-state>

      <div class="grid lg:grid-cols-2 gap-base" *ngIf="!chargement && demandes.length > 0">
        <ul class="space-y-base">
          <li
            *ngFor="let d of demandes"
            (click)="selectionner(d)"
            class="cursor-pointer bg-surface-container-lowest rounded-xl border p-stack-lg shadow-sm transition-colors"
            [ngClass]="selection?.id === d.id ? 'border-secondary' : 'border-outline-variant hover:border-secondary'"
          >
            <div class="flex items-center justify-between gap-2">
              <h2 class="font-label-md text-label-md text-on-surface">{{ d.nomEcole }}</h2>
              <span
                class="px-2 py-0.5 rounded-full font-label-sm text-label-sm"
                [ngClass]="{
                  'bg-surface-container text-on-surface-variant': d.statut === 'SOUMISE',
                  'bg-secondary-container text-on-secondary-container': d.statut === 'EN_COURS_EVALUATION',
                  'bg-success/15 text-success': d.statut === 'ACCREDITE',
                  'bg-error-container text-on-error-container': d.statut === 'REJETE'
                }"
              >
                {{ libellesStatut[d.statut] }}
              </span>
            </div>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
              Déposée le {{ d.dateSoumission | date: 'd MMM y' }} · {{ d.pieces.length }} pièce(s)
            </p>
          </li>
        </ul>

        <div *ngIf="selection" class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3 h-fit">
          <h2 class="font-headline-sm text-headline-sm text-primary">{{ selection.nomEcole }}</h2>

          <ul class="space-y-2">
            <li *ngFor="let p of selection.pieces" class="flex items-center gap-2 font-body-sm text-body-sm text-on-surface">
              <span class="material-symbols-outlined text-[18px] text-on-surface-variant">draft</span>
              {{ libellesType[p.typeDocument] }} — {{ p.nomFichierOriginal }}
            </li>
          </ul>
          <p *ngIf="selection.pieces.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">
            Aucune pièce déposée.
          </p>

          <div *ngIf="selection.statut === 'SOUMISE'" class="pt-3 border-t border-outline-variant">
            <button
              type="button"
              (click)="prendreEnCharge()"
              class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity"
            >
              Prendre en charge
            </button>
          </div>

          <div *ngIf="selection.statut === 'EN_COURS_EVALUATION'" class="pt-3 border-t border-outline-variant space-y-3">
            <div>
              <label for="commentaire" class="font-label-md text-label-md text-on-surface">Commentaire / motif</label>
              <textarea
                id="commentaire"
                [(ngModel)]="commentaire"
                name="commentaire"
                rows="3"
                placeholder="Obligatoire pour un rejet, facultatif pour une accréditation"
                class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
              ></textarea>
            </div>
            <p *ngIf="erreurDecision" class="font-body-sm text-body-sm text-error">{{ erreurDecision }}</p>
            <div class="flex gap-3">
              <button
                type="button"
                (click)="accrediter()"
                [disabled]="decisionEnCours"
                class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
              >
                Accréditer
              </button>
              <button
                type="button"
                (click)="rejeter()"
                [disabled]="decisionEnCours"
                class="px-4 py-2 rounded-lg bg-error text-on-error font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
              >
                Rejeter
              </button>
            </div>
          </div>

          <p *ngIf="selection.statut === 'REJETE' && selection.motifRejet"
             class="p-3 rounded-lg bg-error-container text-on-error-container font-body-sm text-body-sm">
            Motif du rejet : {{ selection.motifRejet }}
          </p>
        </div>
      </div>
    </div>
  `,
})
export class EvaluateurDemandesComponent implements OnInit {
  private evaluateurService = inject(EvaluateurAccreditationService);

  demandes: DemandeAccreditation[] = [];
  selection: DemandeAccreditation | null = null;
  chargement = true;
  filtreStatut: StatutDemandeAccreditation | undefined;
  libellesStatut = LIBELLES_STATUT_DEMANDE;
  libellesType = LIBELLES_TYPE_DOCUMENT;
  statuts: StatutDemandeAccreditation[] = ['SOUMISE', 'EN_COURS_EVALUATION', 'ACCREDITE', 'REJETE'];

  commentaire = '';
  decisionEnCours = false;
  erreurDecision = '';

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement = true;
    this.evaluateurService.getDemandes(this.filtreStatut).subscribe({
      next: (demandes) => {
        this.demandes = demandes;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  selectionner(demande: DemandeAccreditation): void {
    this.selection = demande;
    this.commentaire = '';
    this.erreurDecision = '';
  }

  prendreEnCharge(): void {
    if (!this.selection) {
      return;
    }
    this.evaluateurService.prendreEnCharge(this.selection.id).subscribe({
      next: (demande) => this.remplacer(demande),
      error: (e) => (this.erreurDecision = e?.error?.message || "La prise en charge a échoué."),
    });
  }

  accrediter(): void {
    if (!this.selection) {
      return;
    }
    this.decisionEnCours = true;
    this.erreurDecision = '';
    this.evaluateurService.accrediter(this.selection.id, this.commentaire.trim() || undefined).subscribe({
      next: (demande) => {
        this.decisionEnCours = false;
        this.remplacer(demande);
      },
      error: (e) => {
        this.decisionEnCours = false;
        this.erreurDecision = e?.error?.message || "La décision n'a pas pu être enregistrée.";
      },
    });
  }

  rejeter(): void {
    if (!this.selection) {
      return;
    }
    if (!this.commentaire.trim()) {
      this.erreurDecision = 'Le motif du rejet est obligatoire.';
      return;
    }
    this.decisionEnCours = true;
    this.erreurDecision = '';
    this.evaluateurService.rejeter(this.selection.id, this.commentaire.trim()).subscribe({
      next: (demande) => {
        this.decisionEnCours = false;
        this.remplacer(demande);
      },
      error: (e) => {
        this.decisionEnCours = false;
        this.erreurDecision = e?.error?.message || "La décision n'a pas pu être enregistrée.";
      },
    });
  }

  private remplacer(demande: DemandeAccreditation): void {
    this.demandes = this.demandes.map((d) => (d.id === demande.id ? demande : d));
    this.selection = demande;
    this.commentaire = '';
  }
}
