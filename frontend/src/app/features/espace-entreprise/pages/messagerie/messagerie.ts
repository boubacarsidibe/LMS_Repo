import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { MessageEntreprise } from '../../models/entreprise.models';
import { EntrepriseService } from '../../services/entreprise.service';

/** Canal privé entre l'entreprise connectée et l'équipe pédagogique (TDR §H). */
@Component({
  selector: 'app-messagerie-entreprise',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-4xl mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Espace Entreprise
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Messagerie</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Échangez directement avec l'équipe pédagogique au sujet de vos demandes.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de la messagerie…"></app-ui-spinner>

      <ng-container *ngIf="!chargement">
        <ul class="space-y-stack-md">
          <li
            *ngFor="let m of messages"
            class="rounded-xl border p-stack-lg"
            [ngClass]="
              m.auteurEntreprise
                ? 'bg-secondary-container/30 border-secondary-container ml-0 sm:ml-12'
                : 'bg-surface-container-lowest border-outline-variant mr-0 sm:mr-12'
            "
          >
            <div class="flex items-center gap-2">
              <span class="font-label-md text-label-md text-on-surface">{{ m.auteurNom }}</span>
              <span class="font-body-sm text-body-sm text-on-surface-variant">
                {{ m.dateCreation | date: 'd MMM, HH:mm' }}
              </span>
            </div>
            <p class="font-body-md text-body-md text-on-surface mt-2 whitespace-pre-line">{{ m.contenu }}</p>
          </li>
        </ul>

        <p
          *ngIf="messages.length === 0"
          class="font-body-md text-body-md text-on-surface-variant text-center py-6"
        >
          Aucun message pour l'instant. Lancez la discussion.
        </p>

        <form
          (ngSubmit)="envoyer()"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
        >
          <label for="message" class="font-label-md text-label-md text-on-surface">Votre message</label>
          <textarea
            id="message"
            [(ngModel)]="contenu"
            name="message"
            rows="3"
            placeholder="Votre message…"
            class="w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
          <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
          <button
            type="submit"
            [disabled]="!contenu.trim() || envoiEnCours"
            class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            {{ envoiEnCours ? 'Envoi…' : 'Envoyer' }}
          </button>
        </form>
      </ng-container>
    </div>
  `,
})
export class MessagerieEntreprise implements OnInit {
  private entrepriseService = inject(EntrepriseService);

  messages: MessageEntreprise[] = [];
  chargement = true;
  contenu = '';
  envoiEnCours = false;
  erreur = '';

  ngOnInit(): void {
    this.entrepriseService.getMesMessages().subscribe({
      next: (messages) => {
        this.messages = messages || [];
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  envoyer(): void {
    if (!this.contenu.trim()) {
      return;
    }
    this.envoiEnCours = true;
    this.erreur = '';
    this.entrepriseService.envoyerMessage(this.contenu.trim()).subscribe({
      next: (message) => {
        this.messages = [...this.messages, message];
        this.contenu = '';
        this.envoiEnCours = false;
      },
      error: (e) => {
        this.envoiEnCours = false;
        this.erreur = e?.error?.message || "L'envoi de votre message a échoué.";
      },
    });
  }
}
