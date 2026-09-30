import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ActiviteCours, ActiviteCreation, RemiseActivite } from '../../models/activite-cours';
import { Cours } from '../../models/cours';
import { Support } from '../../models/support';
import { ActiviteCoursService } from '../../services/activite-cours.service';
import { CoursService } from '../../services/cours.service';
import { SupportService } from '../../services/support.service';

/**
 * Fiche opérationnelle d'un cours attribué au formateur.
 * Les inscriptions et la structure restent pilotées par le responsable pédagogique.
 */
@Component({
  selector: 'app-cours-detail-formateur',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <main class="p-margin-desktop max-w-container-max mx-auto space-y-stack-lg">
      <a routerLink="/espace-etudiant/formateur/mes-cours"
         class="inline-flex items-center gap-2 font-label-md text-label-md text-on-surface-variant hover:text-secondary">
        <span class="material-symbols-outlined text-[18px]">arrow_back</span> Mes cours
      </a>

      <p *ngIf="chargement" class="font-body-md text-body-md text-on-surface-variant">Chargement du cours…</p>
      <section *ngIf="erreur" class="rounded-xl border border-error/30 bg-error-container/20 p-4 text-error font-body-md text-body-md">
        {{ erreur }}
      </section>

      <ng-container *ngIf="!chargement && cours">
        <header class="rounded-2xl bg-surface-container-lowest border border-outline-variant/40 p-stack-lg shadow-sm">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p class="font-label-md text-label-md text-secondary">{{ cours.code }} <span *ngIf="cours.semestre">· {{ cours.semestre }}</span></p>
              <h1 class="mt-1 font-display text-display text-on-surface leading-tight">{{ cours.titre }}</h1>
              <p *ngIf="cours.description" class="mt-3 max-w-3xl font-body-md text-body-md text-on-surface-variant">{{ cours.description }}</p>
            </div>
            <a [routerLink]="['/espace-etudiant/formateur/mes-cours']" [queryParams]="{ cours: cours.id }"
               class="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 font-label-md text-label-md text-on-primary hover:opacity-90">
              <span class="material-symbols-outlined text-[19px]">upload_file</span> Déposer une ressource
            </a>
          </div>

          <div class="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div class="rounded-xl bg-surface-container-low p-4">
              <p class="font-label-sm text-label-sm text-on-surface-variant">Étudiants inscrits</p>
              <p class="mt-1 font-headline-md text-headline-md text-on-surface tabular-nums">{{ cours.nbEtudiants }}</p>
            </div>
            <div class="rounded-xl bg-surface-container-low p-4">
              <p class="font-label-sm text-label-sm text-on-surface-variant">Progression moyenne</p>
              <p class="mt-1 font-headline-md text-headline-md text-on-surface tabular-nums">{{ cours.progression }} %</p>
            </div>
            <div class="rounded-xl bg-surface-container-low p-4">
              <p class="font-label-sm text-label-sm text-on-surface-variant">Volume prévu</p>
              <p class="mt-1 font-headline-md text-headline-md text-on-surface tabular-nums">{{ cours.volumeHoraire || 0 }} h</p>
            </div>
          </div>
        </header>

        <section class="grid grid-cols-1 gap-gutter lg:grid-cols-3">
          <div class="lg:col-span-2 rounded-2xl bg-surface-container-lowest border border-outline-variant/40 p-stack-lg shadow-sm">
            <div class="flex items-center justify-between gap-3">
              <div>
                <h2 class="font-headline-sm text-headline-sm text-on-surface">Ressources du cours</h2>
                <p class="mt-1 font-body-sm text-body-sm text-on-surface-variant">Les ressources programmées restent invisibles pour les étudiants jusqu'à leur date de publication.</p>
              </div>
              <span class="rounded-full bg-surface-container px-3 py-1 font-label-sm text-label-sm text-on-surface-variant">{{ supports.length }}</span>
            </div>
            <p *ngIf="supports.length === 0" class="mt-6 rounded-xl bg-surface-container-low p-4 font-body-md text-body-md text-on-surface-variant">Aucune ressource n'est encore déposée.</p>
            <ul *ngIf="supports.length" class="mt-5 space-y-3">
              <li *ngFor="let support of supports" class="flex items-center gap-3 rounded-xl border border-outline-variant/35 p-4">
                <span class="material-symbols-outlined text-secondary">{{ icone(support.typeRessource) }}</span>
                <div class="min-w-0 flex-1">
                  <p class="truncate font-label-md text-label-md text-on-surface">{{ support.titre || 'Ressource sans titre' }}</p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant">
                    {{ support.programme ? 'Programmée' : 'Publiée' }}<span *ngIf="support.datePublication"> · {{ support.datePublication | date: 'd MMM y' }}</span>
                  </p>
                </div>
                <span class="material-symbols-outlined text-outline">{{ support.programme ? 'schedule' : 'visibility' }}</span>
              </li>
            </ul>
          </div>

          <aside class="space-y-4">
            <section class="rounded-2xl bg-surface-container-lowest border border-outline-variant/40 p-stack-lg shadow-sm">
              <h2 class="font-headline-sm text-headline-sm text-on-surface">Promotions concernées</h2>
              <p *ngIf="!cours.promotions.length" class="mt-3 font-body-sm text-body-sm text-on-surface-variant">Aucune promotion n'est indiquée.</p>
              <ul *ngIf="cours.promotions.length" class="mt-3 space-y-2">
                <li *ngFor="let promotion of cours.promotions" class="rounded-lg bg-surface-container-low px-3 py-2 font-body-sm text-body-sm text-on-surface">{{ promotion.libelle || promotion.code || 'Promotion' }}</li>
              </ul>
              <p class="mt-4 font-body-sm text-body-sm text-on-surface-variant">Les affectations sont gérées par le responsable pédagogique.</p>
            </section>
            <a routerLink="/espace-etudiant/formateur/suivi-etudiants" class="flex items-center justify-between rounded-2xl border border-outline-variant/40 bg-surface-container-lowest p-4 font-label-md text-label-md text-on-surface hover:border-secondary hover:text-secondary">
              Suivre les étudiants <span class="material-symbols-outlined">arrow_forward</span>
            </a>
          </aside>
        </section>

        <section class="rounded-2xl bg-surface-container-lowest border border-outline-variant/40 p-stack-lg shadow-sm">
          <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 class="font-headline-sm text-headline-sm text-on-surface">Modules, devoirs et évaluations</h2>
              <p class="mt-1 font-body-sm text-body-sm text-on-surface-variant">Chaque évaluation est notée sur 100 et affiche son équivalent sur 20 aux étudiants.</p>
            </div>
            <span class="rounded-full bg-surface-container px-3 py-1 font-label-sm text-label-sm text-on-surface-variant">{{ activites.length }} activité(s)</span>
          </div>

          <div *ngIf="activites.length" class="mt-5 grid grid-cols-1 gap-3 lg:grid-cols-2">
            <article *ngFor="let activite of activites" class="rounded-xl border border-outline-variant/35 p-4">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="font-label-sm text-label-sm text-secondary">{{ activite.module }} · {{ activite.type === 'DEVOIR' ? 'Devoir' : 'Évaluation' }}</p>
                  <h3 class="mt-1 font-label-md text-label-md text-on-surface">{{ activite.titre }}</h3>
                </div>
                <span class="rounded-full bg-surface-container px-2.5 py-1 font-label-sm text-label-sm text-on-surface-variant">{{ activite.modalite === 'EN_LIGNE' ? 'En ligne' : 'Présentiel' }}</span>
              </div>
              <p *ngIf="activite.description" class="mt-2 font-body-sm text-body-sm text-on-surface-variant">{{ activite.description }}</p>
              <p class="mt-3 font-body-sm text-body-sm text-on-surface-variant">
                {{ activite.modalite === 'PRESENTIEL' ? 'Prévue le' : 'Publication' }} : {{ activite.datePublication | date: 'd MMM y, HH:mm' }}
                <span *ngIf="activite.modalite === 'EN_LIGNE' && activite.dateLimite"> · Limite : {{ activite.dateLimite | date: 'd MMM y, HH:mm' }}</span>
              </p>
              <button *ngIf="activite.modalite === 'EN_LIGNE'" type="button" (click)="ouvrirCorrections(activite)" class="mt-4 inline-flex items-center gap-2 rounded-lg border border-outline-variant px-3 py-2 font-label-md text-label-md text-secondary hover:bg-secondary-fixed">
                <span class="material-symbols-outlined text-[18px]">fact_check</span> Voir les remises
              </button>
            </article>
          </div>
          <p *ngIf="!activites.length" class="mt-5 rounded-xl bg-surface-container-low p-4 font-body-md text-body-md text-on-surface-variant">Aucune activité n'est encore créée pour ce cours.</p>

          <section *ngIf="activitePourCorrection" class="mt-6 border-t border-outline-variant/35 pt-6">
            <div class="flex items-start justify-between gap-3"><div><h3 class="font-headline-sm text-headline-sm text-on-surface">Remises — {{ activitePourCorrection.titre }}</h3><p class="mt-1 font-body-sm text-body-sm text-on-surface-variant">Barème : {{ activitePourCorrection.bareme }} points.</p></div><button type="button" (click)="fermerCorrections()" class="p-1 text-on-surface-variant hover:text-secondary" title="Fermer"><span class="material-symbols-outlined">close</span></button></div>
            <p *ngIf="chargementRemises" class="mt-4 font-body-sm text-body-sm text-on-surface-variant">Chargement des remises…</p>
            <p *ngIf="erreurRemises" class="mt-4 rounded-lg bg-error-container/20 p-3 font-body-sm text-error">{{ erreurRemises }}</p>
            <p *ngIf="!chargementRemises && !erreurRemises && remises.length === 0" class="mt-4 rounded-lg bg-surface-container-low p-4 font-body-sm text-body-sm text-on-surface-variant">Aucune remise n’a encore été déposée.</p>
            <div *ngFor="let remise of remises" class="mt-4 rounded-xl border border-outline-variant/35 p-4"><div class="flex justify-between gap-3"><div><p class="font-label-md text-label-md text-on-surface">{{ remise.etudiant }}</p><p class="font-body-sm text-body-sm text-on-surface-variant">Remis le {{ remise.remiseLe | date:'d MMM y, HH:mm' }}</p></div><span *ngIf="remise.note != null" class="rounded-full bg-secondary-container px-3 py-1 font-label-sm text-label-sm text-on-secondary-container">{{ remise.note }}/{{ activitePourCorrection.bareme }}</span></div><p class="mt-3 whitespace-pre-wrap font-body-sm text-body-sm text-on-surface">{{ remise.contenu }}</p><div class="mt-4 grid gap-3 sm:grid-cols-[120px_1fr_auto]"><input type="number" min="0" [max]="activitePourCorrection.bareme" [(ngModel)]="remise.note" [name]="'note-' + remise.id" class="rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2" placeholder="Note"/><input [(ngModel)]="remise.commentaireCorrection" [name]="'commentaire-' + remise.id" class="rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2" placeholder="Commentaire de correction"/><button type="button" [disabled]="correctionEnCoursId === remise.id" (click)="corrigerRemise(remise)" class="rounded-lg bg-secondary px-4 py-2 font-label-md text-label-md text-on-secondary disabled:opacity-50">{{ correctionEnCoursId === remise.id ? 'Enregistrement…' : 'Enregistrer' }}</button></div></div>
          </section>
          <form #form="ngForm" (ngSubmit)="creerActivite()" class="mt-6 grid grid-cols-1 gap-4 border-t border-outline-variant/35 pt-6 md:grid-cols-2">
            <label class="block font-label-md text-label-md text-on-surface-variant">Module
              <input required name="module" [(ngModel)]="nouvelleActivite.module" class="mt-2 w-full rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-on-surface" placeholder="Ex. Modèle supervisé" />
            </label>
            <label class="block font-label-md text-label-md text-on-surface-variant">Titre
              <input required name="titre" [(ngModel)]="nouvelleActivite.titre" class="mt-2 w-full rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-on-surface" placeholder="Ex. Étude de cas 1" />
            </label>
            <label class="block font-label-md text-label-md text-on-surface-variant">Type
              <select name="type" [(ngModel)]="nouvelleActivite.type" class="mt-2 w-full rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-on-surface"><option value="DEVOIR">Devoir — travail à remettre</option><option value="EVALUATION">Évaluation — contrôle noté</option></select>
              <span class="mt-1 block font-body-sm text-body-sm text-on-surface-variant">{{ nouvelleActivite.type === 'DEVOIR' ? 'Un travail est remis par l’étudiant lorsqu’il est proposé en ligne.' : 'Un contrôle mesure les acquis ; il peut se dérouler en ligne ou en présentiel.' }}</span>
            </label>
            <label class="block font-label-md text-label-md text-on-surface-variant">Modalité
              <select name="modalite" [(ngModel)]="nouvelleActivite.modalite" (ngModelChange)="mettreAJourModalite($event)" class="mt-2 w-full rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-on-surface"><option value="EN_LIGNE">En ligne</option><option value="PRESENTIEL">Présentiel</option></select>
              <span class="mt-1 block font-body-sm text-body-sm text-on-surface-variant">{{ nouvelleActivite.modalite === 'PRESENTIEL' ? 'Aucune remise en ligne ni date limite : les étudiants la réalisent sur place.' : 'Les étudiants peuvent remettre leur travail depuis leur espace.' }}</span>
            </label>
            <label class="block font-label-md text-label-md text-on-surface-variant">{{ nouvelleActivite.modalite === 'PRESENTIEL' ? 'Date et heure de l’activité' : 'Publication' }}
              <input required type="datetime-local" name="publication" [(ngModel)]="nouvelleActivite.datePublication" class="mt-2 w-full rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-on-surface" />
            </label>
            <label *ngIf="nouvelleActivite.modalite === 'EN_LIGNE'" class="block font-label-md text-label-md text-on-surface-variant">Date limite (facultative)
              <input type="datetime-local" name="limite" [(ngModel)]="nouvelleActivite.dateLimite" class="mt-2 w-full rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-on-surface" />
            </label>
            <label class="block md:col-span-2 font-label-md text-label-md text-on-surface-variant">Consigne
              <textarea name="description" [(ngModel)]="nouvelleActivite.description" rows="3" class="mt-2 w-full rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-on-surface" placeholder="Consigne visible par les étudiants"></textarea>
            </label>
            <div class="md:col-span-2 flex items-center gap-3">
              <button type="submit" [disabled]="form.invalid || creationActivite" class="rounded-lg bg-primary px-5 py-2.5 font-label-md text-label-md text-on-primary disabled:opacity-50">{{ creationActivite ? 'Création…' : 'Créer l’activité' }}</button>
              <p *ngIf="erreurActivite" class="font-body-sm text-body-sm text-error">{{ erreurActivite }}</p>
            </div>
          </form>
        </section>
      </ng-container>
    </main>
  `,
})
export class CoursDetailFormateur implements OnInit {
  cours: Cours | null = null;
  supports: Support[] = [];
  chargement = true;
  erreur = '';
  activites: ActiviteCours[] = [];
  creationActivite = false;
  erreurActivite = '';
  activitePourCorrection: ActiviteCours | null = null;
  remises: RemiseActivite[] = [];
  chargementRemises = false;
  erreurRemises = '';
  correctionEnCoursId: number | null = null;
  nouvelleActivite: ActiviteCreation = this.activiteVierge();

  constructor(private route: ActivatedRoute, private coursService: CoursService, private supportService: SupportService,
    private activiteService: ActiviteCoursService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.erreur = 'Ce cours est introuvable.';
      this.chargement = false;
      return;
    }
    forkJoin({ cours: this.coursService.getById(id), supports: this.supportService.getMesSupports(), activites: this.activiteService.getFormateur(id) }).subscribe({
      next: ({ cours, supports, activites }) => {
        this.cours = cours;
        this.supports = (supports || []).filter((support) => Number(support.coursId) === id);
        this.activites = activites || [];
        this.chargement = false;
      },
      error: (error) => {
        this.erreur = error?.error?.message || 'Ce cours est introuvable ou ne vous est plus attribué.';
        this.chargement = false;
      },
    });
  }

  icone(type?: string | null): string {
    return type === 'lien' ? 'link' : type === 'video' ? 'videocam' : 'description';
  }

  ouvrirCorrections(activite: ActiviteCours): void {
    this.activitePourCorrection = activite;
    this.remises = [];
    this.erreurRemises = '';
    this.chargementRemises = true;
    this.activiteService.getRemises(activite.id).subscribe({
      next: (remises) => { this.remises = remises || []; this.chargementRemises = false; },
      error: (error) => { this.erreurRemises = error?.error?.message || 'Impossible de charger les remises.'; this.chargementRemises = false; },
    });
  }

  fermerCorrections(): void {
    this.activitePourCorrection = null;
    this.remises = [];
  }

  corrigerRemise(remise: RemiseActivite): void {
    const bareme = this.activitePourCorrection?.bareme ?? 100;
    if (remise.note == null || remise.note < 0 || remise.note > bareme) {
      this.erreurRemises = `La note doit être comprise entre 0 et ${bareme}.`;
      return;
    }
    this.correctionEnCoursId = remise.id;
    this.erreurRemises = '';
    this.activiteService.corriger(remise.id, remise.note, remise.commentaireCorrection || '').subscribe({
      next: (maj) => { Object.assign(remise, maj); this.correctionEnCoursId = null; },
      error: (error) => { this.erreurRemises = error?.error?.message || 'La correction n’a pas pu être enregistrée.'; this.correctionEnCoursId = null; },
    });
  }
  creerActivite(): void {
    if (!this.cours || this.creationActivite) return;
    this.creationActivite = true;
    this.erreurActivite = '';
    const demande: ActiviteCreation = {
      ...this.nouvelleActivite,
      dateLimite: this.nouvelleActivite.modalite === 'EN_LIGNE' ? this.nouvelleActivite.dateLimite || null : null,
    };
    this.activiteService.creer(this.cours.id, demande).subscribe({
      next: (activite) => {
        this.activites = [activite, ...this.activites];
        this.nouvelleActivite = this.activiteVierge();
        this.creationActivite = false;
      },
      error: (error) => {
        this.erreurActivite = error?.error?.message || 'La création de l’activité a échoué.';
        this.creationActivite = false;
      },
    });
  }

  mettreAJourModalite(modalite: ActiviteCreation['modalite']): void {
    this.nouvelleActivite.modalite = modalite;
    if (modalite === 'PRESENTIEL') {
      this.nouvelleActivite.dateLimite = null;
    }
  }

  private activiteVierge(): ActiviteCreation {
    return { module: '', titre: '', description: '', type: 'DEVOIR', modalite: 'EN_LIGNE', datePublication: new Date().toISOString().slice(0, 16), dateLimite: null, bareme: 100 };
  }
}
