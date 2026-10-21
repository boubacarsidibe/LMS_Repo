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
          <div *ngIf="fichierSelectionne" class="flex items-center gap-2 px-3 py-2 bg-secondary-container/20 rounded-lg">
            <span class="material-symbols-outlined text-[18px] text-secondary">attach_file</span>
            <span class="font-label-sm text-label-sm text-on-surface truncate max-w-xs">{{ fichierSelectionne.name }}</span>
            <button type="button" (click)="retirerFichier()" class="ml-auto p-1 text-outline hover:text-error transition-colors" title="Retirer le fichier" aria-label="Retirer le fichier">
              <span class="material-symbols-outlined text-[16px]">close</span>
            </button>
          </div>
          <p *ngIf="erreurReponse" class="font-body-sm text-body-sm text-error">{{ erreurReponse }}</p>
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
  fichierSelectionne: File | null = null;
  telechargementEnCoursId: number | null = null;

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
    this.messagerie.repondre(this.conversationId, this.reponse.trim(), this.fichierSelectionne).subscribe({
      next: (message) => {
        this.messages = [...this.messages, message];
        this.reponse = '';
        this.fichierSelectionne = null;
        this.envoiEnCours = false;
      },
      error: (e) => {
        this.envoiEnCours = false;
        this.erreurReponse = e?.error?.message || "L'envoi de votre message a échoué.";
      },
    });
  }

  onFichierSelectionne(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fichierSelectionne = input.files?.[0] ?? null;
  }

  retirerFichier(): void {
    this.fichierSelectionne = null;
  }

  telechargerPieceJointe(message: MessagePrive): void {
    if (!message.pieceJointeNom) return;
    this.telechargementEnCoursId = message.id;
    this.messagerie.telechargerPieceJointe(message.id).subscribe({
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
