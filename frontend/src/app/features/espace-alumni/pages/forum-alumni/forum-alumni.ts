import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { ForumSujet } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/**
 * Forum de l'espace Alumni (TDR §C), suite de l'espace alumni livré sans forum
 * faute de portée sur {@code ForumSujet} (voir migration V13). Un seul salon
 * plat : pas de sélecteur de cours, un alumni n'en a plus.
 */
@Component({
  selector: 'app-forum-alumni',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Forum Alumni</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Échangez avec les autres anciens étudiants de l'INPG.
          </p>
        </div>
        <button
          type="button"
          (click)="formulaireOuvert = !formulaireOuvert"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity"
        >
          {{ formulaireOuvert ? 'Annuler' : 'Nouveau sujet' }}
        </button>
      </header>

      <form
        *ngIf="formulaireOuvert"
        (ngSubmit)="ouvrirSujet()"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
      >
        <div>
          <label for="titre" class="font-label-md text-label-md text-on-surface">Titre</label>
          <input
            id="titre"
            [(ngModel)]="nouveauTitre"
            name="titre"
            required
            placeholder="Votre sujet en une phrase"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <div>
          <label for="message" class="font-label-md text-label-md text-on-surface">Message</label>
          <textarea
            id="message"
            [(ngModel)]="nouveauMessage"
            name="message"
            rows="4"
            placeholder="Détaillez votre sujet…"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
        </div>
        <p *ngIf="erreurCreation" class="font-body-sm text-body-sm text-error">
          {{ erreurCreation }}
        </p>
        <button
          type="submit"
          [disabled]="!nouveauTitre.trim() || creationEnCours"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
        >
          {{ creationEnCours ? 'Publication…' : 'Publier le sujet' }}
        </button>
      </form>

      <app-ui-spinner *ngIf="chargement" message="Chargement du forum…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && sujets.length === 0"
        icone="forum"
        titre="Aucun sujet ouvert"
        description="Soyez le premier à lancer une discussion entre alumni."
        actionLibelle="Ouvrir un sujet"
        (action)="formulaireOuvert = true"
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && sujets.length > 0" class="space-y-base">
        <li
          *ngFor="let s of sujets"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant hover:border-secondary transition-colors shadow-sm"
        >
          <a [routerLink]="['/espace-alumni/forum', s.id]" class="block p-stack-lg">
            <h2 class="font-label-md text-label-md text-on-surface">{{ s.titre }}</h2>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-2">
              Ouvert par {{ s.auteur }} · {{ s.dateCreation | date: 'd MMM y' }} ·
              {{ s.nbMessages }} message(s)
              <span *ngIf="s.nbMessages > 0">
                · dernier {{ s.dateDernierMessage | date: 'd MMM, HH:mm' }}</span
              >
            </p>
          </a>
        </li>
      </ul>
    </div>
  `,
})
export class ForumAlumni implements OnInit {
  sujets: ForumSujet[] = [];
  chargement = true;

  formulaireOuvert = false;
  nouveauTitre = '';
  nouveauMessage = '';
  creationEnCours = false;
  erreurCreation = '';

  constructor(private alumniService: AlumniService) {}

  ngOnInit(): void {
    this.alumniService.getSujets().subscribe({
      next: (sujets) => {
        this.sujets = sujets;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  ouvrirSujet(): void {
    if (!this.nouveauTitre.trim()) {
      return;
    }
    this.creationEnCours = true;
    this.erreurCreation = '';
    this.alumniService.ouvrirSujet(this.nouveauTitre.trim(), this.nouveauMessage.trim()).subscribe({
      next: (sujet) => {
        this.sujets = [sujet, ...this.sujets];
        this.creationEnCours = false;
        this.formulaireOuvert = false;
        this.nouveauTitre = '';
        this.nouveauMessage = '';
      },
      error: (e) => {
        this.creationEnCours = false;
        this.erreurCreation = e?.error?.message || "L'ouverture du sujet a échoué.";
      },
    });
  }
}
