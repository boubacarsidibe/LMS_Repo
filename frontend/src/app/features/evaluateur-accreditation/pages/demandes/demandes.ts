import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AvisValeur,
  DemandeAccreditationEvaluateur,
  LIBELLES_AVIS,
  LIBELLES_STATUT_DEMANDE,
  LIBELLES_TYPE_DOCUMENT,
  StatutDemandeAccreditation,
} from '../../../accreditation/models/accreditation.models';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import { Auth } from '../../../../core/auth/services/authService';
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

      <section *ngIf="!chargement" class="grid grid-cols-2 lg:grid-cols-4 gap-gutter">
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
          <div class="flex items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined">pending_actions</span>
            <span class="font-label-sm text-label-sm uppercase tracking-wider">À traiter</span>
          </div>
          <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">{{ compte('SOUMISE') }}</p>
        </div>
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
          <div class="flex items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined">fact_check</span>
            <span class="font-label-sm text-label-sm uppercase tracking-wider">En évaluation</span>
          </div>
          <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">{{ compte('EN_COURS_EVALUATION') }}</p>
        </div>
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
          <div class="flex items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-success">verified</span>
            <span class="font-label-sm text-label-sm uppercase tracking-wider">Accréditées</span>
          </div>
          <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">{{ compte('ACCREDITE') }}</p>
        </div>
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
          <div class="flex items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-error">cancel</span>
            <span class="font-label-sm text-label-sm uppercase tracking-wider">Rejetées</span>
          </div>
          <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">{{ compte('REJETE') }}</p>
        </div>
      </section>

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
              <span class="flex-1 min-w-0 truncate">{{ libellesType[p.typeDocument] }} — {{ p.nomFichierOriginal }}</span>
              <button
                type="button"
                (click)="consulterPiece(p.id)"
                title="Consulter la pièce"
                class="shrink-0 text-outline hover:text-secondary"
              >
                <span class="material-symbols-outlined text-[18px]">visibility</span>
              </button>
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

          <div *ngIf="selection.statut === 'EN_COURS_EVALUATION'" class="pt-3 border-t border-outline-variant space-y-4">
            <div>
              <h3 class="font-label-md text-label-md text-on-surface">Avis des évaluateurs</h3>
              <p *ngIf="selection.avis.length === 0" class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                Aucun avis déposé pour l'instant.
              </p>
              <ul *ngIf="selection.avis.length > 0" class="space-y-2 mt-2">
                <li
                  *ngFor="let a of selection.avis"
                  class="p-3 rounded-lg border font-body-sm text-body-sm"
                  [ngClass]="a.avis === 'FAVORABLE' ? 'bg-success/10 border-success/30' : 'bg-error-container/40 border-error/30'"
                >
                  <div class="flex items-center justify-between gap-2">
                    <span class="font-label-sm text-label-sm text-on-surface">
                      {{ a.evaluateurNom }}
                      <span *ngIf="estMonAvis(a)" class="text-on-surface-variant">(vous)</span>
                    </span>
                    <span class="font-label-sm text-label-sm" [ngClass]="a.avis === 'FAVORABLE' ? 'text-success' : 'text-error'">
                      {{ libellesAvis[a.avis] }}
                    </span>
                  </div>
                  <p *ngIf="a.commentaire" class="text-on-surface-variant mt-1">{{ a.commentaire }}</p>
                  <p class="text-on-surface-variant mt-1">{{ a.dateAvis | date: 'd MMM y, HH:mm' }}</p>
                </li>
              </ul>
            </div>

            <div class="p-3 rounded-lg bg-surface-container-low space-y-2">
              <label class="font-label-md text-label-md text-on-surface">
                {{ monAvis ? 'Mettre à jour mon avis' : 'Déposer mon avis' }}
              </label>
              <div class="flex gap-4">
                <label class="flex items-center gap-2 font-body-sm text-body-sm text-on-surface cursor-pointer">
                  <input type="radio" name="avisValeur" [(ngModel)]="avisSaisi" value="FAVORABLE" class="accent-secondary" />
                  Favorable
                </label>
                <label class="flex items-center gap-2 font-body-sm text-body-sm text-on-surface cursor-pointer">
                  <input type="radio" name="avisValeur" [(ngModel)]="avisSaisi" value="DEFAVORABLE" class="accent-secondary" />
                  Défavorable
                </label>
              </div>
              <textarea
                [(ngModel)]="avisCommentaire"
                name="avisCommentaire"
                rows="2"
                placeholder="Commentaire (facultatif)"
                class="w-full p-3 rounded-lg bg-surface-container-lowest border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
              ></textarea>
              <p *ngIf="erreurAvis" class="font-body-sm text-body-sm text-error">{{ erreurAvis }}</p>
              <button
                type="button"
                (click)="deposerAvis()"
                [disabled]="!avisSaisi || avisEnCours"
                class="px-4 py-2 rounded-lg border border-secondary text-secondary font-label-md text-label-md hover:bg-secondary-container/30 transition-colors disabled:opacity-50"
              >
                {{ avisEnCours ? 'Envoi…' : monAvis ? 'Mettre à jour mon avis' : 'Envoyer mon avis' }}
              </button>
            </div>

            <div class="pt-3 border-t border-outline-variant">
              <div>
                <label for="commentaire" class="font-label-md text-label-md text-on-surface">Commentaire / motif de la décision</label>
                <textarea
                  id="commentaire"
                  [(ngModel)]="commentaire"
                  name="commentaire"
                  rows="3"
                  placeholder="Obligatoire pour un rejet, facultatif pour une accréditation"
                  class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
                ></textarea>
              </div>
              <p *ngIf="selection.avis.length === 0" class="font-body-sm text-body-sm text-on-surface-variant mt-2">
                Au moins un avis d'évaluateur est requis avant de statuer.
              </p>
              <p *ngIf="erreurDecision" class="font-body-sm text-body-sm text-error mt-2">{{ erreurDecision }}</p>
              <div class="flex gap-3 mt-3">
                <button
                  type="button"
                  (click)="accrediter()"
                  [disabled]="decisionEnCours || selection.avis.length === 0"
                  class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
                >
                  Accréditer
                </button>
                <button
                  type="button"
                  (click)="rejeter()"
                  [disabled]="decisionEnCours || selection.avis.length === 0"
                  class="px-4 py-2 rounded-lg bg-error text-on-error font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
                >
                  Rejeter
                </button>
              </div>
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
  private mediaProtege = inject(MediaProtegeService);
  private auth = inject(Auth);

  demandes: DemandeAccreditationEvaluateur[] = [];
  /** Copie non filtrée, uniquement pour les indicateurs : `demandes` varie avec `filtreStatut`. */
  toutesDemandes: DemandeAccreditationEvaluateur[] = [];
  selection: DemandeAccreditationEvaluateur | null = null;
  chargement = true;
  filtreStatut: StatutDemandeAccreditation | undefined;
  libellesStatut = LIBELLES_STATUT_DEMANDE;
  libellesType = LIBELLES_TYPE_DOCUMENT;
  libellesAvis = LIBELLES_AVIS;
  statuts: StatutDemandeAccreditation[] = ['SOUMISE', 'EN_COURS_EVALUATION', 'ACCREDITE', 'REJETE'];

  commentaire = '';
  decisionEnCours = false;
  erreurDecision = '';

  avisSaisi: AvisValeur | null = null;
  avisCommentaire = '';
  avisEnCours = false;
  erreurAvis = '';

  ngOnInit(): void {
    this.charger();
    this.evaluateurService.getDemandes(undefined).subscribe({
      next: (demandes) => (this.toutesDemandes = demandes),
      error: () => (this.toutesDemandes = []),
    });
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

  /** Nombre de demandes dans ce statut, indépendamment du filtre appliqué à la liste. */
  compte(statut: StatutDemandeAccreditation): number {
    return this.toutesDemandes.filter((d) => d.statut === statut).length;
  }

  /** Une décision (accréditer/rejeter) change le statut d'une demande : la resynchroniser côté indicateurs aussi. */
  private synchroniserCompteurs(demande: DemandeAccreditationEvaluateur): void {
    this.toutesDemandes = this.toutesDemandes.map((d) => (d.id === demande.id ? demande : d));
  }

  consulterPiece(pieceId: number): void {
    if (!this.selection) {
      return;
    }
    this.mediaProtege.ouvrir(this.evaluateurService.urlFichierPiece(this.selection.id, pieceId));
  }

  selectionner(demande: DemandeAccreditationEvaluateur): void {
    this.selection = demande;
    this.commentaire = '';
    this.erreurDecision = '';
    this.reinitialiserFormulaireAvis();
  }

  /** L'avis déjà déposé par l'évaluateur connecté sur la demande sélectionnée, s'il existe. */
  get monAvis() {
    const monId = this.auth.currentUser()?.id;
    return this.selection?.avis.find((a) => a.evaluateurId === monId) ?? null;
  }

  estMonAvis(avis: { evaluateurId: number }): boolean {
    return avis.evaluateurId === this.auth.currentUser()?.id;
  }

  private reinitialiserFormulaireAvis(): void {
    const mien = this.monAvis;
    this.avisSaisi = mien?.avis ?? null;
    this.avisCommentaire = mien?.commentaire ?? '';
    this.erreurAvis = '';
  }

  deposerAvis(): void {
    if (!this.selection || !this.avisSaisi || this.avisEnCours) {
      return;
    }
    this.avisEnCours = true;
    this.erreurAvis = '';
    this.evaluateurService.deposerAvis(this.selection.id, this.avisSaisi, this.avisCommentaire.trim() || null).subscribe({
      next: (demande) => {
        this.avisEnCours = false;
        this.remplacer(demande);
        this.reinitialiserFormulaireAvis();
      },
      error: (e) => {
        this.avisEnCours = false;
        this.erreurAvis = e?.error?.message || "L'avis n'a pas pu être enregistré.";
      },
    });
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

  private remplacer(demande: DemandeAccreditationEvaluateur): void {
    this.demandes = this.demandes.map((d) => (d.id === demande.id ? demande : d));
    this.synchroniserCompteurs(demande);
    this.selection = demande;
    this.commentaire = '';
  }
}
