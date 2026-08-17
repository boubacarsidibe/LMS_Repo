import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { ForumMessage, ForumSujet } from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';

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
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3"
        >
          <label for="reponse" class="font-label-md text-label-md text-on-surface">Répondre</label>
          <textarea
            id="reponse"
            [(ngModel)]="reponse"
            name="reponse"
            rows="3"
            placeholder="Votre réponse…"
            class="w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
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
      },
      error: (e) => {
        this.erreur = e?.error?.message || 'Cette discussion est introuvable ou ne vous est pas accessible.';
        this.chargement = false;
      },
    });
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
    this.communication.repondre(this.sujetId, this.reponse.trim()).subscribe({
      next: (message) => {
        this.messages = [...this.messages, message];
        this.reponse = '';
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
}
