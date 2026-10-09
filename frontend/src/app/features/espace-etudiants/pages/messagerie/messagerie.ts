import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { Contactable, Conversation } from '../../models/messagerie.models';
import { MessagerieService } from '../../services/messagerie.service';

/** Liste des conversations de messagerie privée (apprenant ↔ formateur). */
@Component({
  selector: 'app-messagerie',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Messagerie</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Vos échanges privés, en dehors du forum.
          </p>
        </div>
        <button
          type="button"
          (click)="ouvrirCompositeur()"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity"
        >
          {{ compositeurOuvert ? 'Annuler' : 'Nouveau message' }}
        </button>
      </header>

      <form
        *ngIf="compositeurOuvert"
        (ngSubmit)="envoyer()"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
      >
        <div>
          <label for="destinataire" class="font-label-md text-label-md text-on-surface">Destinataire</label>
          <select
            id="destinataire"
            [(ngModel)]="destinataireId"
            name="destinataire"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          >
            <option [ngValue]="null">Choisir…</option>
            <option *ngFor="let c of contactables" [ngValue]="c.id">{{ c.nom }} · {{ c.role }}</option>
          </select>
          <p *ngIf="!chargementContactables && contactables.length === 0" class="font-body-sm text-body-sm text-on-surface-variant mt-1">
            Personne à contacter pour l'instant.
          </p>
        </div>
        <div>
          <label for="premierMessage" class="font-label-md text-label-md text-on-surface">Message</label>
          <textarea
            id="premierMessage"
            [(ngModel)]="premierMessage"
            name="premierMessage"
            rows="3"
            placeholder="Votre message…"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
        </div>
        <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
        <button
          type="submit"
          [disabled]="!destinataireId || !premierMessage.trim() || envoiEnCours"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
        >
          {{ envoiEnCours ? 'Envoi…' : 'Envoyer' }}
        </button>
      </form>

      <app-ui-spinner *ngIf="chargement" message="Chargement de la messagerie…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && conversations.length === 0"
        icone="chat"
        titre="Aucune conversation"
        description="Envoyez un premier message pour démarrer une conversation."
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && conversations.length > 0" class="space-y-base">
        <li
          *ngFor="let c of conversations"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant hover:border-secondary transition-colors shadow-sm"
        >
          <a [routerLink]="[urlFil, c.id]" class="flex items-center justify-between gap-4 p-stack-lg">
            <div class="min-w-0">
              <p class="font-label-md text-label-md text-on-surface">{{ c.autrePersonneNom }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant truncate max-w-md">
                {{ c.dernierMessage || 'Conversation ouverte' }}
              </p>
            </div>
            <div class="flex items-center gap-3 shrink-0">
              <span class="font-body-sm text-body-sm text-on-surface-variant">{{ c.dateDernierMessage }}</span>
              <span
                *ngIf="c.nonLus > 0"
                class="min-w-[22px] h-[22px] px-1.5 flex items-center justify-center rounded-full bg-error text-on-error text-[11px] font-bold"
              >
                {{ c.nonLus }}
              </span>
            </div>
          </a>
        </li>
      </ul>
    </div>
  `,
})
export class Messagerie implements OnInit, OnDestroy {
  conversations: Conversation[] = [];
  contactables: Contactable[] = [];
  chargement = true;
  chargementContactables = true;

  compositeurOuvert = false;
  destinataireId: number | null = null;
  premierMessage = '';
  envoiEnCours = false;
  erreur = '';

  /** Racine différente selon l'espace (étudiant vs formateur), même shell partagé. */
  urlFil = '/espace-etudiant/messagerie';

  private sondage?: Subscription;

  constructor(
    private messagerie: MessagerieService,
    private router: Router
  ) {
    if (this.router.url.startsWith('/espace-etudiant/formateur')) {
      this.urlFil = '/espace-etudiant/formateur/messagerie';
    }
  }

  ngOnInit(): void {
    this.charger();

    // Sans ce sondage, un message reçu n'apparaissait qu'après rechargement.
    this.sondage = interval(8000).subscribe(() => {
      this.messagerie.getConversations().subscribe({
        next: (conversations) => (this.conversations = conversations),
        error: () => undefined,
      });
    });
  }

  ngOnDestroy(): void {
    this.sondage?.unsubscribe();
  }

  private charger(): void {
    this.messagerie.getConversations().subscribe({
      next: (conversations) => {
        this.conversations = conversations;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  ouvrirCompositeur(): void {
    this.compositeurOuvert = !this.compositeurOuvert;
    if (this.compositeurOuvert && this.contactables.length === 0) {
      this.messagerie.getContactables().subscribe({
        next: (contactables) => {
          this.contactables = contactables;
          this.chargementContactables = false;
        },
        error: () => (this.chargementContactables = false),
      });
    }
  }

  envoyer(): void {
    if (!this.destinataireId || !this.premierMessage.trim()) {
      return;
    }
    this.envoiEnCours = true;
    this.erreur = '';
    this.messagerie.demarrerConversation(this.destinataireId, this.premierMessage.trim()).subscribe({
      next: (conversation) => {
        this.envoiEnCours = false;
        this.compositeurOuvert = false;
        this.premierMessage = '';
        this.destinataireId = null;
        this.router.navigate([this.urlFil, conversation.id]);
      },
      error: (e) => {
        this.envoiEnCours = false;
        this.erreur = e?.error?.message || "L'envoi du message a échoué.";
      },
    });
  }
}
