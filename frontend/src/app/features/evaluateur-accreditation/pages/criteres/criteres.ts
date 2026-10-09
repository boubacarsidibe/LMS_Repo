import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CritereAccreditation } from '../../../accreditation/models/accreditation.models';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';

interface SectionReferentiel {
  nom: string;
  criteres: CritereAccreditation[];
  actifs: number;
}

/**
 * Référentiel d'accréditation de la DRCQ (Direction de la Régulation et du
 * Contrôle Qualité de l'INPG), version révisée du 23 mars 2026 : 31 conditions
 * codées, réparties en 4 sections. Les critères actifs alimentent la grille
 * d'évaluation de chaque dossier (Conforme / Non conforme / Sans objet).
 *
 * <p>Écran de lecture avant tout : chaque condition affiche son libellé, ses
 * lignes directrices dépliables, et peut être désactivée puis réactivée sans
 * perte.
 */
@Component({
  selector: 'app-evaluateur-criteres',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Direction de la Régulation et du Contrôle Qualité
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Référentiel d'accréditation</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Programmes de formation Oil &amp; Gas — version révisée du 23 mars 2026. Chaque condition
          active apparaît dans la grille d'évaluation d'un dossier, où l'évaluateur la juge Conforme,
          Non conforme ou Sans objet.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement du référentiel…"></app-ui-spinner>

      <ng-container *ngIf="!chargement">
        <!-- Barre de synthèse + actions -->
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg flex flex-wrap items-center justify-between gap-4">
          <div class="flex flex-wrap gap-x-6 gap-y-1 font-body-sm text-body-sm text-on-surface-variant">
            <span><strong class="text-on-surface tabular-nums">{{ criteres.length }}</strong> conditions</span>
            <span><strong class="text-on-surface tabular-nums">{{ sections.length }}</strong> sections</span>
            <span><strong class="text-success-strong tabular-nums">{{ nombreActifs }}</strong> actives</span>
            <span *ngIf="nombreActifs < criteres.length">
              <strong class="text-on-surface-variant tabular-nums">{{ criteres.length - nombreActifs }}</strong> désactivées
            </span>
          </div>
          <div class="flex items-center gap-2">
            <button
              type="button"
              (click)="basculerTout()"
              class="px-3 py-1.5 rounded-lg border border-outline-variant font-label-sm text-label-sm text-on-surface-variant hover:border-secondary hover:text-secondary transition-colors"
            >
              {{ toutEstOuvert ? 'Tout replier' : 'Tout déplier' }}
            </button>
            <button
              type="button"
              (click)="formulaireOuvert = !formulaireOuvert"
              class="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-secondary text-on-secondary font-label-sm text-label-sm hover:opacity-90 transition-opacity"
            >
              <span class="material-symbols-outlined text-[18px]">{{ formulaireOuvert ? 'close' : 'add' }}</span>
              {{ formulaireOuvert ? 'Fermer' : 'Ajouter une condition' }}
            </button>
          </div>
        </div>

        <!-- Formulaire d'ajout, replié par défaut -->
        <div *ngIf="formulaireOuvert"
             class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-3">
          <div class="grid sm:grid-cols-4 gap-3">
            <label class="block">
              <span class="font-label-sm text-label-sm text-on-surface-variant">Code</span>
              <input type="text" [(ngModel)]="nouveauCode" placeholder="1.13"
                class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
            </label>
            <label class="block sm:col-span-3">
              <span class="font-label-sm text-label-sm text-on-surface-variant">Section</span>
              <input type="text" [(ngModel)]="nouvelleSection" list="sections-referentiel" placeholder="1. Management de la qualité"
                class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
              <datalist id="sections-referentiel">
                <option *ngFor="let s of sections" [value]="s.nom"></option>
              </datalist>
            </label>
          </div>
          <label class="block">
            <span class="font-label-sm text-label-sm text-on-surface-variant">Libellé de la condition</span>
            <input type="text" [(ngModel)]="nouveauLibelle"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors" />
          </label>
          <label class="block">
            <span class="font-label-sm text-label-sm text-on-surface-variant">Lignes directrices (une par phrase)</span>
            <textarea [(ngModel)]="nouvelleDescription" rows="3"
              class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"></textarea>
          </label>
          <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
          <button type="button" (click)="creer()" [disabled]="!nouveauLibelle.trim() || creationEnCours"
            class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50">
            Ajouter la condition
          </button>
        </div>

        <!-- Sections : repliées par défaut, pour parcourir les grandes lignes du
             référentiel (4 sections) avant d'ouvrir celle qui concerne le dossier
             en cours plutôt que de dérouler les 31 conditions d'un coup. -->
        <section *ngFor="let s of sections" class="space-y-base">
          <button
            type="button"
            (click)="basculerSection(s.nom)"
            [attr.aria-expanded]="sectionEstOuverte(s.nom)"
            class="w-full flex items-center justify-between gap-3 border-b border-outline-variant pb-2 text-left cursor-pointer group"
          >
            <span class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[20px] text-primary transition-transform" [class.rotate-90]="sectionEstOuverte(s.nom)">
                chevron_right
              </span>
              <h2 class="font-headline-sm text-headline-sm text-primary">{{ s.nom }}</h2>
            </span>
            <span class="font-label-sm text-label-sm text-on-surface-variant tabular-nums shrink-0">
              {{ s.actifs }} / {{ s.criteres.length }} active(s)
            </span>
          </button>

          <ul *ngIf="sectionEstOuverte(s.nom)" class="space-y-3">
            <li
              *ngFor="let c of s.criteres"
              class="rounded-xl border shadow-sm transition-colors"
              [ngClass]="c.actif
                ? 'bg-surface-container-lowest border-outline-variant'
                : 'bg-surface-container-low border-outline-variant/60 opacity-70'"
            >
              <div class="p-stack-lg space-y-3">
                <div class="flex items-start gap-3">
                  <span
                    *ngIf="c.code"
                    class="shrink-0 mt-0.5 px-2 py-0.5 rounded-lg bg-secondary-container text-on-secondary-container font-label-sm text-label-sm tabular-nums"
                  >{{ c.code }}</span>
                  <p class="flex-1 font-label-md text-label-md text-on-surface leading-snug">{{ c.libelle }}</p>
                  <div class="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      (click)="basculer(c)"
                      class="p-2 rounded-full transition-colors"
                      [ngClass]="c.actif
                        ? 'text-on-surface-variant hover:bg-surface-container'
                        : 'text-success-strong hover:bg-success/10'"
                      [title]="c.actif ? 'Désactiver' : 'Réactiver'"
                      [attr.aria-label]="c.actif ? 'Désactiver' : 'Réactiver'"
                    >
                      <span class="material-symbols-outlined text-[20px]">
                        {{ c.actif ? 'toggle_on' : 'toggle_off' }}
                      </span>
                    </button>
                    <button
                      type="button"
                      (click)="supprimer(c)"
                      class="p-2 text-on-surface-variant hover:bg-error-container hover:text-on-error-container rounded-full transition-colors"
                      title="Supprimer"
                      aria-label="Supprimer"
                    >
                      <span class="material-symbols-outlined text-[20px]">delete</span>
                    </button>
                  </div>
                </div>

                <div class="flex items-center gap-2 pl-0.5">
                  <span
                    class="px-2 py-0.5 rounded-full font-label-sm text-label-sm"
                    [ngClass]="c.actif ? 'bg-success/15 text-success-strong' : 'bg-surface-container text-on-surface-variant'"
                  >{{ c.actif ? 'Active' : 'Désactivée' }}</span>
                  <span *ngIf="!c.actif" class="font-body-sm text-body-sm text-on-surface-variant">
                    absente de la grille des nouveaux dossiers
                  </span>
                </div>

                <details *ngIf="lignesDirectrices(c).length" class="group" [open]="detailsOuverts">
                  <summary class="cursor-pointer list-none flex items-center gap-1 font-label-sm text-label-sm text-secondary hover:opacity-80 select-none">
                    <span class="material-symbols-outlined text-[18px] transition-transform group-open:rotate-90">chevron_right</span>
                    Lignes directrices ({{ lignesDirectrices(c).length }})
                  </summary>
                  <ul class="mt-2 ml-1 space-y-1.5 border-l-2 border-outline-variant pl-3">
                    <li *ngFor="let ligne of lignesDirectrices(c)"
                        class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                      {{ ligne }}
                    </li>
                  </ul>
                </details>
              </div>
            </li>
          </ul>
        </section>
      </ng-container>
    </div>
  `,
})
export class EvaluateurCriteresComponent implements OnInit {
  private evaluateurService = inject(EvaluateurAccreditationService);
  private confirmation = inject(ConfirmationService);

  criteres: CritereAccreditation[] = [];
  chargement = true;
  formulaireOuvert = false;

  /** Sections dépliées, par nom — toutes repliées par défaut (voir commentaire au gabarit). */
  private sectionsOuvertes = new Set<string>();
  /** Lignes directrices des critères, dépliées ou non — indépendant des sections. */
  detailsOuverts = false;

  nouveauCode = '';
  nouvelleSection = '';
  nouveauLibelle = '';
  nouvelleDescription = '';
  creationEnCours = false;
  erreur = '';

  get nombreActifs(): number {
    return this.criteres.filter((c) => c.actif).length;
  }

  sectionEstOuverte(nom: string): boolean {
    return this.sectionsOuvertes.has(nom);
  }

  basculerSection(nom: string): void {
    if (this.sectionsOuvertes.has(nom)) {
      this.sectionsOuvertes.delete(nom);
    } else {
      this.sectionsOuvertes.add(nom);
    }
  }

  get toutEstOuvert(): boolean {
    return this.sections.length > 0 && this.sections.every((s) => this.sectionsOuvertes.has(s.nom));
  }

  basculerTout(): void {
    if (this.toutEstOuvert) {
      this.sectionsOuvertes.clear();
      this.detailsOuverts = false;
    } else {
      this.sections.forEach((s) => this.sectionsOuvertes.add(s.nom));
      this.detailsOuverts = true;
    }
  }

  /** Critères regroupés par section, dans l'ordre du référentiel. */
  get sections(): SectionReferentiel[] {
    const groupes = new Map<string, CritereAccreditation[]>();
    for (const c of this.criteres) {
      const nom = c.section?.trim() || 'Autres conditions';
      const liste = groupes.get(nom);
      if (liste) {
        liste.push(c);
      } else {
        groupes.set(nom, [c]);
      }
    }
    return [...groupes.entries()].map(([nom, criteres]) => ({
      nom,
      criteres,
      actifs: criteres.filter((c) => c.actif).length,
    }));
  }

  /** Découpe la description (phrases jointes) en lignes directrices lisibles. */
  lignesDirectrices(critere: CritereAccreditation): string[] {
    const texte = critere.description?.trim();
    if (!texte) {
      return [];
    }
    return texte
      .split(/(?<=[.;])\s+(?=[A-ZÀ-ÝÉÈÊ0-9])/)
      .map((l) => l.trim())
      .filter(Boolean);
  }

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement = true;
    this.evaluateurService.getCriteres().subscribe({
      next: (criteres) => {
        this.criteres = criteres;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  creer(): void {
    if (!this.nouveauLibelle.trim()) {
      return;
    }
    this.creationEnCours = true;
    this.erreur = '';
    this.evaluateurService
      .creerCritere(
        this.nouveauLibelle.trim(),
        this.nouvelleDescription.trim() || null,
        this.nouveauCode.trim() || null,
        this.nouvelleSection.trim() || null
      )
      .subscribe({
        next: (critere) => {
          this.creationEnCours = false;
          this.criteres = [...this.criteres, critere];
          // Sans ça, la section qui vient de recevoir la nouvelle condition resterait
          // repliée : rien ne montrerait que l'ajout a bien eu lieu.
          this.sectionsOuvertes.add(critere.section?.trim() || 'Autres conditions');
          this.nouveauCode = '';
          this.nouvelleSection = '';
          this.nouveauLibelle = '';
          this.nouvelleDescription = '';
          this.formulaireOuvert = false;
        },
        error: (e) => {
          this.creationEnCours = false;
          this.erreur = e?.error?.message || "La création de la condition a échoué.";
        },
      });
  }

  basculer(critere: CritereAccreditation): void {
    const appel = critere.actif
      ? this.evaluateurService.desactiverCritere(critere.id)
      : this.evaluateurService.reactiverCritere(critere.id);
    appel.subscribe({
      next: (mis) => (this.criteres = this.criteres.map((c) => (c.id === mis.id ? mis : c))),
    });
  }

  supprimer(critere: CritereAccreditation): void {
    this.confirmation.demander({
      titre: 'Supprimer la condition',
      message: `Supprimer définitivement la condition « ${critere.code || critere.libelle} » ?`,
      libelleConfirmer: 'Supprimer',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.evaluateurService.supprimerCritere(critere.id).subscribe({
        next: () => (this.criteres = this.criteres.filter((c) => c.id !== critere.id)),
      });
    });
  }
}
