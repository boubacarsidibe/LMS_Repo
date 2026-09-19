import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DemandeAccreditationEvaluateur, LIBELLES_STATUT_DEMANDE } from '../../../accreditation/models/accreditation.models';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { Auth } from '../../../../core/auth/services/authService';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';

interface ConformiteSection {
  nom: string;
  conformes: number;
  nonConformes: number;
  sansObjet: number;
  /** Sur les critères jugés CONFORME ou NON_CONFORME ; SANS_OBJET n'entre pas dans le taux. */
  tauxConformite: number;
}

/**
 * Aucune page d'accueil n'existait pour l'espace évaluateur (la route racine
 * redirigeait directement vers « Demandes ») : rien ne signalait, en un coup
 * d'œil, combien de dossiers attendaient un avis, ni où se concentraient les
 * non-conformités d'une section à l'autre du référentiel — chaque dossier
 * n'affiche que sa propre grille, jamais une vue agrégée.
 *
 * <p>La conformité par section porte volontairement sur `maGrille` (les
 * jugements de l'évaluateur connecté), pas sur l'ensemble des avis
 * collégiaux : eux seuls sont accessibles sans exposer le détail des
 * jugements d'un autre évaluateur avant la décision finale (voir le
 * commentaire de {@link DemandeAccreditationEvaluateur} et la distinction
 * déjà faite par PedagogieController vs. l'espace école pour un principe
 * similaire).
 */
