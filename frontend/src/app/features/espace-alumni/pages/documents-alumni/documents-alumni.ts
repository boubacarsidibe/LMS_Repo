import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { DocumentPartage } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/**
 * Partage de documents entre alumni (TDR §C), suite de l'espace alumni livré
 * sans ce volet faute d'écran naturel où l'accrocher (voir migration V12 et
 * LOG_BLOQUANT.md) — désormais possible une fois le forum alumni en place.
 *
 * <p>Un seul espace plat, ouvert à tous les alumni : voir le commentaire de la
 * migration V14 sur l'absence de stratification par promotion.
 */
@Component({
  selector: 'app-documents-alumni',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Documents partagés</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Documents mis à disposition par les alumni de l'INPG.
          </p>
        </div>
        <button
          type="button"
          (click)="ouvrirExplorateur()"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity"
        >
          Partager un document
        </button>
        <input
          #champFichier
          type="file"
          class="hidden"
          (change)="fichierChoisi($event)"
        />
      </header>

      <form
        *ngIf="fichierSelectionne"
        (ngSubmit)="envoyer()"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
      >
        <p class="font-body-sm text-body-sm text-on-surface-variant">
          Fichier choisi : <span class="font-label-md text-label-md text-on-surface">{{ fichierSelectionne.name }}</span>
        </p>
        <div>
          <label for="titre" class="font-label-md text-label-md text-on-surface">Titre</label>
          <input
            id="titre"
            [(ngModel)]="titre"
            name="titre"
            required
            placeholder="Intitulé du document"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <div>
          <label for="description" class="font-label-md text-label-md text-on-surface">Description (facultative)</label>
          <textarea
            id="description"
            [(ngModel)]="description"
            name="description"
            rows="2"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
        </div>
        <p *ngIf="messageErreur" class="font-body-sm text-body-sm text-error">{{ messageErreur }}</p>
        <div class="flex gap-3">
          <button
            type="submit"
            [disabled]="!titre.trim() || envoiEnCours"
            class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            {{ envoiEnCours ? 'Envoi…' : 'Partager' }}
          </button>
          <button
            type="button"
            (click)="annuler()"
            class="px-4 py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container transition-colors"
          >
            Annuler
          </button>
        </div>
      </form>

      <app-ui-spinner *ngIf="chargement" message="Chargement des documents…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && documents.length === 0"
        icone="folder_shared"
        titre="Aucun document partagé"
        description="Soyez le premier à partager un document utile aux autres alumni."
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && documents.length > 0" class="space-y-base">
        <li
          *ngFor="let d of documents"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg shadow-sm flex items-start gap-4"
        >
          <span class="material-symbols-outlined text-secondary text-[28px] mt-1">description</span>
          <div class="flex-1 min-w-0">
            <h2 class="font-label-md text-label-md text-on-surface">{{ d.titre }}</h2>
            <p *ngIf="d.description" class="font-body-sm text-body-sm text-on-surface-variant mt-1">
              {{ d.description }}
            </p>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
              {{ d.nomFichierOriginal }} · partagé par {{ d.auteur }} · {{ d.dateAjout | date: 'd MMM y' }}
            </p>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <button
              type="button"
              (click)="telecharger(d)"
              class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container hover:text-secondary transition-colors"
              aria-label="Télécharger"
              title="Télécharger"
            >
              <span class="material-symbols-outlined">download</span>
            </button>
            <button
              *ngIf="d.estAuteur"
              type="button"
              (click)="supprimer(d)"
              class="p-2 rounded-full text-on-surface-variant hover:bg-error-container hover:text-on-error-container transition-colors"
              aria-label="Supprimer"
              title="Supprimer"
            >
              <span class="material-symbols-outlined">delete</span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  `,
})
export class DocumentsAlumni implements OnInit {
  private alumniService = inject(AlumniService);
  private media = inject(MediaProtegeService);

  @ViewChild('champFichier') champFichier?: ElementRef<HTMLInputElement>;

  documents: DocumentPartage[] = [];
  chargement = true;

  fichierSelectionne: File | null = null;
  titre = '';
  description = '';
  envoiEnCours = false;
  messageErreur = '';

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    this.alumniService.getDocuments().subscribe({
      next: (documents) => {
        this.documents = documents;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  ouvrirExplorateur(): void {
    this.champFichier?.nativeElement.click();
  }

  fichierChoisi(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    this.fichierSelectionne = champ.files?.length ? champ.files[0] : null;
    this.messageErreur = '';
  }

  annuler(): void {
    this.fichierSelectionne = null;
    this.titre = '';
    this.description = '';
    this.messageErreur = '';
    if (this.champFichier) {
      this.champFichier.nativeElement.value = '';
    }
  }

  envoyer(): void {
    if (!this.fichierSelectionne || !this.titre.trim()) {
      return;
    }
    this.envoiEnCours = true;
    this.messageErreur = '';
    this.alumniService
      .ajouterDocument(this.titre.trim(), this.description.trim() || null, this.fichierSelectionne)
      .subscribe({
        next: (document) => {
          this.documents = [document, ...this.documents];
          this.envoiEnCours = false;
          this.annuler();
        },
        error: (erreur) => {
          this.envoiEnCours = false;
          this.messageErreur = erreur?.error?.message || "Le document n'a pas pu être partagé.";
        },
      });
  }

  telecharger(document: DocumentPartage): void {
    this.media.ouvrir(this.alumniService.urlFichierDocument(document.id));
  }

  supprimer(document: DocumentPartage): void {
    this.alumniService.supprimerDocument(document.id).subscribe({
      next: () => (this.documents = this.documents.filter((d) => d.id !== document.id)),
      error: () => undefined,
    });
  }
}
