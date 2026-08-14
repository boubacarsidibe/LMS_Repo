import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { CoursEtudiant, SupportEtudiant, SyntheseEvaluation } from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';
import { ScolariteService } from '../../services/scolarite.service';
import { environment } from '../../../../../environments/environment';

/**
 * Fiche d'un cours autorisé : ses supports, la progression de l'étudiant et
 * son évaluation de satisfaction (TDR §3.5 et §3.7).
 */
@Component({
  selector: 'app-cours-detail-etudiant',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg">
      <a
        routerLink="/espace-etudiant/mes-cours"
        class="inline-flex items-center gap-1 font-label-md text-label-md text-on-surface-variant hover:text-secondary transition-colors"
      >
        <span class="material-symbols-outlined text-[18px]">arrow_back</span> Mes cours
      </a>

      <app-ui-spinner *ngIf="chargement" message="Chargement du cours…"></app-ui-spinner>
      <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>

      <ng-container *ngIf="!chargement && cours">
        <header
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg"
        >
          <p class="font-label-sm text-label-sm text-on-surface-variant">
            {{ cours.code }} · {{ cours.semestre }} · {{ cours.volumeHoraire }} h
          </p>
          <h1 class="font-headline-lg text-headline-lg text-on-surface mt-1">{{ cours.titre }}</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-2 max-w-3xl">
            {{ cours.description }}
          </p>
          <p
            *ngIf="cours.formateurs.length"
            class="font-body-sm text-body-sm text-on-surface-variant mt-2"
          >
            Assuré par {{ cours.formateurs.join(', ') }}
          </p>

          <div class="mt-4 max-w-md">
            <div class="flex items-center justify-between font-label-sm text-label-sm mb-1">
              <span class="text-on-surface-variant">Ma progression</span>
              <span class="text-secondary tabular-nums">{{ cours.progression }}%</span>
            </div>
            <div class="h-2 rounded-full bg-surface-container overflow-hidden">
              <div
                class="h-full bg-secondary rounded-full transition-all"
                [style.width.%]="cours.progression"
              ></div>
            </div>
          </div>
        </header>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-gutter">
          <!-- Supports -->
          <section class="lg:col-span-2 space-y-stack-md">
            <h2 class="font-headline-sm text-headline-sm text-on-surface">Supports de cours</h2>

            <app-ui-empty-state
              *ngIf="supports.length === 0"
              icone="folder_open"
              titre="Aucun support publié"
              description="Le formateur n’a pas encore déposé de ressource pour ce cours."
            ></app-ui-empty-state>

            <ul *ngIf="supports.length > 0" class="space-y-base">
              <li
                *ngFor="let s of supports"
                class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-md flex items-center gap-4"
              >
                <span
                  class="material-symbols-outlined text-[22px]"
                  [class.text-secondary]="s.consulte"
                  [class.text-outline]="!s.consulte"
                  >{{ s.consulte ? 'task_alt' : 'draft' }}</span
                >
                <div class="min-w-0 flex-1">
                  <p class="font-label-md text-label-md text-on-surface truncate">{{ s.titre }}</p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant">
                    {{ s.typeRessource }}
                    <span *ngIf="s.datePublication"> · {{ s.datePublication | date: 'd MMM y' }}</span>
                  </p>
                </div>
                <a
                  *ngIf="s.url"
                  [href]="lienAbsolu(s.url)"
                  (click)="marquerConsulte(s)"
                  target="_blank"
                  rel="noopener"
                  class="px-3 py-1.5 rounded-lg bg-secondary text-on-secondary font-label-sm text-label-sm hover:opacity-90 transition-opacity shrink-0"
                >
                  Ouvrir
                </a>
                <button
                  *ngIf="!s.consulte"
                  type="button"
                  (click)="marquerConsulte(s)"
                  class="px-3 py-1.5 rounded-lg border border-outline-variant font-label-sm text-label-sm text-on-surface-variant hover:border-secondary hover:text-secondary transition-colors shrink-0"
                >
                  Marquer lu
                </button>
              </li>
            </ul>
          </section>

          <!-- Évaluation de satisfaction -->
          <aside class="space-y-stack-md">
            <h2 class="font-headline-sm text-headline-sm text-on-surface">Mon évaluation</h2>
            <div
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3"
            >
              <p class="font-body-sm text-body-sm text-on-surface-variant">
                Votre avis alimente le suivi qualité de la formation.
              </p>

              <div class="flex items-center gap-1" role="radiogroup" aria-label="Note de 1 à 5">
                <button
                  *ngFor="let etoile of [1, 2, 3, 4, 5]"
                  type="button"
                  role="radio"
                  [attr.aria-checked]="note === etoile"
                  [attr.aria-label]="etoile + ' sur 5'"
                  (click)="note = etoile"
                  class="p-1 transition-transform hover:scale-110"
                >
                  <span
                    class="material-symbols-outlined text-[26px]"
                    [class.text-secondary]="note >= etoile"
                    [class.text-outline-variant]="note < etoile"
                    [style.font-variation-settings]="
                      note >= etoile ? &quot;'FILL' 1&quot; : &quot;'FILL' 0&quot;
                    "
                    >star</span
                  >
                </button>
              </div>

              <textarea
                [(ngModel)]="commentaire"
                rows="4"
                placeholder="Votre commentaire (facultatif)"
                aria-label="Commentaire sur le cours"
                class="w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
              ></textarea>

              <button
                type="button"
                (click)="envoyerEvaluation()"
                [disabled]="note === 0 || envoiEnCours"
                class="w-full py-2.5 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
              >
                {{ envoiEnCours ? 'Envoi…' : cours.maNote ? 'Mettre à jour' : 'Envoyer' }}
              </button>

              <p *ngIf="messageEvaluation" class="font-label-md text-label-md text-secondary">
                {{ messageEvaluation }}
              </p>
              <p *ngIf="erreurEvaluation" class="font-body-sm text-body-sm text-error">
                {{ erreurEvaluation }}
              </p>
            </div>

            <div
              *ngIf="synthese"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg"
            >
              <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
                Satisfaction du groupe
              </p>
              <p class="font-headline-md text-headline-md text-on-surface mt-1 tabular-nums">
                {{ synthese.moyenne ? (synthese.moyenne | number: '1.1-1') : '—' }}
                <span class="font-body-md text-body-md text-on-surface-variant">/ 5</span>
              </p>
              <p class="font-body-sm text-body-sm text-on-surface-variant">
                {{ synthese.nombreEvaluations }} évaluation(s)
              </p>
            </div>
          </aside>
        </div>
      </ng-container>
    </div>
  `,
})
export class CoursDetailEtudiant implements OnInit {
  cours: CoursEtudiant | null = null;
  supports: SupportEtudiant[] = [];
  synthese: SyntheseEvaluation | null = null;
  chargement = true;
  erreur = '';

  note = 0;
  commentaire = '';
  envoiEnCours = false;
  messageEvaluation = '';
  erreurEvaluation = '';

  private coursId = 0;

  constructor(
    private route: ActivatedRoute,
    private scolarite: ScolariteService,
    private communication: CommunicationService
  ) {}

  ngOnInit(): void {
    this.coursId = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({
      cours: this.scolarite.getCours(this.coursId),
      supports: this.scolarite.getSupports(this.coursId),
      synthese: this.communication.getSyntheseCours(this.coursId),
    }).subscribe({
      next: ({ cours, supports, synthese }) => {
        this.cours = cours;
        this.supports = supports;
        this.synthese = synthese;
        this.note = cours.maNote ?? 0;
        this.chargement = false;
      },
      error: (e) => {
        this.erreur =
          e?.error?.message || 'Ce cours est introuvable ou ne fait pas partie de votre formation.';
        this.chargement = false;
      },
    });
  }

  /** L'URL renvoyée par le serveur est relative à l'API. */
  lienAbsolu(url: string): string {
    if (url.startsWith('http')) {
      return url;
    }
    return environment.apiUrl.replace(/\/api$/, '') + url;
  }

  marquerConsulte(support: SupportEtudiant): void {
    if (support.consulte) {
      return;
    }
    this.scolarite.marquerConsulte(support.id).subscribe({
      next: (reponse) => {
        support.consulte = true;
        if (this.cours) {
          this.cours.progression = reponse.progression;
          this.cours.nbSupportsConsultes += 1;
        }
      },
      error: () => undefined,
    });
  }

  envoyerEvaluation(): void {
    if (this.note === 0) {
      return;
    }
    this.envoiEnCours = true;
    this.messageEvaluation = '';
    this.erreurEvaluation = '';
    this.communication.evaluerCours(this.coursId, this.note, this.commentaire).subscribe({
      next: () => {
        this.envoiEnCours = false;
        this.messageEvaluation = 'Merci, votre évaluation a été enregistrée.';
        if (this.cours) {
          this.cours.maNote = this.note;
        }
        this.communication.getSyntheseCours(this.coursId).subscribe({
          next: (s) => (this.synthese = s),
          error: () => undefined,
        });
      },
      error: (e) => {
        this.envoiEnCours = false;
        this.erreurEvaluation = e?.error?.message || "L'envoi de votre évaluation a échoué.";
      },
    });
  }
}
