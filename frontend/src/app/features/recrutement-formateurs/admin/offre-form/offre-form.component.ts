import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import { erreurFichePoste } from '../../fiche-poste';
import {
  LIBELLES_TYPE_CONTRAT,
  OffreFormateur,
  OffreFormateurSaisie,
  StatutOffreFormateur,
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
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Recrutement des formateurs — QMS-007
        </p>
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

        <div>
          <span class="font-label-md text-label-md text-on-surface">Fiche de poste (PDF, facultatif)</span>
          <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
            Le document officiel du poste. Les candidats le téléchargent depuis l'offre publiée. 10 Mo au maximum.
          </p>

          <div *ngIf="fichePosteNom && !retirerFiche && !fichierChoisi"
               class="mt-2 flex flex-wrap items-center gap-3 p-3 rounded-lg bg-surface-container-low border border-outline-variant">
            <span class="material-symbols-outlined text-secondary" aria-hidden="true">description</span>
            <span class="min-w-0 flex-1 truncate font-body-sm text-body-sm text-on-surface">{{ fichePosteNom }}</span>
            <button type="button" (click)="consulterFichePoste()"
              class="px-3 py-1 rounded-lg bg-surface-container-lowest border border-outline-variant text-on-surface font-label-md text-label-md hover:bg-surface-container-low transition-colors">
              Consulter
            </button>
            <button type="button" (click)="retirerFiche = true"
              class="px-3 py-1 rounded-lg bg-surface-container-lowest border border-outline-variant text-error font-label-md text-label-md hover:bg-surface-container-low transition-colors">
              Retirer
            </button>
          </div>

          <div *ngIf="fichierChoisi"
               class="mt-2 flex flex-wrap items-center gap-3 p-3 rounded-lg bg-surface-container-low border border-outline-variant">
            <span class="material-symbols-outlined text-secondary" aria-hidden="true">upload_file</span>
            <span class="min-w-0 flex-1 truncate font-body-sm text-body-sm text-on-surface">
              {{ fichierChoisi.name }} <span class="text-on-surface-variant">· {{ tailleKo(fichierChoisi) }} Ko, envoyé à l'enregistrement</span>
            </span>
            <button type="button" (click)="fichierChoisi = null"
              class="px-3 py-1 rounded-lg bg-surface-container-lowest border border-outline-variant text-on-surface font-label-md text-label-md hover:bg-surface-container-low transition-colors">
              Annuler le choix
            </button>
          </div>

          <p *ngIf="retirerFiche"
             class="mt-2 flex flex-wrap items-center gap-3 p-3 rounded-lg bg-warning-container text-on-warning-container font-body-sm text-body-sm">
            La fiche « {{ fichePosteNom }} » sera retirée à l'enregistrement.
            <button type="button" (click)="retirerFiche = false" class="underline font-label-md text-label-md">Annuler</button>
          </p>

          <label for="fichePoste"
            class="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-surface-container-lowest border border-outline-variant text-on-surface font-label-md text-label-md hover:bg-surface-container-low transition-colors cursor-pointer focus-within:border-secondary">
            <span class="material-symbols-outlined text-[18px]" aria-hidden="true">attach_file</span>
            {{ fichierChoisi ? 'Choisir un autre fichier' : fichePosteNom && !retirerFiche ? 'Remplacer le fichier' : 'Joindre un PDF' }}
            <input id="fichePoste" type="file" accept="application/pdf,.pdf" class="sr-only" (change)="choisirFichier($event)" />
          </label>
          <p *ngIf="erreurFichier" class="mt-1 font-body-sm text-body-sm text-error">{{ erreurFichier }}</p>
        </div>

        <label *ngIf="peutPublier"
               class="flex items-start gap-3 p-3 rounded-lg bg-surface-container-low border border-outline-variant cursor-pointer">
          <input type="checkbox" [(ngModel)]="publierMaintenant" name="publierMaintenant"
                 class="mt-1 rounded border-outline-variant text-secondary focus:ring-secondary" />
          <span>
            <span class="block font-label-md text-label-md text-on-surface">Publier l'offre dès l'enregistrement</span>
            <span class="block font-body-sm text-body-sm text-on-surface-variant">
              Sans cela, l'offre reste en brouillon : les candidats ne la voient pas sur la page « Offres de recrutement ».
              Vous pourrez la publier plus tard depuis la liste.
            </span>
          </span>
        </label>
        <p *ngIf="statut === 'PUBLIEE'" class="flex items-center gap-2 font-body-sm text-body-sm text-success-strong">
          <span class="material-symbols-outlined text-[18px]" aria-hidden="true">visibility</span>
          Cette offre est publiée : les candidats la voient et peuvent postuler.
        </p>

        <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
        <div class="flex gap-3">
          <button type="submit" [disabled]="!form.intitule.trim() || enregistrement"
            class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50">
            {{ enregistrement ? 'Enregistrement…' : libelleBouton }}
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
  private mediaProtege = inject(MediaProtegeService);

  id: number | null = null;
  chargement = false;
  enregistrement = false;
  erreur = '';

  /** Fiche de poste déjà jointe à l'offre (nom du fichier), telle que le serveur la connaît. */
  fichePosteNom: string | null = null;
  /** Fichier choisi, envoyé après l'enregistrement de l'offre. */
  fichierChoisi: File | null = null;
  /** Retrait demandé, appliqué à l'enregistrement. */
  retirerFiche = false;
  /** État de l'offre côté serveur ; `null` tant qu'elle n'est pas créée. */
  statut: StatutOffreFormateur | null = null;
  /** Case « publier dès l'enregistrement » : sans elle, l'offre reste un brouillon invisible des candidats. */
  publierMaintenant = false;
  erreurFichier = '';

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
          this.fichePosteNom = o.fichePosteNom;
          this.statut = o.statut;
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
      next: (offre) => {
        // Une seconde tentative (fiche refusée) doit modifier cette offre, pas en créer une autre.
        this.id = offre.id;
        this.appliquerFichePoste(offre.id);
      },
      error: (e) => {
        this.enregistrement = false;
        this.erreur = e?.error?.message || "L'enregistrement a échoué.";
      },
    });
  }

  choisirFichier(evenement: Event): void {
    const saisie = evenement.target as HTMLInputElement;
    const fichier = saisie.files?.[0] ?? null;
    saisie.value = ''; // permet de re-choisir le même fichier après l'avoir annulé
    if (!fichier) {
      return;
    }
    const probleme = erreurFichePoste(fichier);
    this.erreurFichier = probleme ?? '';
    if (!probleme) {
      this.fichierChoisi = fichier;
      this.retirerFiche = false;
    }
  }

  /** Taille arrondie au Ko, jamais « 0 » pour un fichier non vide. */
  tailleKo(fichier: File): number {
    return Math.max(1, Math.round(fichier.size / 1024));
  }

  consulterFichePoste(): void {
    if (this.id) {
      this.mediaProtege.ouvrir(this.service.urlFichePoste(this.id));
    }
  }

  /** Envoie ou retire la fiche une fois l'offre enregistrée ; sans changement de fiche, on a fini. */
  private appliquerFichePoste(offreId: number): void {
    let etape: Observable<OffreFormateur> | null = null;
    if (this.fichierChoisi) {
      etape = this.service.joindreFichePoste(offreId, this.fichierChoisi);
    } else if (this.retirerFiche && this.fichePosteNom) {
      etape = this.service.retirerFichePoste(offreId);
    }
    if (!etape) {
      this.publierSiDemande(offreId);
      return;
    }
    etape.subscribe({
      next: () => this.publierSiDemande(offreId),
      error: (e) => {
        this.enregistrement = false;
        this.erreur =
          "L'offre est enregistrée, mais la fiche de poste n'a pas pu être mise à jour : " +
          (e?.error?.message || 'envoi impossible') +
          ' Corrigez puis enregistrez de nouveau.';
      },
    });
  }

  /** Un brouillon n'apparaît pas sur la vitrine : la publication se fait ici si elle a été demandée. */
  private publierSiDemande(offreId: number): void {
    if (!this.publierMaintenant || !this.peutPublier) {
      this.terminer();
      return;
    }
    this.service.publierOffre(offreId).subscribe({
      next: () => this.terminer(),
      error: (e) => {
        this.enregistrement = false;
        this.erreur =
          "L'offre est enregistrée, mais elle n'a pas pu être publiée : " +
          (e?.error?.message || 'publication impossible') +
          ' Vous pouvez réessayer depuis la liste des offres.';
      },
    });
  }

  /** Une offre déjà publiée ou clôturée ne repasse pas par la case de publication. */
  get peutPublier(): boolean {
    return this.statut !== 'PUBLIEE' && this.statut !== 'CLOTUREE';
  }

  get libelleBouton(): string {
    const publier = this.publierMaintenant && this.peutPublier;
    if (this.id) {
      return publier ? 'Enregistrer et publier' : 'Enregistrer';
    }
    return publier ? "Créer et publier l'offre" : "Créer l'offre (brouillon)";
  }

  private terminer(): void {
    this.router.navigate(['/responsable-admission/recrutement-formateurs/offres']);
  }
}
