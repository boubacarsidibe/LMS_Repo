import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import {
  DemandeEntreprise,
  FormationCatalogue,
  LIBELLES_STATUT_DEMANDE_ENTREPRISE,
  LIBELLES_TYPE_DEMANDE,
  TypeDemandeEntreprise,
} from '../../models/entreprise.models';
import { EntrepriseService } from '../../services/entreprise.service';

/**
 * Soumission d'un besoin de formation ou d'une demande de devis (TDR §H), et
 * suivi de toutes les demandes déjà déposées — contrairement à l'école de
 * formation (une seule demande active à la fois), une entreprise peut en
 * déposer plusieurs au fil du temps.
 */
@Component({
  selector: 'app-nouvelle-demande',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Espace Entreprise
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Mes demandes</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Exprimez un besoin de formation ou demandez un devis pour une session sur mesure, et suivez la
          réponse de l'équipe pédagogique.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de vos demandes…"></app-ui-spinner>

      <form
        *ngIf="!chargement"
        (ngSubmit)="soumettre()"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3"
      >
        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
          <span class="material-symbols-outlined text-secondary">add_business</span>
          Nouvelle demande
        </h2>

        <div>
          <label class="font-label-md text-label-md text-on-surface">Nature de la demande</label>
          <div class="grid sm:grid-cols-2 gap-3 mt-1">
            <button
              type="button"
              (click)="type = 'BESOIN_FORMATION'"
              [class.border-secondary]="type === 'BESOIN_FORMATION'"
              [class.bg-secondary-container/20]="type === 'BESOIN_FORMATION'"
              class="bg-surface-container-low rounded-lg border border-outline-variant p-4 text-left transition-colors cursor-pointer"
            >
              <span class="font-label-md text-label-md text-on-surface block">Besoin de formation</span>
              <span class="font-body-sm text-body-sm text-on-surface-variant">Décrivez librement votre besoin</span>
            </button>
            <button
              type="button"
              (click)="type = 'DEVIS_SESSION'"
              [class.border-secondary]="type === 'DEVIS_SESSION'"
              [class.bg-secondary-container/20]="type === 'DEVIS_SESSION'"
              class="bg-surface-container-low rounded-lg border border-outline-variant p-4 text-left transition-colors cursor-pointer"
            >
              <span class="font-label-md text-label-md text-on-surface block">Devis / session sur mesure</span>
              <span class="font-body-sm text-body-sm text-on-surface-variant">Effectif et dates souhaitées</span>
            </button>
          </div>
        </div>

        <h3 class="font-label-md text-label-md text-on-surface pt-2">Coordonnées de l'entreprise</h3>
        <div class="grid sm:grid-cols-2 gap-3">
          <div>
            <label for="raisonSociale" class="font-label-md text-label-md text-on-surface">Raison sociale</label>
            <input
              id="raisonSociale"
              [(ngModel)]="raisonSociale"
              name="raisonSociale"
              required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
          <div>
            <label for="secteur" class="font-label-md text-label-md text-on-surface">Secteur d'activité (facultatif)</label>
            <input
              id="secteur"
              [(ngModel)]="secteur"
              name="secteur"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
          <div>
            <label for="adresse" class="font-label-md text-label-md text-on-surface">Adresse</label>
            <input
              id="adresse"
              [(ngModel)]="adresse"
              name="adresse"
              required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
          <div>
            <label for="telephone" class="font-label-md text-label-md text-on-surface">Téléphone</label>
            <input
              id="telephone"
              [(ngModel)]="telephone"
              name="telephone"
              required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
        </div>

        <h3 class="font-label-md text-label-md text-on-surface pt-2">Détail de la demande</h3>
        <div>
          <label for="formation" class="font-label-md text-label-md text-on-surface">Formation du catalogue visée (facultatif)</label>
          <select
            id="formation"
            [(ngModel)]="formationId"
            name="formation"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          >
            <option [ngValue]="null">Aucune en particulier</option>
            <option *ngFor="let f of catalogue" [ngValue]="f.id">{{ f.intitule }}</option>
          </select>
        </div>
        <div>
          <label for="description" class="font-label-md text-label-md text-on-surface">Description</label>
          <textarea
            id="description"
            [(ngModel)]="description"
            name="description"
            rows="3"
            required
            placeholder="Décrivez votre besoin ou le contexte de votre demande de devis…"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
        </div>
        <div *ngIf="type === 'DEVIS_SESSION'" class="grid sm:grid-cols-2 gap-3">
          <div>
            <label for="effectif" class="font-label-md text-label-md text-on-surface">Effectif concerné</label>
            <input
              id="effectif"
              type="number"
              min="1"
              [(ngModel)]="effectifConcerne"
              name="effectif"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
          <div>
            <label for="dates" class="font-label-md text-label-md text-on-surface">Dates souhaitées</label>
            <input
              id="dates"
              [(ngModel)]="datesSouhaitees"
              name="dates"
              placeholder="Ex. : courant octobre 2026"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
            />
          </div>
        </div>

        <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
        <button
          type="submit"
          [disabled]="!formulaireValide || envoiEnCours"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
        >
          {{ envoiEnCours ? 'Envoi…' : 'Soumettre la demande' }}
        </button>
      </form>

      <div *ngIf="!chargement" class="space-y-stack-md">
        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
          <span class="material-symbols-outlined text-secondary">history</span>
          Historique
        </h2>
        <ul class="space-y-base">
          <li
            *ngFor="let d of demandes"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="font-label-md text-label-md text-on-surface">{{ libellesType[d.type] }}</p>
                <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                  Déposée le {{ d.dateSoumission | date: 'd MMMM y' }}
                  <span *ngIf="d.intituleFormation"> · {{ d.intituleFormation }}</span>
                </p>
              </div>
              <span
                class="px-3 py-1.5 rounded-full font-label-sm text-label-sm"
                [ngClass]="{
                  'bg-surface-container text-on-surface-variant': d.statut === 'SOUMISE',
                  'bg-secondary-container text-on-secondary-container': d.statut === 'EN_COURS',
                  'bg-success/15 text-success': d.statut === 'REPONDUE',
                  'bg-outline-variant/40 text-on-surface-variant': d.statut === 'CLOTUREE'
                }"
              >
                {{ libellesStatut[d.statut] }}
              </span>
            </div>
            <p class="font-body-md text-body-md text-on-surface mt-3 whitespace-pre-line">{{ d.description }}</p>
            <p *ngIf="d.reponseEquipe" class="mt-4 p-3 rounded-lg bg-secondary-container/20 font-body-sm text-body-sm text-on-surface">
              <strong>Réponse de l'équipe pédagogique</strong> ({{ d.dateReponse | date: 'd MMMM y' }}) :
              {{ d.reponseEquipe }}
            </p>
          </li>
        </ul>
        <p *ngIf="demandes.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">
          Aucune demande déposée pour l'instant.
        </p>
      </div>
    </div>
  `,
})
export class NouvelleDemande implements OnInit {
  private entrepriseService = inject(EntrepriseService);
  private route = inject(ActivatedRoute);

  chargement = true;
  demandes: DemandeEntreprise[] = [];
  catalogue: FormationCatalogue[] = [];
  libellesStatut = LIBELLES_STATUT_DEMANDE_ENTREPRISE;
  libellesType = LIBELLES_TYPE_DEMANDE;

  type: TypeDemandeEntreprise = 'BESOIN_FORMATION';
  raisonSociale = '';
  secteur = '';
  adresse = '';
  telephone = '';
  formationId: number | null = null;
  description = '';
  effectifConcerne: number | null = null;
  datesSouhaitees = '';
  envoiEnCours = false;
  erreur = '';

  ngOnInit(): void {
    const formationIdParam = this.route.snapshot.queryParamMap.get('formationId');
    if (formationIdParam) {
      this.formationId = Number(formationIdParam);
    }
    this.entrepriseService.getMesDemandes().subscribe({
      next: (demandes) => {
        this.demandes = demandes;
        if (demandes.length > 0) {
          this.raisonSociale = demandes[0].raisonSociale;
        }
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
    this.entrepriseService.getCatalogue().subscribe({
      next: (formations) => (this.catalogue = formations || []),
      error: () => (this.catalogue = []),
    });
  }

  get formulaireValide(): boolean {
    return !!this.raisonSociale.trim() && !!this.adresse.trim() && !!this.telephone.trim() && !!this.description.trim();
  }

  soumettre(): void {
    if (!this.formulaireValide) {
      return;
    }
    this.envoiEnCours = true;
    this.erreur = '';
    this.entrepriseService
      .soumettreDemande({
        entreprise: {
          raisonSociale: this.raisonSociale.trim(),
          secteur: this.secteur.trim() || null,
          adresse: this.adresse.trim(),
          telephone: this.telephone.trim(),
        },
        type: this.type,
        formationId: this.formationId,
        description: this.description.trim(),
        effectifConcerne: this.type === 'DEVIS_SESSION' ? this.effectifConcerne : null,
        datesSouhaitees: this.type === 'DEVIS_SESSION' ? this.datesSouhaitees.trim() || null : null,
      })
      .subscribe({
        next: (demande) => {
          this.demandes = [demande, ...this.demandes];
          this.description = '';
          this.formationId = null;
          this.effectifConcerne = null;
          this.datesSouhaitees = '';
          this.envoiEnCours = false;
        },
        error: (e) => {
          this.envoiEnCours = false;
          this.erreur = e?.error?.message || 'La soumission de la demande a échoué.';
        },
      });
  }
}