@Component({
  selector: 'app-evaluateur-tableau-de-bord',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Direction de la Régulation et du Contrôle Qualité
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Tableau de bord</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Les dossiers qui attendent votre avis, et la conformité par section du référentiel sur
          les dossiers que vous avez déjà instruits.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement…"></app-ui-spinner>

      <ng-container *ngIf="!chargement">
        <div class="grid sm:grid-cols-3 gap-4">
          <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest">Dossiers actifs</p>
            <p class="font-display text-display text-on-surface mt-2">{{ dossiersActifs.length }}</p>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">soumis ou en cours d'évaluation</p>
          </div>
          <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg"
               [class.border-l-4]="dossiersEnAttenteDeMonAvis.length > 0"
               [class.border-l-error]="dossiersEnAttenteDeMonAvis.length > 0">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest">En attente de mon avis</p>
            <p class="font-display text-display text-on-surface mt-2">{{ dossiersEnAttenteDeMonAvis.length }}</p>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">sur les dossiers actifs</p>
          </div>
          <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest">Dossiers déjà instruits</p>
            <p class="font-display text-display text-on-surface mt-2">{{ nombreDossiersJuges }}</p>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">au moins un critère jugé par vous</p>
          </div>
        </div>

        <section *ngIf="dossiersEnAttenteDeMonAvis.length" class="space-y-base">
          <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-secondary">fact_check</span>
            Dossiers en attente de votre avis
          </h2>
          <ul class="space-y-2">
            <li *ngFor="let d of dossiersEnAttenteDeMonAvis">
              <a [routerLink]="['/evaluateur-accreditation/demandes']" [queryParams]="{ id: d.id }"
                 class="flex items-center justify-between gap-3 bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-md hover:border-secondary transition-colors no-underline">
                <div class="min-w-0">
                  <p class="font-label-md text-label-md text-on-surface truncate">{{ d.nomEcole }}</p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant truncate">{{ d.intituleProgramme || 'Programme non précisé' }}</p>
                </div>
                <span class="px-2 py-0.5 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm shrink-0">
                  {{ libellesStatut[d.statut] }}
                </span>
              </a>
            </li>
          </ul>
        </section>

        <section class="space-y-base">
          <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-secondary">insights</span>
            Conformité par section, sur vos dossiers instruits
          </h2>
          <p *ngIf="!conformiteParSection.length" class="font-body-sm text-body-sm text-on-surface-variant">
            Aucun critère jugé pour l'instant : cette synthèse se remplit au fil de vos instructions.
          </p>
          <div *ngIf="conformiteParSection.length" class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm divide-y divide-outline-variant">
            <div *ngFor="let s of conformiteParSection" class="p-stack-lg space-y-2">
              <div class="flex items-center justify-between gap-3">
                <p class="font-label-md text-label-md text-on-surface">{{ s.nom }}</p>
                <span class="font-label-sm text-label-sm text-on-surface-variant tabular-nums">
                  {{ s.conformes }} conforme(s) · {{ s.nonConformes }} non conforme(s)<ng-container *ngIf="s.sansObjet"> · {{ s.sansObjet }} sans objet</ng-container>
                </span>
              </div>
              <div class="h-2 rounded-full bg-surface-container overflow-hidden">
                <div class="h-full bg-success" [style.width.%]="s.tauxConformite"></div>
              </div>
            </div>
          </div>
        </section>
      </ng-container>
    </div>
  `,
})
export class EvaluateurTableauDeBordComponent implements OnInit {
  private evaluateurService = inject(EvaluateurAccreditationService);
  private auth = inject(Auth);

  demandes: DemandeAccreditationEvaluateur[] = [];
  chargement = true;

  /**
   * Getter plutôt que champ : `libellesStatut = LIBELLES_STATUT_DEMANDE;`
   * s'initialisait à `undefined` dans ce fichier précis (constaté par un
   * garde-fou de test — 3 échecs sur ce composant, jamais en production où
   * la construction du composant emprunte un autre chemin de compilation).
   * `demandes`/`chargement` juste au-dessus, initialisés par un littéral, ne
   * sont pas touchés ; seule la valeur qui référence un import s'est
   * retrouvée non affectée, cause non identifiée avec certitude (suspect :
   * l'énorme template en ligne du même fichier). Un getter contourne le
   * problème en calculant la valeur à l'usage plutôt qu'à la construction.
   */
  get libellesStatut(): Record<string, string> {
    return LIBELLES_STATUT_DEMANDE;
  }

  ngOnInit(): void {
    this.evaluateurService.getDemandes(undefined).subscribe({
      next: (demandes) => {
        this.demandes = demandes;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  get dossiersActifs(): DemandeAccreditationEvaluateur[] {
    return this.demandes.filter((d) => d.statut === 'SOUMISE' || d.statut === 'EN_COURS_EVALUATION');
  }

  get dossiersEnAttenteDeMonAvis(): DemandeAccreditationEvaluateur[] {
    const monId = this.auth.currentUser()?.id;
    return this.dossiersActifs.filter((d) => !d.avis.some((a) => a.evaluateurId === monId));
  }

  get nombreDossiersJuges(): number {
    return this.demandes.filter((d) => d.maGrille.length > 0).length;
  }

  get conformiteParSection(): ConformiteSection[] {
    const parSection = new Map<string, { conformes: number; nonConformes: number; sansObjet: number }>();
    for (const demande of this.demandes) {
      const sectionParCritere = new Map(
        demande.criteresActifs.map((c) => [c.id, c.section?.trim() || 'Autres conditions'])
      );
      for (const evaluation of demande.maGrille) {
        const nom = sectionParCritere.get(evaluation.critereId) || 'Autres conditions';
        const compteurs = parSection.get(nom) ?? { conformes: 0, nonConformes: 0, sansObjet: 0 };
        if (evaluation.appreciation === 'CONFORME') compteurs.conformes++;
        else if (evaluation.appreciation === 'NON_CONFORME') compteurs.nonConformes++;
        else compteurs.sansObjet++;
        parSection.set(nom, compteurs);
      }
    }
    return [...parSection.entries()]
      .map(([nom, c]) => {
        const jugesAvecObjet = c.conformes + c.nonConformes;
        return {
          nom,
          ...c,
          tauxConformite: jugesAvecObjet > 0 ? Math.round((c.conformes / jugesAvecObjet) * 100) : 0,
        };
      })
      .sort((a, b) => a.nom.localeCompare(b.nom));
  }
}
