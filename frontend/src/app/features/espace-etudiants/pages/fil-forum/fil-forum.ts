import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { ForumMessage, ForumSujet } from '../../models/scolarite.models';
import { CommunicationService, Mentionnable } from '../../services/communication.service';

/** Fil de discussion d'un sujet du forum. */
@Component({
  selector: 'app-fil-forum',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-4xl mx-auto">
      <a
        routerLink="/espace-etudiant/forum"
        class="inline-flex items-center gap-1 font-label-md text-label-md text-on-surface-variant hover:text-secondary transition-colors"
      >
        <span class="material-symbols-outlined text-[18px]">arrow_back</span> Forum
      </a>

      <app-ui-spinner *ngIf="chargement" message="Chargement de la discussion…"></app-ui-spinner>
      <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>

      <ng-container *ngIf="!chargement && sujet">
        <header class="border-b border-outline-variant pb-4">
          <h1 class="font-display text-display text-on-surface leading-tight">{{ sujet.titre }}</h1>
          <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
            {{ sujet.coursTitre || 'Salon général' }} · ouvert par {{ sujet.auteur }} ·
            {{ sujet.dateCreation | date: 'd MMMM y' }}
          </p>
        </header>

        <ul class="space-y-stack-md">
          <li
            *ngFor="let m of messages"
            class="rounded-xl border p-stack-lg"
            [ngClass]="
              m.estAuteur
                ? 'bg-secondary-container/30 border-secondary-container ml-0 sm:ml-12'
                : 'bg-surface-container-lowest border-outline-variant mr-0 sm:mr-12'
            "
          >
            <div class="flex items-center gap-2">
              <div
                class="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center shrink-0"
              >
                <span class="text-on-primary-container font-label-sm text-label-sm">{{ initiales(m.auteur) }}</span>
              </div>
              <span class="font-label-md text-label-md text-on-surface">{{ m.auteur }}</span>
              <span class="font-body-sm text-body-sm text-on-surface-variant">
                {{ m.dateCreation | date: 'd MMM, HH:mm' }}
              </span>
              <button
                *ngIf="m.estAuteur"
                type="button"
                (click)="supprimer(m)"
                class="ml-auto p-1 rounded text-on-surface-variant hover:text-error transition-colors"
                aria-label="Supprimer mon message"
                title="Supprimer mon message"
              >
                <span class="material-symbols-outlined text-[18px]">delete</span>
              </button>
            </div>
            <p class="font-body-md text-body-md text-on-surface mt-2 whitespace-pre-line">
              {{ m.contenu }}
            </p>
            <button
              *ngIf="m.pieceJointeNom"
              type="button"
              (click)="telechargerPieceJointe(m)"
              [disabled]="telechargementEnCoursId === m.id"
              class="mt-3 flex items-center gap-2 px-3 py-2 bg-surface-container rounded-lg border border-outline-variant/50 hover:bg-surface-container-high transition-colors text-left disabled:opacity-60"
            >
              <span class="material-symbols-outlined text-[20px] text-secondary">attach_file</span>
              <span class="font-label-sm text-label-sm text-on-surface truncate max-w-[220px]">{{ m.pieceJointeNom }}</span>
              <span class="material-symbols-outlined text-[18px] text-outline ml-1">{{ telechargementEnCoursId === m.id ? 'hourglass_empty' : 'download' }}</span>
            </button>
          </li>
        </ul>

        <p
          *ngIf="messages.length === 0"
          class="font-body-md text-body-md text-on-surface-variant text-center py-6"
        >
          Aucun message pour l’instant. Lancez la discussion.
        </p>

        <form
          *ngIf="!sujet.clos"
          (ngSubmit)="repondre()"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
        >
          <label for="reponse" class="font-label-md text-label-md text-on-surface">Répondre</label>
          <p class="font-body-sm text-body-sm text-on-surface-variant">
            Tapez <strong>&#64;</strong> pour citer un camarade ou un formateur du cours : il en
            sera prévenu.
          </p>
          <div class="relative">
            <textarea
              id="reponse"
              [(ngModel)]="reponse"
              name="reponse"
              rows="3"
              (input)="surSaisie($event)"
              (keydown.escape)="fermerSuggestions()"
              placeholder="Votre réponse…"
              class="w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
            ></textarea>

            <ul
              *ngIf="suggestions.length > 0"
              class="absolute z-10 left-0 right-0 mt-1 max-h-56 overflow-y-auto bg-surface-container-lowest border border-outline-variant rounded-lg shadow-sm"
            >
              <li *ngFor="let personne of suggestions">
                <button
                  type="button"
                  (click)="choisir(personne)"
                  class="w-full text-left px-4 py-2 hover:bg-surface-container transition-colors border-0 bg-transparent cursor-pointer"
                >
                  <span class="font-label-md text-label-md text-on-surface">{{ personne.nom }}</span>
                  <span class="font-body-sm text-body-sm text-on-surface-variant"> · {{ personne.role }}</span>
                </button>
              </li>
            </ul>
          </div>

          <p *ngIf="cites.length > 0" class="font-body-sm text-body-sm text-on-surface-variant">
            Seront prévenus : {{ nomsCites }}.
          </p>
          <div *ngIf="fichierSelectionne" class="flex items-center gap-2 px-3 py-2 bg-secondary-container/20 rounded-lg">
            <span class="material-symbols-outlined text-[18px] text-secondary">attach_file</span>
            <span class="font-label-sm text-label-sm text-on-surface truncate max-w-xs">{{ fichierSelectionne.name }}</span>
            <button type="button" (click)="retirerFichier()" class="ml-auto p-1 text-outline hover:text-error transition-colors" title="Retirer le fichier" aria-label="Retirer le fichier">
              <span class="material-symbols-outlined text-[16px]">close</span>
            </button>
          </div>
          <p *ngIf="erreurReponse" class="font-body-sm text-body-sm text-error">
            {{ erreurReponse }}
          </p>
          <div class="flex items-center justify-between gap-3">
            <input #fichierInput (change)="onFichierSelectionne($event)" class="hidden" type="file">
            <button type="button" (click)="fichierInput.click()" class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-secondary hover:bg-secondary-container/20 transition-colors font-label-md text-label-md" title="Joindre un fichier">
              <span class="material-symbols-outlined text-[20px]">attach_file</span>
              <span>Joindre un fichier</span>
            </button>
            <button
              type="submit"
              [disabled]="!reponse.trim() || envoiEnCours"
              class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
            >
              {{ envoiEnCours ? 'Envoi…' : 'Envoyer' }}
            </button>
          </div>
        </form>

        <p
          *ngIf="sujet.clos"
          class="font-body-md text-body-md text-on-surface-variant text-center py-4 bg-surface-container rounded-xl"
        >
          Ce sujet est clos : il n’accepte plus de réponse.
        </p>
      </ng-container>
    </div>
  `,
})
export class FilForum implements OnInit {
  sujet: ForumSujet | null = null;
  messages: ForumMessage[] = [];
  chargement = true;
  erreur = '';

  reponse = '';
  envoiEnCours = false;
  erreurReponse = '';
  fichierSelectionne: File | null = null;
  telechargementEnCoursId: number | null = null;

  /** Personnes citables dans ce fil, chargées une fois à l'ouverture. */
  citables: Mentionnable[] = [];
  suggestions: Mentionnable[] = [];
  /** Personnes effectivement citées, dans l'ordre où elles ont été choisies. */
  cites: Mentionnable[] = [];

  private sujetId = 0;

  constructor(
    private route: ActivatedRoute,
    private communication: CommunicationService
  ) {}

  ngOnInit(): void {
    this.sujetId = Number(this.route.snapshot.paramMap.get('id'));
    this.communication.getFil(this.sujetId).subscribe({
      next: (fil) => {
        this.sujet = fil.sujet;
        this.messages = fil.messages;
        this.chargement = false;
        // Silencieux en cas d'echec : sans la liste, la citation ne marche pas, mais
        // repondre reste possible — mieux vaut une fonction en moins qu'un ecran bloque.
        this.communication.citables(this.sujetId).subscribe({
          next: (personnes) => (this.citables = personnes || []),
          error: () => (this.citables = []),
        });
      },
      error: (e) => {
        this.erreur = e?.error?.message || 'Cette discussion est introuvable ou ne vous est pas accessible.';
        this.chargement = false;
      },
    });
  }

  get nomsCites(): string {
    return this.cites.map((c) => c.nom).join(', ');
  }

  /**
   * Propose des personnes dès qu'un « @ » est en cours de frappe.
   *
   * <p>La recherche part du dernier « @ » saisi, et s'arrête à la première espace
   * double ou au retour à la ligne : un nom contient une espace, on ne peut donc pas
   * couper au premier blanc sans rendre « @Fatou Ndiaye » introuvable.
   */
  surSaisie(evenement: Event): void {
    const zone = evenement.target as HTMLTextAreaElement;
    const avantCurseur = this.reponse.slice(0, zone.selectionStart ?? this.reponse.length);
    const dernierArobase = avantCurseur.lastIndexOf('@');

    if (dernierArobase === -1) {
      this.fermerSuggestions();
      return;
    }
    const fragment = avantCurseur.slice(dernierArobase + 1);
    const passeALaLigne = fragment.indexOf(String.fromCharCode(10)) !== -1;
    if (passeALaLigne || fragment.includes('  ') || fragment.length > 30) {
      this.fermerSuggestions();
      return;
    }
    const recherche = fragment.trim().toLowerCase();
    this.suggestions = this.citables
      .filter((personne) => !this.cites.some((c) => c.id === personne.id))
      .filter((personne) => personne.nom.toLowerCase().includes(recherche))
      .slice(0, 6);
  }

  /** Remplace le fragment en cours de frappe par le nom complet retenu. */
  choisir(personne: Mentionnable): void {
    const dernierArobase = this.reponse.lastIndexOf('@');
    if (dernierArobase !== -1) {
      this.reponse = this.reponse.slice(0, dernierArobase) + '@' + personne.nom + ' ';
    }
    if (!this.cites.some((c) => c.id === personne.id)) {
      this.cites = [...this.cites, personne];
    }
    this.fermerSuggestions();
  }

  fermerSuggestions(): void {
    this.suggestions = [];
  }

  initiales(nom: string): string {
    return nom
      .split(' ')
      .filter((p) => p.length > 0)
      .slice(0, 2)
      .map((p) => p.charAt(0).toUpperCase())
      .join('');
  }

  repondre(): void {
    if (!this.reponse.trim()) {
      return;
    }
    this.envoiEnCours = true;
    this.erreurReponse = '';
    // Seules les personnes encore nommees dans le texte sont citees : effacer un
    // « @Nom » apres l'avoir choisi ne doit pas laisser une notification partir.
    const citesRetenus = this.cites.filter((c) => this.reponse.includes('@' + c.nom));
    this.communication
      .repondre(this.sujetId, this.reponse.trim(), citesRetenus.map((c) => c.id), this.fichierSelectionne)
      .subscribe({
      next: (message) => {
        this.messages = [...this.messages, message];
        this.reponse = '';
        this.cites = [];
        this.fichierSelectionne = null;
        this.fermerSuggestions();
        this.envoiEnCours = false;
      },
      error: (e) => {
        this.envoiEnCours = false;
        this.erreurReponse = e?.error?.message || "L'envoi de votre réponse a échoué.";
      },
    });
  }

  supprimer(message: ForumMessage): void {
    this.communication.supprimerMessage(message.id).subscribe({
      next: () => (this.messages = this.messages.filter((m) => m.id !== message.id)),
      error: () => undefined,
    });
  }

  onFichierSelectionne(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fichierSelectionne = input.files?.[0] ?? null;
  }

  retirerFichier(): void {
    this.fichierSelectionne = null;
  }

  telechargerPieceJointe(message: ForumMessage): void {
    if (!message.pieceJointeNom) return;
    this.telechargementEnCoursId = message.id;
    this.communication.telechargerPieceJointe(message.id).subscribe({
      next: (blob) => {
        this.telechargementEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = message.pieceJointeNom!;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementEnCoursId = null;
      },
    });
  }
}
