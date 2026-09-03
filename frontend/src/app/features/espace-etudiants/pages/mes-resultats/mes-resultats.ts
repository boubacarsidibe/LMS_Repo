import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { Certificat } from '../../models/certificat';
import { NotesCours, ReleveDelivre, ReleveNotes } from '../../models/scolarite.models';
import { CertificatService } from '../../services/certificat.service';
import { ScolariteService } from '../../services/scolarite.service';

/**
 * Résultats de l'étudiant : ses notes par cours et ses certificats.
 *
 * <p>Les deux répondent à la même question — « qu'ai-je obtenu ? » — d'où leur
 * réunion sur un seul écran plutôt que deux entrées de navigation.
 *
 * <p>Les notes existaient en base depuis l'origine sans qu'aucun écran ne les
 * montre à l'étudiant : elles n'étaient lisibles que par le formateur. Les
 * certificats, eux, avaient bien une page, mais aucun lien n'y menait.
 */
@Component({
  selector: 'app-mes-resultats',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <h1 class="font-display text-display text-on-surface leading-tight">Mes résultats</h1>
        <p class="font-body-md text-body-md text-on-surface-variant mt-1">
          Vos notes par cours et les certificats qui vous ont été délivrés.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement de vos résultats…"></app-ui-spinner>

      <ng-container *ngIf="!chargement">
        <!-- Synthèse -->
        <section class="grid grid-cols-1 sm:grid-cols-3 gap-gutter">
          <article
            class="bg-surface-container-lowest rounded-xl p-stack-lg border border-outline-variant shadow-sm"
          >
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined text-[20px]">grade</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">Moyenne générale</span>
            </div>
            <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">
              {{ releve?.moyenneGenerale !== null && releve?.moyenneGenerale !== undefined
                  ? (releve!.moyenneGenerale | number: '1.1-1') + ' / 20'
                  : '—' }}
            </p>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-2">
              Pondérée par le coefficient de chaque cours, et non par le nombre de devoirs.
            </p>
          </article>
          <article
            class="bg-surface-container-lowest rounded-xl p-stack-lg border border-outline-variant shadow-sm"
          >
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined text-[20px]">fact_check</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">Notes reçues</span>
            </div>
            <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">
              {{ releve?.nombreNotes ?? 0 }}
            </p>
          </article>
          <article
            class="bg-surface-container-lowest rounded-xl p-stack-lg border border-outline-variant shadow-sm"
          >
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined text-[20px]">workspace_premium</span>
              <span class="font-label-sm text-label-sm uppercase tracking-wider">Certificats</span>
            </div>
            <p class="font-headline-lg text-headline-lg text-on-surface mt-2 tabular-nums">
              {{ certificats.length }}
            </p>
          </article>
        </section>

        <!-- Notes par cours -->
        <section class="space-y-stack-md">
          <h2 class="font-headline-sm text-headline-sm text-on-surface">Notes par cours</h2>

          <app-ui-empty-state
            *ngIf="parCours.length === 0"
            icone="fact_check"
            titre="Aucune note pour l’instant"
            description="Vos formateurs n’ont pas encore saisi de note. Elles apparaîtront ici dès la première évaluation."
          ></app-ui-empty-state>

          <article
            *ngFor="let cours of parCours"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm"
          >
            <header
              class="flex items-center justify-between gap-4 px-stack-lg py-stack-md border-b border-outline-variant"
            >
              <div class="min-w-0">
                <p class="font-label-sm text-label-sm text-on-surface-variant">
                  {{ cours.coursCode }}
                  <span class="text-on-surface-variant">· coefficient {{ cours.coefficient }}</span>
                </p>
                <a
                  [routerLink]="['/espace-etudiant/mes-cours', cours.coursId]"
                  class="font-label-md text-label-md text-on-surface hover:underline"
                  >{{ cours.coursTitre }}</a
                >
              </div>
              <div class="text-right shrink-0">
                <p class="font-label-sm text-label-sm text-on-surface-variant">Moyenne</p>
                <p class="font-headline-sm text-headline-sm text-secondary tabular-nums">
                  {{ cours.moyenne !== null ? (cours.moyenne | number: '1.1-1') : '—' }}
                </p>
              </div>
            </header>

            <ul class="divide-y divide-outline-variant">
              <li
                *ngFor="let note of cours.notes"
                class="px-stack-lg py-stack-md flex items-start justify-between gap-4"
              >
                <div class="min-w-0">
                  <p class="font-label-md text-label-md text-on-surface">{{ note.module }}</p>
                  <p
                    *ngIf="note.commentaire"
                    class="font-body-sm text-body-sm text-on-surface-variant mt-1"
                  >
                    {{ note.commentaire }}
                  </p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                    {{ note.dateEvaluation || 'Date non précisée' }}
                    <ng-container *ngIf="note.formateurNom"> · {{ note.formateurNom }}</ng-container>
                  </p>
                </div>
                <p
                  class="font-headline-sm text-headline-sm tabular-nums shrink-0"
                  [class.text-error]="note.note < 10"
                  [class.text-on-surface]="note.note >= 10"
                >
                  {{ note.note | number: '1.0-2' }}<span
                    class="font-body-sm text-body-sm text-on-surface-variant"
                    >/20</span
                  >
                </p>
              </li>
            </ul>
          </article>
        </section>

        <!-- Relevés officiels délivrés -->
        <section class="space-y-stack-md" *ngIf="releves.length > 0">
          <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-secondary">description</span>
            Relevés officiels
          </h2>
          <p class="font-body-sm text-body-sm text-on-surface-variant">
            Délivrés par la direction pédagogique. Leurs chiffres sont figés à la date
            d’émission : ils peuvent donc différer de vos notes en cours ci-dessus.
          </p>

          <article *ngFor="let r of releves"
                   class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
            <div class="flex items-start justify-between gap-4 flex-wrap">
              <div class="min-w-0">
                <p class="font-label-md text-label-md text-on-surface">{{ r.periode }}</p>
                <p class="font-body-sm text-body-sm text-on-surface-variant">
                  {{ r.reference }} · délivré le {{ r.dateEmission | date: 'dd/MM/yyyy' }}
                </p>
                <p class="font-body-sm text-body-sm text-on-surface-variant">{{ r.delivrePar }}</p>
              </div>
              <div class="flex items-center gap-3 shrink-0">
                <p class="font-headline-sm text-headline-sm text-secondary tabular-nums">
                  {{ r.moyenneGenerale !== null ? (r.moyenneGenerale | number: '1.1-1') : '—' }}<span
                    class="font-body-sm text-body-sm text-on-surface-variant">/20</span>
                </p>
                <button type="button" (click)="telechargerReleve(r)" [disabled]="releveEnCoursId === r.id"
                        class="flex items-center gap-1 px-3 py-2 rounded-lg border border-outline-variant font-label-sm text-label-sm text-on-surface hover:bg-surface-container transition-colors disabled:opacity-50">
                  <span class="material-symbols-outlined text-[18px]">download</span>
                  {{ releveEnCoursId === r.id ? 'Téléchargement…' : 'PDF' }}
                </button>
              </div>
            </div>

            <ul class="mt-4 border-t border-outline-variant/50 pt-3 space-y-1">
              <li *ngFor="let l of r.lignes"
                  class="flex items-center justify-between gap-4 font-body-sm text-body-sm">
                <span class="text-on-surface min-w-0 truncate">
                  {{ l.coursCode }} — {{ l.coursTitre }}
                  <span class="text-on-surface-variant">(coef. {{ l.coefficient }})</span>
                </span>
                <span class="text-on-surface tabular-nums shrink-0">
                  {{ l.moyenne !== null ? (l.moyenne | number: '1.1-1') : '—' }}
                </span>
              </li>
            </ul>
          </article>
        </section>

        <!-- Certificats -->
        <section class="space-y-stack-md">
          <h2 class="font-headline-sm text-headline-sm text-on-surface">Mes certificats</h2>

          <app-ui-empty-state
            *ngIf="certificats.length === 0"
            icone="workspace_premium"
            titre="Aucun certificat"
            description="Les attestations délivrées à l’issue de vos formations seront téléchargeables ici."
          ></app-ui-empty-state>

          <ul *ngIf="certificats.length > 0" class="grid grid-cols-1 lg:grid-cols-2 gap-gutter">
            <li
              *ngFor="let certificat of certificats"
              class="bg-surface-container-lowest rounded-xl p-stack-lg border border-outline-variant flex items-start justify-between gap-4 shadow-sm"
            >
              <div class="min-w-0">
                <p class="font-label-md text-label-md text-on-surface">
                  {{ certificat.intituleFormation }}
                </p>
                <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                  Délivré le {{ certificat.dateEmission }} · {{ certificat.formateurNom }}
                </p>
                <p class="font-body-sm text-body-sm text-on-surface-variant tabular-nums">
                  Réf. {{ certificat.reference }}
                </p>
              </div>
              <button
                type="button"
                (click)="telecharger(certificat)"
                [disabled]="telechargementEnCoursId === certificat.id"
                class="shrink-0 flex items-center gap-2 px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 disabled:opacity-50 transition-opacity"
              >
                <span class="material-symbols-outlined text-[18px]">download</span>
                {{ telechargementEnCoursId === certificat.id ? 'Téléchargement…' : 'PDF' }}
              </button>
            </li>
          </ul>

          <p *ngIf="erreurTelechargement" class="font-body-sm text-body-sm text-error">
            {{ erreurTelechargement }}
          </p>
        </section>
      </ng-container>
    </div>
  `,
})
export class MesResultats implements OnInit {
  chargement = true;
  releve: ReleveNotes | null = null;
  releves: ReleveDelivre[] = [];
  certificats: Certificat[] = [];
  telechargementEnCoursId: number | null = null;
  releveEnCoursId: number | null = null;
  erreurTelechargement: string | null = null;

  constructor(
    private scolarite: ScolariteService,
    private certificatService: CertificatService
  ) {}

  ngOnInit(): void {
    forkJoin({
      releve: this.scolarite.getMesNotes(),
      releves: this.scolarite.getMesReleves(),
      certificats: this.certificatService.getAll(),
    }).subscribe({
      next: ({ releve, releves, certificats }) => {
        this.releve = releve;
        this.releves = releves || [];
        this.certificats = certificats ?? [];
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  /** Cours possédant au moins une note : un cours sans note n'a rien à montrer ici. */
  get parCours(): NotesCours[] {
    return (this.releve?.parCours ?? []).filter((cours) => cours.notes.length > 0);
  }

  telecharger(certificat: Certificat): void {
    this.telechargementEnCoursId = certificat.id;
    this.erreurTelechargement = null;
    this.certificatService.telechargerPdf(certificat.id).subscribe({
      next: (blob) => {
        this.telechargementEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `INPG-certificat-${certificat.reference}.pdf`;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementEnCoursId = null;
        this.erreurTelechargement = "Le certificat n'a pas pu être téléchargé. Réessayez dans un instant.";
      },
    });
  }

  telechargerReleve(releve: ReleveDelivre): void {
    this.releveEnCoursId = releve.id;
    this.erreurTelechargement = null;
    this.scolarite.telechargerRelevePdf(releve.id).subscribe({
      next: (blob) => {
        this.releveEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `INPG-releve-${releve.reference}.pdf`;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.releveEnCoursId = null;
        this.erreurTelechargement = "Le relevé n'a pas pu être téléchargé. Réessayez dans un instant.";
      },
    });
  }
}
