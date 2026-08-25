import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import {
  DemandeAccreditation,
  LIBELLES_STATUT_DEMANDE,
  LIBELLES_TYPE_DOCUMENT,
  TypeDocumentAccreditation,
} from '../../models/accreditation.models';
import { AccreditationService } from '../../services/accreditation.service';

/**
 * Espace École de formation (TDR §F) : dépose une demande d'accréditation,
 * complète ses pièces, suit l'état de sa demande.
 *
 * <p>Une seule page : contrairement au recrutement (plusieurs sessions
 * possibles), une école n'a jamais qu'une seule demande active à la fois
 * (garde-fou en base, migration V15) — pas besoin d'un écran de liste séparé.
 */
@Component({
  selector: 'app-demande-accreditation',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Écoles de formation
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Demande d'accréditation</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Soumettez votre demande d'accréditation pour dispenser des formations dans le domaine du
          pétrole et du gaz, et suivez son évolution.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de votre dossier…"></app-ui-spinner>

      <!-- Aucune demande active : formulaire de dépôt -->
      <form
        *ngIf="!chargement && !demande"
        (ngSubmit)="soumettre()"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3"
      >
        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
          <span class="material-symbols-outlined text-secondary">school</span>
          Coordonnées de l'école
        </h2>
        <div>
          <label for="nomEcole" class="font-label-md text-label-md text-on-surface">Nom de l'école</label>
          <input
            id="nomEcole"
            [(ngModel)]="nomEcole"
            name="nomEcole"
            required
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <div>
          <label for="adresse" class="font-label-md text-label-md text-on-surface">Adresse</label>
          <input
            id="adresse"
            [(ngModel)]="adresse"
            name="adresse"
            required
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <div class="grid sm:grid-cols-2 gap-3">
          <div>
            <label for="telephone" class="font-label-md text-label-md text-on-surface">Téléphone</label>
            <input
              id="telephone"
              [(ngModel)]="telephone"
              name="telephone"
              required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
          <div>
            <label for="siteWeb" class="font-label-md text-label-md text-on-surface">Site web (facultatif)</label>
            <input
              id="siteWeb"
              [(ngModel)]="siteWeb"
              name="siteWeb"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
        </div>
        <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
        <button
          type="submit"
          [disabled]="!formulaireValide || envoiEnCours"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
        >
          {{ envoiEnCours ? 'Envoi…' : 'Soumettre la demande' }}
        </button>
      </form>

      <!-- Demande existante : suivi + pièces -->
      <ng-container *ngIf="!chargement && demande">
        <!-- Progression du dossier -->
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg" *ngIf="demande.statut !== 'REJETE'">
          <ol class="flex items-center w-full">
            <li *ngFor="let etape of etapes; let i = index" class="flex items-center" [class.flex-1]="i < etapes.length - 1">
              <div class="flex flex-col items-center gap-1 shrink-0">
                <div class="w-9 h-9 rounded-full flex items-center justify-center shrink-0"
                     [class.bg-secondary]="i <= etapeActuelle" [class.text-on-secondary]="i <= etapeActuelle"
                     [class.bg-surface-container]="i > etapeActuelle" [class.text-on-surface-variant]="i > etapeActuelle">
                  <span class="material-symbols-outlined text-[20px]">{{ i < etapeActuelle ? 'check' : etape.icone }}</span>
                </div>
                <span class="font-label-sm text-label-sm text-center max-w-[7rem]"
                      [class.text-primary]="i <= etapeActuelle" [class.text-on-surface-variant]="i > etapeActuelle">
                  {{ etape.libelle }}
                </span>
              </div>
              <div *ngIf="i < etapes.length - 1" class="flex-1 h-0.5 mx-2 mb-5"
                   [class.bg-secondary]="i < etapeActuelle" [class.bg-outline-variant]="i >= etapeActuelle"></div>
            </li>
          </ol>
        </div>

        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="font-headline-sm text-headline-sm text-primary">{{ demande.nomEcole }}</h2>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                Déposée le {{ demande.dateSoumission | date: 'd MMMM y' }}
              </p>
            </div>
            <span
              class="px-3 py-1.5 rounded-full font-label-sm text-label-sm"
              [ngClass]="{
                'bg-surface-container text-on-surface-variant': demande.statut === 'SOUMISE',
                'bg-secondary-container text-on-secondary-container': demande.statut === 'EN_COURS_EVALUATION',
                'bg-success/15 text-success': demande.statut === 'ACCREDITE',
                'bg-error-container text-on-error-container': demande.statut === 'REJETE'
              }"
            >
              {{ libellesStatut[demande.statut] }}
            </span>
          </div>

          <p *ngIf="demande.statut === 'REJETE' && demande.motifRejet"
             class="mt-4 p-3 rounded-lg bg-error-container text-on-error-container font-body-sm text-body-sm">
            Motif du rejet : {{ demande.motifRejet }}
          </p>
          <p *ngIf="demande.statut === 'ACCREDITE'"
             class="mt-4 p-3 rounded-lg bg-success/10 border border-success/30 text-success font-body-sm text-body-sm">
            Félicitations, votre école est accréditée par l'INPG.
          </p>
        </div>

        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3">
          <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-secondary">description</span>
            Pièces du dossier
          </h2>

          <ul class="space-y-base">
            <li
              *ngFor="let p of demande.pieces"
              class="flex items-center gap-3 p-3 rounded-lg bg-surface-container-low"
            >
              <span class="material-symbols-outlined text-on-surface-variant">draft</span>
              <div class="flex-1 min-w-0">
                <p class="font-label-md text-label-md text-on-surface">{{ libellesType[p.typeDocument] }}</p>
                <p class="font-body-sm text-body-sm text-on-surface-variant truncate">{{ p.nomFichierOriginal }}</p>
              </div>
            </li>
          </ul>
          <p *ngIf="demande.pieces.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">
            Aucune pièce déposée pour l'instant.
          </p>

          <form
            *ngIf="demande.statut === 'SOUMISE' || demande.statut === 'EN_COURS_EVALUATION'"
            (ngSubmit)="televerser()"
            class="pt-3 border-t border-outline-variant flex flex-wrap items-end gap-3"
          >
            <div>
              <label for="typeDocument" class="font-label-md text-label-md text-on-surface">Type de document</label>
              <select
                id="typeDocument"
                [(ngModel)]="typeDocumentChoisi"
                name="typeDocument"
                class="mt-1 p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
              >
                <option *ngFor="let type of typesDocument" [ngValue]="type">{{ libellesType[type] }}</option>
              </select>
            </div>
            <input type="file" (change)="fichierChoisi($event)" class="font-body-sm text-body-sm" />
            <button
              type="submit"
              [disabled]="!fichierSelectionne || televersementEnCours"
              class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
            >
              {{ televersementEnCours ? 'Envoi…' : 'Ajouter la pièce' }}
            </button>
          </form>
          <p *ngIf="erreurPiece" class="font-body-sm text-body-sm text-error">{{ erreurPiece }}</p>
        </div>
      </ng-container>
    </div>
  `,
})
export class DemandeAccreditationComponent implements OnInit {
  private accreditationService = inject(AccreditationService);

  chargement = true;
  demande: DemandeAccreditation | null = null;
  libellesStatut = LIBELLES_STATUT_DEMANDE;
  libellesType = LIBELLES_TYPE_DOCUMENT;
  typesDocument = Object.keys(LIBELLES_TYPE_DOCUMENT) as TypeDocumentAccreditation[];

  // Formulaire de dépôt
  nomEcole = '';
  adresse = '';
  telephone = '';
  siteWeb = '';
  envoiEnCours = false;
  erreur = '';

  // Téléversement de pièce
  typeDocumentChoisi: TypeDocumentAccreditation = 'CURRICULUM';
  fichierSelectionne: File | null = null;
  televersementEnCours = false;
  erreurPiece = '';

  /** Étapes du parcours d'accréditation, dans l'ordre du workflow (REJETE n'y figure pas : il interrompt le parcours). */
  readonly etapes = [
    { statut: 'SOUMISE', libelle: 'Déposée', icone: 'upload_file' },
    { statut: 'EN_COURS_EVALUATION', libelle: "En cours d'évaluation", icone: 'fact_check' },
    { statut: 'ACCREDITE', libelle: 'Accréditée', icone: 'verified' },
  ];

  get etapeActuelle(): number {
    if (!this.demande) return 0;
    const index = this.etapes.findIndex((e) => e.statut === this.demande!.statut);
    return index === -1 ? 0 : index;
  }

  ngOnInit(): void {
    this.accreditationService.getMesDemandes().subscribe({
      next: (demandes) => {
        this.demande = demandes.find((d) => d.statut === 'SOUMISE' || d.statut === 'EN_COURS_EVALUATION')
          || demandes[0]
          || null;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  get formulaireValide(): boolean {
    return !!this.nomEcole.trim() && !!this.adresse.trim() && !!this.telephone.trim();
  }

  soumettre(): void {
    if (!this.formulaireValide) {
      return;
    }
    this.envoiEnCours = true;
    this.erreur = '';
    this.accreditationService
      .soumettreDemande({
        nomEcole: this.nomEcole.trim(),
        adresse: this.adresse.trim(),
        telephone: this.telephone.trim(),
        siteWeb: this.siteWeb.trim() || null,
      })
      .subscribe({
        next: (demande) => {
          this.demande = demande;
          this.envoiEnCours = false;
        },
        error: (e) => {
          this.envoiEnCours = false;
          this.erreur = e?.error?.message || "La soumission de la demande a échoué.";
        },
      });
  }

  fichierChoisi(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    this.fichierSelectionne = champ.files?.length ? champ.files[0] : null;
  }

  televerser(): void {
    if (!this.demande || !this.fichierSelectionne) {
      return;
    }
    this.televersementEnCours = true;
    this.erreurPiece = '';
    this.accreditationService
      .televerserPiece(this.demande.id, this.fichierSelectionne, this.typeDocumentChoisi)
      .subscribe({
        next: (piece) => {
          this.demande!.pieces = [...this.demande!.pieces, piece];
          this.televersementEnCours = false;
          this.fichierSelectionne = null;
        },
        error: (e) => {
          this.televersementEnCours = false;
          this.erreurPiece = e?.error?.message || "L'ajout de la pièce a échoué.";
        },
      });
  }
}
