import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import {
  DemandeAccreditation,
  LIBELLES_NIVEAU_PROGRAMME,
  LIBELLES_STATUT_DEMANDE,
  LIBELLES_TYPE_DOCUMENT,
  LIBELLES_TYPE_STRUCTURE,
  NiveauProgramme,
  PieceAccreditation,
  TYPES_DOCUMENT_RECOMMANDES,
  TYPES_DOCUMENT_REQUIS,
  TypeDocumentAccreditation,
  TypeStructure,
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

        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2 pt-3">
          <span class="material-symbols-outlined text-secondary">workspace_premium</span>
          Programme visé
        </h2>
        <p class="font-body-sm text-body-sm text-on-surface-variant -mt-1">
          L'accréditation porte sur un programme de formation précis, sa cohorte et ses formateurs de
          spécialité — pas sur l'établissement dans son ensemble (Cadre de référence DRCQ §II).
        </p>
        <div>
          <label for="intituleProgramme" class="font-label-md text-label-md text-on-surface">Intitulé du programme</label>
          <input
            id="intituleProgramme"
            [(ngModel)]="intituleProgramme"
            name="intituleProgramme"
            required
            placeholder="Ex. Licence professionnelle Forage & Production"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <div class="grid sm:grid-cols-2 gap-3">
          <div>
            <label for="niveauProgramme" class="font-label-md text-label-md text-on-surface">Niveau</label>
            <select
              id="niveauProgramme"
              [(ngModel)]="niveauProgramme"
              name="niveauProgramme"
              required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            >
              <option [ngValue]="null" disabled>Sélectionner…</option>
              <option *ngFor="let n of niveaux" [ngValue]="n">{{ libellesNiveau[n] }}</option>
            </select>
          </div>
          <div>
            <label for="typeStructure" class="font-label-md text-label-md text-on-surface">Type de structure</label>
            <select
              id="typeStructure"
              [(ngModel)]="typeStructure"
              name="typeStructure"
              required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            >
              <option [ngValue]="null" disabled>Sélectionner…</option>
              <option *ngFor="let t of typesStructure" [ngValue]="t">{{ libellesTypeStructure[t] }}</option>
            </select>
          </div>
        </div>
        <div class="grid sm:grid-cols-2 gap-3">
          <div>
            <label for="effectifCohorte" class="font-label-md text-label-md text-on-surface">Effectif de la cohorte (facultatif)</label>
            <input
              id="effectifCohorte"
              type="number"
              min="1"
              [(ngModel)]="effectifCohorte"
              name="effectifCohorte"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
          <div>
            <label for="dateDebutCohorte" class="font-label-md text-label-md text-on-surface">Début de la cohorte (facultatif)</label>
            <input
              id="dateDebutCohorte"
              type="date"
              [(ngModel)]="dateDebutCohorte"
              name="dateDebutCohorte"
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
              <h2 class="font-headline-sm text-headline-sm text-primary">{{ demande.intituleProgramme || demande.nomEcole }}</h2>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                {{ demande.nomEcole }} · déposée le {{ demande.dateSoumission | date: 'd MMMM y' }}
              </p>
            </div>
            <span
              class="px-3 py-1.5 rounded-full font-label-sm text-label-sm"
              [ngClass]="{
                'bg-surface-container text-on-surface-variant': demande.statut === 'SOUMISE',
                'bg-secondary-container text-on-secondary-container': demande.statut === 'EN_COURS_EVALUATION',
                'bg-success/15 text-success': demande.statut === 'ACCREDITE',
                'bg-warning/15 text-warning': demande.statut === 'ACCREDITE_SOUS_RESERVE',
                'bg-error-container text-on-error-container': demande.statut === 'REJETE'
              }"
            >
              {{ libellesStatut[demande.statut] }}
            </span>
          </div>

          <!-- Programme visé -->
          <dl class="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-2 text-body-sm font-body-sm bg-surface-container-low rounded-lg p-3">
            <div>
              <dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Niveau</dt>
              <dd class="text-on-surface">{{ demande.niveauProgramme ? libellesNiveau[demande.niveauProgramme] : '—' }}</dd>
            </div>
            <div>
              <dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Type de structure</dt>
              <dd class="text-on-surface">{{ demande.typeStructure ? libellesTypeStructure[demande.typeStructure] : '—' }}</dd>
            </div>
            <div>
              <dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Effectif de la cohorte</dt>
              <dd class="text-on-surface">{{ demande.effectifCohorte ?? '—' }}</dd>
            </div>
            <div>
              <dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Début de la cohorte</dt>
              <dd class="text-on-surface">{{ demande.dateDebutCohorte ? (demande.dateDebutCohorte | date: 'd MMMM y') : '—' }}</dd>
            </div>
          </dl>

          <p *ngIf="demande.statut === 'REJETE' && demande.motifRejet"
             class="mt-4 p-3 rounded-lg bg-error-container text-on-error-container font-body-sm text-body-sm">
            Motif du rejet : {{ demande.motifRejet }}
          </p>
          <div *ngIf="demande.statut === 'ACCREDITE'"
               class="mt-4 p-3 rounded-lg bg-success/10 border border-success/30 font-body-sm text-body-sm space-y-1">
            <p class="text-success">Félicitations, ce programme est accrédité par l'INPG.</p>
            <p *ngIf="demande.dateExpiration" class="text-on-surface-variant">
              Accréditation valable jusqu'au {{ demande.dateExpiration | date: 'd MMMM y' }} (3 ans).
            </p>
            <p *ngIf="demande.commentaireEvaluation" class="text-on-surface-variant">
              Synthèse de l'évaluateur : {{ demande.commentaireEvaluation }}
            </p>
          </div>
          <div *ngIf="demande.statut === 'ACCREDITE_SOUS_RESERVE'"
               class="mt-4 p-3 rounded-lg bg-warning/10 border border-warning/40 font-body-sm text-body-sm space-y-1">
            <p class="text-warning">Ce programme bénéficie d'une accréditation conditionnelle (sous réserve).</p>
            <p *ngIf="demande.dateExpiration" class="text-on-surface-variant">
              Accréditation valable jusqu'au {{ demande.dateExpiration | date: 'd MMMM y' }} (3 ans).
            </p>
            <p *ngIf="demande.planActions" class="text-on-surface whitespace-pre-line">
              <span class="font-label-sm text-label-sm text-on-surface-variant block">Plan d'actions à mettre en œuvre</span>
              {{ demande.planActions }}
            </p>
          </div>
        </div>

        <!-- Pièces du dossier : checklist par type de document attendu -->
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
              <span class="material-symbols-outlined text-secondary">description</span>
              Pièces du dossier
            </h2>
            <span
              class="px-3 py-1.5 rounded-full font-label-sm text-label-sm"
              [ngClass]="dossierComplet
                ? 'bg-success/15 text-success'
                : 'bg-error-container/40 text-on-error-container'"
            >
              {{ dossierComplet
                ? 'Dossier complet — ' + typesRequis.length + '/' + typesRequis.length + ' pièces requises'
                : typesRequisManquants.length + ' pièce(s) requise(s) manquante(s)' }}
            </span>
          </div>
          <p *ngIf="!dossierComplet" class="font-body-sm text-body-sm text-on-surface-variant">
            À fournir : {{ libellesManquants }}.
          </p>

          <input #champFichier type="file" class="hidden" (change)="fichierChoisi($event)" />

          <!-- Une ligne par type requis -->
          <ul class="divide-y divide-outline-variant/50 border border-outline-variant rounded-lg overflow-hidden">
            <li *ngFor="let type of typesRequis"
                class="flex items-center gap-3 p-3 border-l-4"
                [class.border-l-success]="pieceDeType(type)"
                [class.border-l-transparent]="!pieceDeType(type)">
              <span class="material-symbols-outlined shrink-0"
                    [class.text-success]="pieceDeType(type)"
                    [class.text-outline]="!pieceDeType(type)">
                {{ pieceDeType(type) ? 'check_circle' : 'radio_button_unchecked' }}
              </span>
              <div class="flex-1 min-w-0">
                <p class="font-label-md text-label-md text-on-surface">{{ libellesType[type] }}</p>
                <p *ngIf="pieceDeType(type) as p" class="font-body-sm text-body-sm text-on-surface-variant truncate">
                  {{ p.nomFichierOriginal }}
                </p>
                <p *ngIf="!pieceDeType(type)" class="font-body-sm text-body-sm text-on-surface-variant">Non fournie</p>
              </div>
              <div class="flex items-center gap-1 shrink-0">
                <button *ngIf="pieceDeType(type) as p" type="button" (click)="consulterPiece(p.id)"
                        title="Consulter" class="p-2 text-outline hover:text-secondary rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">visibility</span>
                </button>
                <button *ngIf="modifiable" type="button" (click)="ajouterPiece(type)"
                        [disabled]="televersementEnCours"
                        [title]="pieceDeType(type) ? 'Remplacer' : 'Ajouter'"
                        class="p-2 text-outline hover:text-secondary rounded-full transition-colors disabled:opacity-40">
                  <span class="material-symbols-outlined text-[20px]">{{ pieceDeType(type) ? 'sync' : 'upload' }}</span>
                </button>
                <button *ngIf="modifiable && pieceDeType(type) as p" type="button" (click)="supprimerPiece(p.id)"
                        title="Supprimer"
                        class="p-2 text-outline hover:text-error rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">delete</span>
                </button>
              </div>
            </li>
          </ul>

          <!-- Pièces recommandées : attendues à la visite du comité (Cadre §IV.4), non bloquantes -->
          <div class="space-y-2">
            <p class="font-label-md text-label-md text-on-surface">Pièces recommandées (attendues à la visite du comité)</p>
            <ul class="divide-y divide-outline-variant/50 border border-outline-variant rounded-lg overflow-hidden">
              <li *ngFor="let type of typesRecommandes"
                  class="flex items-center gap-3 p-3 border-l-4"
                  [class.border-l-success]="pieceDeType(type)"
                  [class.border-l-transparent]="!pieceDeType(type)">
                <span class="material-symbols-outlined shrink-0"
                      [class.text-success]="pieceDeType(type)"
                      [class.text-outline]="!pieceDeType(type)">
                  {{ pieceDeType(type) ? 'check_circle' : 'radio_button_unchecked' }}
                </span>
                <div class="flex-1 min-w-0">
                  <p class="font-label-md text-label-md text-on-surface">{{ libellesType[type] }}</p>
                  <p *ngIf="pieceDeType(type) as p" class="font-body-sm text-body-sm text-on-surface-variant truncate">
                    {{ p.nomFichierOriginal }}
                  </p>
                  <p *ngIf="!pieceDeType(type)" class="font-body-sm text-body-sm text-on-surface-variant">Non fournie</p>
                </div>
                <div class="flex items-center gap-1 shrink-0">
                  <button *ngIf="pieceDeType(type) as p" type="button" (click)="consulterPiece(p.id)"
                          title="Consulter" class="p-2 text-outline hover:text-secondary rounded-full transition-colors">
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                  </button>
                  <button *ngIf="modifiable" type="button" (click)="ajouterPiece(type)"
                          [disabled]="televersementEnCours"
                          [title]="pieceDeType(type) ? 'Remplacer' : 'Ajouter'"
                          class="p-2 text-outline hover:text-secondary rounded-full transition-colors disabled:opacity-40">
                    <span class="material-symbols-outlined text-[20px]">{{ pieceDeType(type) ? 'sync' : 'upload' }}</span>
                  </button>
                  <button *ngIf="modifiable && pieceDeType(type) as p" type="button" (click)="supprimerPiece(p.id)"
                          title="Supprimer"
                          class="p-2 text-outline hover:text-error rounded-full transition-colors">
                    <span class="material-symbols-outlined text-[20px]">delete</span>
                  </button>
                </div>
              </li>
            </ul>
          </div>

          <!-- Pièces « Autre », facultatives et multiples -->
          <div class="space-y-2">
            <div class="flex items-center justify-between gap-3">
              <p class="font-label-md text-label-md text-on-surface">Autres documents (facultatif)</p>
              <button *ngIf="modifiable" type="button" (click)="ajouterPiece('AUTRE')"
                      [disabled]="televersementEnCours"
                      class="flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant font-label-sm text-label-sm text-on-surface-variant hover:border-secondary hover:text-secondary transition-colors disabled:opacity-40">
                <span class="material-symbols-outlined text-[18px]">add</span>Ajouter
              </button>
            </div>
            <ul *ngIf="piecesAutres.length > 0" class="space-y-2">
              <li *ngFor="let p of piecesAutres"
                  class="flex items-center gap-3 p-3 rounded-lg bg-surface-container-low">
                <span class="material-symbols-outlined text-on-surface-variant shrink-0">draft</span>
                <span class="flex-1 min-w-0 truncate font-body-sm text-body-sm text-on-surface">{{ p.nomFichierOriginal }}</span>
                <button type="button" (click)="consulterPiece(p.id)" title="Consulter"
                        class="p-2 text-outline hover:text-secondary rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">visibility</span>
                </button>
                <button *ngIf="modifiable" type="button" (click)="supprimerPiece(p.id)" title="Supprimer"
                        class="p-2 text-outline hover:text-error rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">delete</span>
                </button>
              </li>
            </ul>
            <p *ngIf="piecesAutres.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">Aucun.</p>
          </div>

          <p *ngIf="televersementEnCours" class="font-body-sm text-body-sm text-on-surface-variant">Envoi du fichier…</p>
          <p *ngIf="erreurPiece" class="font-body-sm text-body-sm text-error">{{ erreurPiece }}</p>
        </div>
      </ng-container>
    </div>
  `,
})
export class DemandeAccreditationComponent implements OnInit {
  private accreditationService = inject(AccreditationService);
  private mediaProtege = inject(MediaProtegeService);

  @ViewChild('champFichier') champFichier?: ElementRef<HTMLInputElement>;

  chargement = true;
  demande: DemandeAccreditation | null = null;
  libellesStatut = LIBELLES_STATUT_DEMANDE;
  libellesType = LIBELLES_TYPE_DOCUMENT;
  libellesNiveau = LIBELLES_NIVEAU_PROGRAMME;
  libellesTypeStructure = LIBELLES_TYPE_STRUCTURE;
  readonly typesRequis = TYPES_DOCUMENT_REQUIS;
  readonly typesRecommandes = TYPES_DOCUMENT_RECOMMANDES;
  readonly niveaux = Object.keys(LIBELLES_NIVEAU_PROGRAMME) as NiveauProgramme[];
  readonly typesStructure = Object.keys(LIBELLES_TYPE_STRUCTURE) as TypeStructure[];

  // Formulaire de dépôt
  nomEcole = '';
  adresse = '';
  telephone = '';
  siteWeb = '';
  intituleProgramme = '';
  niveauProgramme: NiveauProgramme | null = null;
  typeStructure: TypeStructure | null = null;
  effectifCohorte: number | null = null;
  dateDebutCohorte = '';
  envoiEnCours = false;
  erreur = '';

  // Téléversement de pièce (le type est fixé par la ligne cliquée, l'input s'ouvre aussitôt)
  private typeEnCours: TypeDocumentAccreditation = 'CURRICULUM';
  televersementEnCours = false;
  erreurPiece = '';

  /** La demande accepte encore des pièces tant qu'elle n'est pas tranchée. */
  get modifiable(): boolean {
    return this.demande?.statut === 'SOUMISE' || this.demande?.statut === 'EN_COURS_EVALUATION';
  }

  /** Première pièce déposée pour ce type, ou undefined. */
  pieceDeType(type: TypeDocumentAccreditation): PieceAccreditation | undefined {
    return this.demande?.pieces.find((p) => p.typeDocument === type);
  }

  get piecesAutres(): PieceAccreditation[] {
    return this.demande?.pieces.filter((p) => p.typeDocument === 'AUTRE') ?? [];
  }

  get typesRequisManquants(): TypeDocumentAccreditation[] {
    return this.typesRequis.filter((t) => !this.pieceDeType(t));
  }

  get dossierComplet(): boolean {
    return this.typesRequisManquants.length === 0;
  }

  get libellesManquants(): string {
    return this.typesRequisManquants.map((t) => this.libellesType[t]).join(', ');
  }

  /** Étapes du parcours d'accréditation, dans l'ordre du workflow (REJETE n'y figure pas : il interrompt le parcours). */
  readonly etapes = [
    { statut: 'SOUMISE', libelle: 'Déposée', icone: 'upload_file' },
    { statut: 'EN_COURS_EVALUATION', libelle: "En cours d'évaluation", icone: 'fact_check' },
    { statut: 'ACCREDITE', libelle: 'Accréditée', icone: 'verified' },
  ];

  get etapeActuelle(): number {
    if (!this.demande) return 0;
    // Une accréditation conditionnelle atteint la dernière étape, comme une accréditation pleine.
    const statut = this.demande.statut === 'ACCREDITE_SOUS_RESERVE' ? 'ACCREDITE' : this.demande.statut;
    const index = this.etapes.findIndex((e) => e.statut === statut);
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
    return (
      !!this.nomEcole.trim() &&
      !!this.adresse.trim() &&
      !!this.telephone.trim() &&
      !!this.intituleProgramme.trim() &&
      !!this.niveauProgramme &&
      !!this.typeStructure
    );
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
        intituleProgramme: this.intituleProgramme.trim(),
        niveauProgramme: this.niveauProgramme!,
        typeStructure: this.typeStructure!,
        effectifCohorte: this.effectifCohorte ? Number(this.effectifCohorte) : null,
        dateDebutCohorte: this.dateDebutCohorte || null,
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

  consulterPiece(pieceId: number): void {
    if (!this.demande) {
      return;
    }
    this.mediaProtege.ouvrir(this.accreditationService.urlFichierPiece(this.demande.id, pieceId));
  }

  /** Ouvre le sélecteur de fichier pour un type donné ; l'envoi part dès qu'un fichier est choisi. */
  ajouterPiece(type: TypeDocumentAccreditation): void {
    this.typeEnCours = type;
    this.erreurPiece = '';
    this.champFichier?.nativeElement.click();
  }

  fichierChoisi(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.length ? champ.files[0] : null;
    champ.value = ''; // permet de re-choisir le même fichier plus tard
    if (fichier && this.demande) {
      this.televerser(fichier, this.typeEnCours);
    }
  }

  private televerser(fichier: File, type: TypeDocumentAccreditation): void {
    if (!this.demande) {
      return;
    }
    const ancienne = this.pieceDeType(type); // pour un type requis : remplacement = retrait de l'ancienne après succès
    this.televersementEnCours = true;
    this.erreurPiece = '';
    this.accreditationService.televerserPiece(this.demande.id, fichier, type).subscribe({
      next: (piece) => {
        this.demande!.pieces = [...this.demande!.pieces, piece];
        if (ancienne && type !== 'AUTRE') {
          this.accreditationService.supprimerPiece(this.demande!.id, ancienne.id).subscribe({
            next: () => (this.demande!.pieces = this.demande!.pieces.filter((p) => p.id !== ancienne.id)),
            error: () => undefined,
          });
        }
        this.televersementEnCours = false;
      },
      error: (e) => {
        this.televersementEnCours = false;
        this.erreurPiece = e?.error?.message || "L'ajout de la pièce a échoué.";
      },
    });
  }

  supprimerPiece(pieceId: number): void {
    if (!this.demande || !confirm('Retirer cette pièce du dossier ?')) {
      return;
    }
    this.erreurPiece = '';
    this.accreditationService.supprimerPiece(this.demande.id, pieceId).subscribe({
      next: () => (this.demande!.pieces = this.demande!.pieces.filter((p) => p.id !== pieceId)),
      error: (e) => (this.erreurPiece = e?.error?.message || 'La suppression a échoué.'),
    });
  }
}
