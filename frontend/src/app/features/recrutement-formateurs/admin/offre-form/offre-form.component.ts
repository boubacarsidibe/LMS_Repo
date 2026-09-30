import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import {
  LIBELLES_TYPE_CONTRAT,
  OffreFormateurSaisie,
  TypeContratFormateur,
} from '../../models/recrutement-formateur.models';
import { AdminRecrutementFormateurService } from '../../services/admin-recrutement-formateur.service';

/** Création / édition d'une fiche de poste (page dédiée, patron session-form). */
@Component({
  selector: 'app-offre-form-formateurs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <a routerLink="/responsable-admission/recrutement-formateurs/offres"
           class="inline-flex items-center gap-1 font-label-sm text-label-sm text-on-surface-variant hover:text-primary transition-colors no-underline mb-2">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>Retour aux offres
        </a>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">
          {{ id ? "Modifier l'offre" : 'Nouvelle offre' }}
        </h1>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement…"></app-ui-spinner>

      <form *ngIf="!chargement" (ngSubmit)="enregistrer()"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3">
        <div>
          <label for="intitule" class="font-label-md text-label-md text-on-surface">Intitulé du poste</label>
          <input id="intitule" [(ngModel)]="form.intitule" name="intitule" required
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
        </div>
        <div class="grid sm:grid-cols-2 gap-3">
          <div>
            <label for="typeContrat" class="font-label-md text-label-md text-on-surface">Type de contrat</label>
            <select id="typeContrat" [(ngModel)]="form.typeContrat" name="typeContrat" required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors">
              <option *ngFor="let t of typesContrat" [ngValue]="t">{{ libellesContrat[t] }}</option>
            </select>
          </div>
          <div>
            <label for="domaine" class="font-label-md text-label-md text-on-surface">Domaine (facultatif)</label>
            <input id="domaine" [(ngModel)]="form.domaine" name="domaine"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
          <div>
            <label for="lieu" class="font-label-md text-label-md text-on-surface">Lieu (facultatif)</label>
            <input id="lieu" [(ngModel)]="form.lieu" name="lieu"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
          <div>
            <label for="nbPostes" class="font-label-md text-label-md text-on-surface">Nombre de postes</label>
            <input id="nbPostes" type="number" min="1" [(ngModel)]="form.nbPostes" name="nbPostes"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
          <div>
            <label for="dateLimite" class="font-label-md text-label-md text-on-surface">Date limite de candidature (facultatif)</label>
            <input id="dateLimite" type="date" [(ngModel)]="form.dateLimiteCandidature" name="dateLimite"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
        </div>
        <div>
          <label for="missions" class="font-label-md text-label-md text-on-surface">Description des missions</label>
          <textarea id="missions" [(ngModel)]="form.descriptionMissions" name="missions" rows="4"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"></textarea>
        </div>
        <div>
          <label for="profil" class="font-label-md text-label-md text-on-surface">Profil recherché (diplômes, expérience)</label>
          <textarea id="profil" [(ngModel)]="form.profilRecherche" name="profil" rows="3"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"></textarea>
        </div>

        <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
        <div class="flex gap-3">
          <button type="submit" [disabled]="!form.intitule.trim() || enregistrement"
            class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50">
            {{ enregistrement ? 'Enregistrement…' : id ? 'Enregistrer' : 'Créer l\\'offre (brouillon)' }}
          </button>
          <a routerLink="/responsable-admission/recrutement-formateurs/offres"
             class="px-4 py-2 rounded-lg border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:bg-surface-container transition-colors no-underline">
            Annuler
          </a>
        </div>
      </form>
    </div>
  `,
})
export class OffreFormFormateursComponent implements OnInit {
  private service = inject(AdminRecrutementFormateurService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  id: number | null = null;
  chargement = false;
  enregistrement = false;
  erreur = '';

  readonly typesContrat: TypeContratFormateur[] = ['VACATAIRE', 'CDD', 'CDI', 'PRESTATION'];
  libellesContrat = LIBELLES_TYPE_CONTRAT;

  form: OffreFormateurSaisie = {
    intitule: '',
    domaine: null,
    descriptionMissions: null,
    profilRecherche: null,
    typeContrat: 'VACATAIRE',
    lieu: null,
    nbPostes: 1,
    formationId: null,
    dateLimiteCandidature: null,
  };

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('id');
    if (param) {
      this.id = Number(param);
      this.chargement = true;
      this.service.getOffre(this.id).subscribe({
        next: (o) => {
          this.form = {
            intitule: o.intitule,
            domaine: o.domaine,
            descriptionMissions: o.descriptionMissions,
            profilRecherche: o.profilRecherche,
            typeContrat: o.typeContrat,
            lieu: o.lieu,
            nbPostes: o.nbPostes,
            formationId: o.formationId,
            dateLimiteCandidature: o.dateLimiteCandidature,
          };
          this.chargement = false;
        },
        error: () => {
          this.erreur = "L'offre est introuvable.";
          this.chargement = false;
        },
      });
    }
  }

  enregistrer(): void {
    if (!this.form.intitule.trim()) {
      return;
    }
    this.enregistrement = true;
    this.erreur = '';
    const payload: OffreFormateurSaisie = {
      ...this.form,
      intitule: this.form.intitule.trim(),
      nbPostes: this.form.nbPostes ? Number(this.form.nbPostes) : 1,
    };
    const appel = this.id
      ? this.service.modifierOffre(this.id, payload)
      : this.service.creerOffre(payload);
    appel.subscribe({
      next: () => this.router.navigate(['/responsable-admission/recrutement-formateurs/offres']),
      error: (e) => {
        this.enregistrement = false;
        this.erreur = e?.error?.message || "L'enregistrement a échoué.";
      },
    });
  }
}
