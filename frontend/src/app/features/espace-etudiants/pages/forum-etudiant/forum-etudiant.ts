import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { CoursEtudiant, ForumSujet } from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';
import { ScolariteService } from '../../services/scolarite.service';

/**
 * Forum de discussion (TDR §3.2), branché sur le backend.
 *
 * <p>L'étudiant ne peut ouvrir un sujet que dans un cours de son cursus, ou dans
 * le salon général.
 */
@Component({
  selector: 'app-forum-etudiant',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-tight">Forum</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Échangez avec votre promotion et vos formateurs.
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
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3"
      >
        <div>
          <label for="titre" class="font-label-md text-label-md text-on-surface">Titre</label>
          <input
            id="titre"
            [(ngModel)]="nouveauTitre"
            name="titre"
            required
            placeholder="Votre question en une phrase"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <div>
          <label for="cours" class="font-label-md text-label-md text-on-surface">Cours</label>
          <select
            id="cours"
            [(ngModel)]="nouveauCoursId"
            name="cours"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          >
            <option [ngValue]="null">Salon général</option>
            <option *ngFor="let c of cours" [ngValue]="c.id">{{ c.titre }}</option>
          </select>
        </div>
        <div>
          <label for="message" class="font-label-md text-label-md text-on-surface">Message</label>
          <textarea
            id="message"
            [(ngModel)]="nouveauMessage"
            name="message"
            rows="4"
            placeholder="Détaillez votre question…"
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
        description="Soyez le premier à lancer une discussion."
        actionLibelle="Ouvrir un sujet"
        (action)="formulaireOuvert = true"
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && sujets.length > 0" class="space-y-base">
        <li
          *ngFor="let s of sujets"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant hover:border-secondary transition-colors"
        >
          <a [routerLink]="['/espace-etudiant/forum', s.id]" class="block p-stack-lg">
            <div class="flex flex-wrap items-center gap-2">
              <span
                *ngIf="s.epingle"
                class="material-symbols-outlined text-[16px] text-secondary"
                title="Sujet épinglé"
                >push_pin</span
              >
              <h2 class="font-label-md text-label-md text-on-surface">{{ s.titre }}</h2>
              <span
                *ngIf="s.clos"
                class="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm"
                >Clos</span
              >
              <span
                class="px-2 py-0.5 rounded-full font-label-sm text-label-sm"
                [ngClass]="
                  s.coursTitre
                    ? 'bg-secondary-container text-on-secondary-container'
                    : 'bg-surface-container text-on-surface-variant'
                "
              >
                {{ s.coursTitre || 'Général' }}
              </span>
            </div>
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
export class ForumEtudiant implements OnInit {
  sujets: ForumSujet[] = [];
  cours: CoursEtudiant[] = [];
  chargement = true;

  formulaireOuvert = false;
  nouveauTitre = '';
  nouveauCoursId: number | null = null;
  nouveauMessage = '';
  creationEnCours = false;
  erreurCreation = '';

  constructor(
    private communication: CommunicationService,
    private scolarite: ScolariteService
  ) {}

  ngOnInit(): void {
    forkJoin({
      sujets: this.communication.getSujets(),
      cours: this.scolarite.getMesCours(),
    }).subscribe({
      next: ({ sujets, cours }) => {
        this.sujets = sujets;
        this.cours = cours;
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
    this.communication
      .ouvrirSujet({
        titre: this.nouveauTitre.trim(),
        coursId: this.nouveauCoursId,
        premierMessage: this.nouveauMessage.trim(),
      })
      .subscribe({
        next: (sujet) => {
          this.sujets = [sujet, ...this.sujets];
          this.creationEnCours = false;
          this.formulaireOuvert = false;
          this.nouveauTitre = '';
          this.nouveauMessage = '';
          this.nouveauCoursId = null;
        },
        error: (e) => {
          this.creationEnCours = false;
          this.erreurCreation = e?.error?.message || "L'ouverture du sujet a échoué.";
        },
      });
  }
}
