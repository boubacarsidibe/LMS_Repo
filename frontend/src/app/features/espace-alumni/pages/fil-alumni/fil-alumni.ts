import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { ForumMessage, ForumSujet, Mentionnable } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/** Fil de discussion d'un sujet du forum alumni. */
@Component({
  selector: 'app-fil-alumni',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-4xl mx-auto">
      <a
        routerLink="/espace-alumni/forum"
        class="inline-flex items-center gap-1 font-label-md text-label-md text-on-surface-variant hover:text-secondary transition-colors"
      >
        <span class="material-symbols-outlined text-[18px]">arrow_back</span> Forum Alumni
      </a>

      <app-ui-spinner *ngIf="chargement" message="Chargement de la discussion…"></app-ui-spinner>
      <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>

      <ng-container *ngIf="!chargement && sujet">
        <header class="border-b border-outline-variant pb-4">
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">{{ sujet.titre }}</h1>
          <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
            Ouvert par {{ sujet.auteur }} · {{ sujet.dateCreation | date: 'd MMMM y' }}
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
          </li>
        </ul>

        <p
          *ngIf="messages.length === 0"
          class="font-body-md text-body-md text-on-surface-variant text-center py-6"
        >
          Aucun message pour l'instant. Lancez la discussion.
        </p>

        <form
          (ngSubmit)="repondre()"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
        >
          <label for="reponse" class="font-label-md text-label-md text-on-surface">Répondre</label>
          <p class="font-body-sm text-body-sm text-on-surface-variant">
            Tapez <strong>&#64;</strong> pour citer un participant déjà présent dans le fil : il en
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
                </button>
              </li>
            </ul>
          </div>

          <p *ngIf="cites.length > 0" class="font-body-sm text-body-sm text-on-surface-variant">
            Seront prévenus : {{ nomsCites }}.
          </p>
          <p *ngIf="erreurReponse" class="font-body-sm text-body-sm text-error">
            {{ erreurReponse }}
          </p>
          <button
            type="submit"
            [disabled]="!reponse.trim() || envoiEnCours"
            class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            {{ envoiEnCours ? 'Envoi…' : 'Envoyer' }}
          </button>
        </form>
      </ng-container>
    </div>
  `,
})
export class FilAlumni implements OnInit {
  sujet: ForumSujet | null = null;
  messages: ForumMessage[] = [];
  chargement = true;
  erreur = '';

  reponse = '';
  envoiEnCours = false;
  erreurReponse = '';

  citables: Mentionnable[] = [];
  suggestions: Mentionnable[] = [];
  cites: Mentionnable[] = [];

  private sujetId = 0;

  constructor(
    private route: ActivatedRoute,
    private alumniService: AlumniService
  ) {}

  ngOnInit(): void {
    this.sujetId = Number(this.route.snapshot.paramMap.get('id'));
    this.alumniService.getFil(this.sujetId).subscribe({
      next: (fil) => {
        this.sujet = fil.sujet;
        this.messages = fil.messages;
        this.chargement = false;
        this.alumniService.citables(this.sujetId).subscribe({
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
    const citesRetenus = this.cites.filter((c) => this.reponse.includes('@' + c.nom));
    this.alumniService
      .repondre(this.sujetId, this.reponse.trim(), citesRetenus.map((c) => c.id))
      .subscribe({
        next: (message) => {
          this.messages = [...this.messages, message];
          this.reponse = '';
          this.cites = [];
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
    this.alumniService.supprimerMessage(message.id).subscribe({
      next: () => (this.messages = this.messages.filter((m) => m.id !== message.id)),
      error: () => undefined,
    });
  }
}
