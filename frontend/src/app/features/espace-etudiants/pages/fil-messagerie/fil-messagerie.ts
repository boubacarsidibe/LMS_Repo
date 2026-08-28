import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, interval, switchMap } from 'rxjs';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { Conversation, MessagePrive } from '../../models/messagerie.models';
import { MessagerieService } from '../../services/messagerie.service';

/** Fil d'une conversation de messagerie privée. */
@Component({
  selector: 'app-fil-messagerie',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-4xl mx-auto">
      <a
        [routerLink]="urlListe"
        class="inline-flex items-center gap-1 font-label-md text-label-md text-on-surface-variant hover:text-secondary transition-colors"
      >
        <span class="material-symbols-outlined text-[18px]">arrow_back</span> Messagerie
      </a>

      <app-ui-spinner *ngIf="chargement" message="Chargement de la conversation…"></app-ui-spinner>
      <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>

      <ng-container *ngIf="!chargement && conversation">
        <header class="border-b border-outline-variant pb-4">
          <h1 class="font-display text-display text-on-surface leading-tight">{{ conversation.autrePersonneNom }}</h1>
        </header>

        <ul class="space-y-stack-md">
          <li
            *ngFor="let m of messages"
            class="rounded-xl border p-stack-lg max-w-[75%]"
            [ngClass]="
              m.estAuteur
                ? 'bg-secondary-container/30 border-secondary-container ml-auto'
                : 'bg-surface-container-lowest border-outline-variant mr-auto'
            "
          >
            <div class="flex items-center gap-2">
              <span class="font-label-md text-label-md text-on-surface">{{ m.auteurNom }}</span>
              <span class="font-body-sm text-body-sm text-on-surface-variant">{{ m.dateEnvoi }}</span>
            </div>
            <p class="font-body-md text-body-md text-on-surface mt-2 whitespace-pre-line">{{ m.contenu }}</p>
          </li>
        </ul>

        <p
          *ngIf="messages.length === 0"
          class="font-body-md text-body-md text-on-surface-variant text-center py-6"
        >
          Aucun message pour l'instant.
        </p>

        <form
          (ngSubmit)="repondre()"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
        >
          <label for="reponse" class="font-label-md text-label-md text-on-surface">Répondre</label>
          <textarea
            id="reponse"
            [(ngModel)]="reponse"
            name="reponse"
            rows="3"
            (keydown.control.enter)="repondre()"
            placeholder="Votre message…"
            class="w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
          <p *ngIf="erreurReponse" class="font-body-sm text-body-sm text-error">{{ erreurReponse }}</p>
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
export class FilMessagerie implements OnInit, OnDestroy {
  conversation: Conversation | null = null;
  messages: MessagePrive[] = [];
  chargement = true;
  erreur = '';

  reponse = '';
  envoiEnCours = false;
  erreurReponse = '';

  urlListe = '/espace-etudiant/messagerie';

  private conversationId = 0;
  private sondage?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messagerie: MessagerieService
  ) {
    if (this.router.url.includes('/formateur/')) {
      this.urlListe = '/espace-etudiant/formateur/messagerie';
    }
  }

  ngOnInit(): void {
    this.conversationId = Number(this.route.snapshot.paramMap.get('id'));
    this.charger();

    // Sans ce sondage, une réponse de l'autre personne n'apparaissait qu'après rechargement.
    this.sondage = interval(6000)
      .pipe(switchMap(() => this.messagerie.getFil(this.conversationId)))
      .subscribe({
        next: (fil) => {
          this.conversation = fil.conversation;
          this.messages = fil.messages;
        },
        error: () => undefined,
      });
  }

  ngOnDestroy(): void {
    this.sondage?.unsubscribe();
  }

  private charger(): void {
    this.messagerie.getFil(this.conversationId).subscribe({
      next: (fil) => {
        this.conversation = fil.conversation;
        this.messages = fil.messages;
        this.chargement = false;
      },
      error: (e) => {
        this.erreur = e?.error?.message || 'Cette conversation est introuvable ou ne vous est pas accessible.';
        this.chargement = false;
      },
    });
  }

  repondre(): void {
    if (!this.reponse.trim()) {
      return;
    }
    this.envoiEnCours = true;
    this.erreurReponse = '';
    this.messagerie.repondre(this.conversationId, this.reponse.trim()).subscribe({
      next: (message) => {
        this.messages = [...this.messages, message];
        this.reponse = '';
        this.envoiEnCours = false;
      },
      error: (e) => {
        this.envoiEnCours = false;
        this.erreurReponse = e?.error?.message || "L'envoi de votre message a échoué.";
      },
    });
  }
}
