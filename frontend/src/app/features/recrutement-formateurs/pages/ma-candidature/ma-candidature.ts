import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import {
  CandidatureFormateur,
  LIBELLES_STATUT_CANDIDATURE,
  LIBELLES_TYPE_CONTRAT,
  LIBELLES_TYPE_DOCUMENT,
  OffreFormateur,
  PieceCandidatureFormateur,
  TYPES_DOCUMENT_RECOMMANDES,
  TYPES_DOCUMENT_REQUIS,
  TypeDocumentCandidatureFormateur,
} from '../../models/recrutement-formateur.models';
import { RecrutementFormateurService } from '../../services/recrutement-formateur.service';

/**
 * Espace du candidat formateur (QMS-007 §3) : dépose sa candidature à une offre,
 * complète ses pièces, suit l'état (Dossier reçu → En cours de traitement →
 * Terminé). Même patron que {@code DemandeAccreditationComponent}.
 */
@Component({
  selector: 'app-ma-candidature-formateur',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Recrutement des formateurs
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Ma candidature</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Déposez votre candidature à une offre de recrutement de formateur de l'INPG et suivez son évolution.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de votre dossier…"></app-ui-spinner>

      <!-- Aucune candidature : il faut une offre pour postuler -->
      <div *ngIf="!chargement && !candidature && !offreCiblee"
           class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg text-center space-y-3">
        <span class="material-symbols-outlined text-secondary text-[40px]">work</span>
        <p class="font-body-md text-body-md text-on-surface-variant">
          Choisissez d'abord une offre de recrutement pour déposer votre candidature.
        </p>
        <a routerLink="/offres-formateurs"
           class="inline-block px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity no-underline">
          Voir les offres
        </a>
      </div>

      <!-- Formulaire de dépôt -->
      <form
        *ngIf="!chargement && !candidature && offreCiblee"
        (ngSubmit)="soumettre()"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3"
      >
        <div class="p-3 rounded-lg bg-primary-container/40">
          <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Vous postulez pour</p>
          <p class="font-label-md text-label-md text-on-surface">{{ offreCiblee.intitule }}
            <span class="text-on-surface-variant">— {{ libellesContrat[offreCiblee.typeContrat] }}</span></p>
        </div>

        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2 pt-1">
          <span class="material-symbols-outlined text-secondary">person</span>Votre profil
        </h2>
        <div class="grid sm:grid-cols-2 gap-3">
          <div>
            <label for="nom" class="font-label-md text-label-md text-on-surface">Nom</label>
            <input id="nom" [(ngModel)]="nom" name="nom" required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
          <div>
            <label for="prenom" class="font-label-md text-label-md text-on-surface">Prénom</label>
            <input id="prenom" [(ngModel)]="prenom" name="prenom" required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
          <div>
            <label for="telephone" class="font-label-md text-label-md text-on-surface">Téléphone</label>
            <input id="telephone" [(ngModel)]="telephone" name="telephone" required
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
          <div>
            <label for="anneesExperience" class="font-label-md text-label-md text-on-surface">Années d'expérience (facultatif)</label>
            <input id="anneesExperience" type="number" min="0" [(ngModel)]="anneesExperience" name="anneesExperience"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </div>
        </div>
        <div>
          <label for="diplomePrincipal" class="font-label-md text-label-md text-on-surface">Diplôme principal (facultatif)</label>
          <input id="diplomePrincipal" [(ngModel)]="diplomePrincipal" name="diplomePrincipal"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
        </div>
        <div>
          <label for="lettreMotivation" class="font-label-md text-label-md text-on-surface">Lettre de motivation (facultatif)</label>
          <textarea id="lettreMotivation" [(ngModel)]="lettreMotivation" name="lettreMotivation" rows="4"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"></textarea>
        </div>

        <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
        <button type="submit" [disabled]="!formulaireValide || envoiEnCours"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50">
          {{ envoiEnCours ? 'Envoi…' : 'Déposer ma candidature' }}
        </button>
      </form>

      <!-- Candidature existante : suivi + pièces -->
      <ng-container *ngIf="!chargement && candidature">
        <!-- Progression -->
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
          <ol class="flex items-center w-full">
            <li *ngFor="let etape of etapes; let i = index" class="flex items-center" [class.flex-1]="i < etapes.length - 1">
              <div class="flex flex-col items-center gap-1 shrink-0">
                <div class="w-9 h-9 rounded-full flex items-center justify-center shrink-0"
                     [class.bg-secondary]="i <= etapeActuelle" [class.text-on-secondary]="i <= etapeActuelle"
                     [class.bg-surface-container]="i > etapeActuelle" [class.text-on-surface-variant]="i > etapeActuelle">
                  <span class="material-symbols-outlined text-[20px]">{{ i < etapeActuelle ? 'check' : etape.icone }}</span>
                </div>
                <span class="font-label-sm text-label-sm text-center max-w-[8rem]"
                      [class.text-primary]="i <= etapeActuelle" [class.text-on-surface-variant]="i > etapeActuelle">
                  {{ etape.libelle }}
                </span>
              </div>
              <div *ngIf="i < etapes.length - 1" class="flex-1 h-0.5 mx-2 mb-5"
                   [class.bg-secondary]="i < etapeActuelle" [class.bg-outline-variant]="i >= etapeActuelle"></div>
            </li>
          </ol>
        </div>

        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="font-headline-sm text-headline-sm text-primary">{{ candidature.intituleOffre || 'Ma candidature' }}</h2>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                {{ candidature.prenom }} {{ candidature.nom }} · déposée le {{ candidature.dateSoumission | date: 'd MMMM y' }}
              </p>
            </div>
            <span class="px-3 py-1.5 rounded-full font-label-sm text-label-sm"
              [ngClass]="{
                'bg-surface-container text-on-surface-variant': candidature.statut === 'DOSSIER_RECU',
                'bg-secondary-container text-on-secondary-container': candidature.statut === 'EN_COURS_DE_TRAITEMENT',
                'bg-success/15 text-success-strong': candidature.statut === 'TERMINE' && candidature.decision === 'RETENU',
                'bg-error-container text-on-error-container': candidature.statut === 'TERMINE' && candidature.decision === 'NON_RETENU',
                'bg-surface-container-high text-on-surface': candidature.statut === 'TERMINE' && !candidature.decision
              }">
              {{ libellesStatut[candidature.statut] }}
            </span>
          </div>

          <dl class="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-2 text-body-sm font-body-sm bg-surface-container-low rounded-lg p-3">
            <div>
              <dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Diplôme</dt>
              <dd class="text-on-surface">{{ candidature.diplomePrincipal || '—' }}</dd>
            </div>
            <div>
              <dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Expérience</dt>
              <dd class="text-on-surface">{{ candidature.anneesExperience != null ? candidature.anneesExperience + ' an(s)' : '—' }}</dd>
            </div>
            <div>
              <dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Type de contrat</dt>
              <dd class="text-on-surface">{{ candidature.typeContratOffre ? libellesContrat[candidature.typeContratOffre] : '—' }}</dd>
            </div>
          </dl>

          <div *ngIf="candidature.statut === 'TERMINE' && candidature.decision === 'RETENU'"
               class="mt-4 p-3 rounded-lg bg-success/10 border border-success/30 font-body-sm text-body-sm space-y-1">
            <p class="text-success-strong">Félicitations, votre candidature a été retenue.</p>
            <p class="text-on-surface-variant">Votre espace formateur est activé — reconnectez-vous avec les mêmes identifiants.</p>
          </div>
          <div *ngIf="candidature.statut === 'TERMINE' && candidature.decision === 'NON_RETENU'"
               class="mt-4 p-3 rounded-lg bg-error-container text-on-error-container font-body-sm text-body-sm">
            Votre candidature n'a pas été retenue.<span *ngIf="candidature.motifDecision"> Motif : {{ candidature.motifDecision }}</span>
          </div>
        </div>

        <!-- Pièces du dossier -->
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
              <span class="material-symbols-outlined text-secondary">description</span>Pièces du dossier
            </h2>
            <span class="px-3 py-1.5 rounded-full font-label-sm text-label-sm"
              [ngClass]="dossierComplet ? 'bg-success/15 text-success-strong' : 'bg-error-container/40 text-on-error-container'">
              {{ dossierComplet ? 'Pièces requises fournies' : typesRequisManquants.length + ' pièce(s) requise(s) manquante(s)' }}
            </span>
          </div>
          <p *ngIf="!dossierComplet" class="font-body-sm text-body-sm text-on-surface-variant">À fournir : {{ libellesManquants }}.</p>

          <input #champFichier type="file" class="hidden" (change)="fichierChoisi($event)" />

          <ul class="divide-y divide-outline-variant/50 border border-outline-variant rounded-lg overflow-hidden">
            <li *ngFor="let type of typesRequis" class="flex items-center gap-3 p-3 border-l-4"
                [class.border-l-success]="pieceDeType(type)" [class.border-l-transparent]="!pieceDeType(type)">
              <span class="material-symbols-outlined shrink-0"
                    [class.text-success-strong]="pieceDeType(type)" [class.text-outline]="!pieceDeType(type)">
                {{ pieceDeType(type) ? 'check_circle' : 'radio_button_unchecked' }}
              </span>
              <div class="flex-1 min-w-0">
                <p class="font-label-md text-label-md text-on-surface">{{ libellesType[type] }}</p>
                <p *ngIf="pieceDeType(type) as p" class="font-body-sm text-body-sm text-on-surface-variant truncate">{{ p.nomFichierOriginal }}</p>
                <p *ngIf="!pieceDeType(type)" class="font-body-sm text-body-sm text-on-surface-variant">Non fournie</p>
              </div>
              <div class="flex items-center gap-1 shrink-0">
                <button *ngIf="pieceDeType(type) as p" type="button" (click)="consulterPiece(p.id)" title="Consulter"
                        class="p-2 text-outline hover:text-secondary rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">visibility</span>
                </button>
                <button *ngIf="modifiable" type="button" (click)="ajouterPiece(type)" [disabled]="televersementEnCours"
                        [title]="pieceDeType(type) ? 'Remplacer' : 'Ajouter'"
                        class="p-2 text-outline hover:text-secondary rounded-full transition-colors disabled:opacity-40">
                  <span class="material-symbols-outlined text-[20px]">{{ pieceDeType(type) ? 'sync' : 'upload' }}</span>
                </button>
                <button *ngIf="modifiable && pieceDeType(type) as p" type="button" (click)="supprimerPiece(p.id)" title="Supprimer"
                        class="p-2 text-outline hover:text-error rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">delete</span>
                </button>
              </div>
            </li>
          </ul>

          <div class="space-y-2">
            <p class="font-label-md text-label-md text-on-surface">Pièces recommandées</p>
            <ul class="divide-y divide-outline-variant/50 border border-outline-variant rounded-lg overflow-hidden">
              <li *ngFor="let type of typesRecommandes" class="flex items-center gap-3 p-3 border-l-4"
                  [class.border-l-success]="pieceDeType(type)" [class.border-l-transparent]="!pieceDeType(type)">
                <span class="material-symbols-outlined shrink-0"
                      [class.text-success-strong]="pieceDeType(type)" [class.text-outline]="!pieceDeType(type)">
                  {{ pieceDeType(type) ? 'check_circle' : 'radio_button_unchecked' }}
                </span>
                <div class="flex-1 min-w-0">
                  <p class="font-label-md text-label-md text-on-surface">{{ libellesType[type] }}</p>
                  <p *ngIf="pieceDeType(type) as p" class="font-body-sm text-body-sm text-on-surface-variant truncate">{{ p.nomFichierOriginal }}</p>
                  <p *ngIf="!pieceDeType(type)" class="font-body-sm text-body-sm text-on-surface-variant">Non fournie</p>
                </div>
                <div class="flex items-center gap-1 shrink-0">
                  <button *ngIf="pieceDeType(type) as p" type="button" (click)="consulterPiece(p.id)" title="Consulter"
                          class="p-2 text-outline hover:text-secondary rounded-full transition-colors">
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                  </button>
                  <button *ngIf="modifiable" type="button" (click)="ajouterPiece(type)" [disabled]="televersementEnCours"
                          [title]="pieceDeType(type) ? 'Remplacer' : 'Ajouter'"
                          class="p-2 text-outline hover:text-secondary rounded-full transition-colors disabled:opacity-40">
                    <span class="material-symbols-outlined text-[20px]">{{ pieceDeType(type) ? 'sync' : 'upload' }}</span>
                  </button>
                  <button *ngIf="modifiable && pieceDeType(type) as p" type="button" (click)="supprimerPiece(p.id)" title="Supprimer"
                          class="p-2 text-outline hover:text-error rounded-full transition-colors">
                    <span class="material-symbols-outlined text-[20px]">delete</span>
                  </button>
                </div>
              </li>
            </ul>
          </div>

          <div class="space-y-2">
            <div class="flex items-center justify-between gap-3">
              <p class="font-label-md text-label-md text-on-surface">Autres documents (facultatif)</p>
              <button *ngIf="modifiable" type="button" (click)="ajouterPiece('AUTRE')" [disabled]="televersementEnCours"
                      class="flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant font-label-sm text-label-sm text-on-surface-variant hover:border-secondary hover:text-secondary transition-colors disabled:opacity-40">
                <span class="material-symbols-outlined text-[18px]">add</span>Ajouter
              </button>
            </div>
            <ul *ngIf="piecesAutres.length > 0" class="space-y-2">
              <li *ngFor="let p of piecesAutres" class="flex items-center gap-3 p-3 rounded-lg bg-surface-container-low">
                <span class="material-symbols-outlined text-on-surface-variant shrink-0">draft</span>
                <span class="flex-1 min-w-0 truncate font-body-sm text-body-sm text-on-surface">{{ p.nomFichierOriginal }}</span>
                <button type="button" (click)="consulterPiece(p.id)" title="Consulter"
                        class="p-2 text-outline hover:text-secondary rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">visibility</span>
                </button>
                <button *ngIf="modifiable" type="button" (click)="supprimerPiece(p.id)" title="Supprimer"
                        class="p-2 text-outline hover:text-error rounded-full transition-colors">
                  <span class="material-symbols-outlined text-[20px]">delete</span>
                </button>
              </li>
            </ul>
            <p *ngIf="piecesAutres.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">Aucun.</p>
          </div>

          <p *ngIf="televersementEnCours" class="font-body-sm text-body-sm text-on-surface-variant">Envoi du fichier…</p>
          <p *ngIf="erreurPiece" class="font-body-sm text-body-sm text-error">{{ erreurPiece }}</p>
        </div>
      </ng-container>
    </div>
  `,
})
export class MaCandidatureComponent implements OnInit {
  private service = inject(RecrutementFormateurService);
  private mediaProtege = inject(MediaProtegeService);
  private route = inject(ActivatedRoute);

  @ViewChild('champFichier') champFichier?: ElementRef<HTMLInputElement>;

  chargement = true;
  candidature: CandidatureFormateur | null = null;
  offreCiblee: OffreFormateur | null = null;

  libellesStatut = LIBELLES_STATUT_CANDIDATURE;
  libellesContrat = LIBELLES_TYPE_CONTRAT;
  libellesType = LIBELLES_TYPE_DOCUMENT;
  readonly typesRequis = TYPES_DOCUMENT_REQUIS;
  readonly typesRecommandes = TYPES_DOCUMENT_RECOMMANDES;

  // Formulaire de dépôt
  nom = '';
  prenom = '';
  telephone = '';
  diplomePrincipal = '';
  anneesExperience: number | null = null;
  lettreMotivation = '';
  envoiEnCours = false;
  erreur = '';

  // Téléversement
  private typeEnCours: TypeDocumentCandidatureFormateur = 'CV';
  televersementEnCours = false;
  erreurPiece = '';

  readonly etapes = [
    { statut: 'DOSSIER_RECU', libelle: 'Dossier reçu', icone: 'inbox' },
    { statut: 'EN_COURS_DE_TRAITEMENT', libelle: 'En cours de traitement', icone: 'fact_check' },
    { statut: 'TERMINE', libelle: 'Terminé', icone: 'task_alt' },
  ];

  get etapeActuelle(): number {
    if (!this.candidature) return 0;
    const i = this.etapes.findIndex((e) => e.statut === this.candidature!.statut);
    return i === -1 ? 0 : i;
  }

  get modifiable(): boolean {
    return this.candidature?.statut !== 'TERMINE';
  }

  get formulaireValide(): boolean {
    return !!this.nom.trim() && !!this.prenom.trim() && !!this.telephone.trim() && !!this.offreCiblee;
  }

  pieceDeType(type: TypeDocumentCandidatureFormateur): PieceCandidatureFormateur | undefined {
    return this.candidature?.pieces.find((p) => p.typeDocument === type);
  }

  get piecesAutres(): PieceCandidatureFormateur[] {
    return this.candidature?.pieces.filter((p) => p.typeDocument === 'AUTRE') ?? [];
  }

  get typesRequisManquants(): TypeDocumentCandidatureFormateur[] {
    return this.typesRequis.filter((t) => !this.pieceDeType(t));
  }

  get dossierComplet(): boolean {
    return this.typesRequisManquants.length === 0;
  }

  get libellesManquants(): string {
    return this.typesRequisManquants.map((t) => this.libellesType[t]).join(', ');
  }

  ngOnInit(): void {
    const offreId = Number(this.route.snapshot.queryParamMap.get('offre')) || null;
    this.service.getMesCandidatures().subscribe({
      next: (candidatures) => {
        this.candidature =
          (offreId ? candidatures.find((c) => c.offreId === offreId) : undefined) || candidatures[0] || null;
        if (!this.candidature && offreId) {
          this.service.getOffre(offreId).subscribe({
            next: (o) => {
              this.offreCiblee = o;
              this.chargement = false;
            },
            error: () => (this.chargement = false),
          });
        } else {
          this.chargement = false;
        }
      },
      error: () => (this.chargement = false),
    });
  }

  soumettre(): void {
    if (!this.formulaireValide || !this.offreCiblee) {
      return;
    }
    this.envoiEnCours = true;
    this.erreur = '';
    this.service
      .postuler({
        offreId: this.offreCiblee.id,
        nom: this.nom.trim(),
        prenom: this.prenom.trim(),
        telephone: this.telephone.trim(),
        diplomePrincipal: this.diplomePrincipal.trim() || null,
        anneesExperience: this.anneesExperience ? Number(this.anneesExperience) : null,
        lettreMotivation: this.lettreMotivation.trim() || null,
      })
      .subscribe({
        next: (candidature) => {
          this.candidature = candidature;
          this.offreCiblee = null;
          this.envoiEnCours = false;
        },
        error: (e) => {
          this.envoiEnCours = false;
          this.erreur = e?.error?.message || 'Le dépôt de la candidature a échoué.';
        },
      });
  }

  consulterPiece(pieceId: number): void {
    if (!this.candidature) {
      return;
    }
    this.mediaProtege.ouvrir(this.service.urlFichierPiece(this.candidature.id, pieceId));
  }

  ajouterPiece(type: TypeDocumentCandidatureFormateur): void {
    this.typeEnCours = type;
    this.erreurPiece = '';
    this.champFichier?.nativeElement.click();
  }

  fichierChoisi(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.length ? champ.files[0] : null;
    champ.value = '';
    if (fichier && this.candidature) {
      this.televerser(fichier, this.typeEnCours);
    }
  }

  private televerser(fichier: File, type: TypeDocumentCandidatureFormateur): void {
    if (!this.candidature) {
      return;
    }
    const ancienne = this.pieceDeType(type);
    this.televersementEnCours = true;
    this.erreurPiece = '';
    this.service.televerserPiece(this.candidature.id, fichier, type).subscribe({
      next: (piece) => {
        this.candidature!.pieces = [...this.candidature!.pieces, piece];
        if (ancienne && type !== 'AUTRE') {
          this.service.supprimerPiece(this.candidature!.id, ancienne.id).subscribe({
            next: () => (this.candidature!.pieces = this.candidature!.pieces.filter((p) => p.id !== ancienne.id)),
            error: () => undefined,
          });
        }
        this.televersementEnCours = false;
      },
      error: (e) => {
        this.televersementEnCours = false;
        this.erreurPiece = e?.error?.message || "L'ajout de la pièce a échoué.";
      },
    });
  }

  supprimerPiece(pieceId: number): void {
    if (!this.candidature || !confirm('Retirer cette pièce du dossier ?')) {
      return;
    }
    this.erreurPiece = '';
    this.service.supprimerPiece(this.candidature.id, pieceId).subscribe({
      next: () => (this.candidature!.pieces = this.candidature!.pieces.filter((p) => p.id !== pieceId)),
      error: (e) => (this.erreurPiece = e?.error?.message || 'La suppression a échoué.'),
    });
  }
}
