import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { Auth } from '../../../../core/auth/services/authService';
import { EcoleAccreditee, LIBELLES_NIVEAU_PROGRAMME } from '../../models/accreditation.models';
import { AccreditationService } from '../../services/accreditation.service';
import { LogoInpgComponent } from '../../../../shared/components/logo-inpg/logo-inpg.component';

/**
 * Liste des écoles accréditées (TDR §F : « processus transparent »).
 * Réservée aux comptes connectés (`authGuard`) — décision produit du
 * 2026-08-25, voir `LOG_BLOQUANT.md`.
 *
 * <p>Accessible depuis l'accueil public à des comptes de n'importe quel rôle
 * (école, entreprise, étudiant, candidat…) : pas de layout de rôle unique à
 * utiliser ici. Portait donc jusqu'ici sa propre page nue, sans le moindre
 * moyen de revenir à son espace — un utilisateur connecté qui cliquait ce
 * lien depuis l'accueil se retrouvait dans une impasse de navigation. Le
 * bandeau ci-dessous (logo + retour à l'espace du rôle courant) comble ce
 * trou sans dupliquer la barre de navigation propre à chaque rôle.
 */
@Component({
  selector: 'app-ecoles-accreditees',
  standalone: true,
  imports: [CommonModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent, LogoInpgComponent],
  template: `
    <header class="bg-white/95 backdrop-blur-sm border-b border-outline-variant/40 sticky top-0 z-50">
      <div class="flex justify-between items-center w-full max-w-container-max mx-auto px-4 md:px-margin-desktop h-[72px]">
        <a routerLink="/accueil" class="no-underline hover:opacity-80 transition-opacity" aria-label="INPG — accueil">
          <app-logo-inpg hauteur="h-10"></app-logo-inpg>
        </a>
        <a [routerLink]="retourUrl"
           class="flex items-center gap-2 px-4 py-2 rounded-lg border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:border-secondary hover:text-secondary transition-colors no-underline">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          Retour à mon espace
        </a>
      </div>
    </header>

    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Accréditation
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Programmes accrédités</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Ces programmes de formation ont été accrédités par l'INPG pour être dispensés dans le
          domaine du pétrole et du gaz. L'accréditation est valable trois ans.
        </p>
      </header>

      <div class="bg-primary-container rounded-xl p-stack-lg flex flex-wrap items-center justify-between gap-4">
        <p class="font-body-md text-body-md text-on-primary-container max-w-xl">
          Vous représentez une école de formation et souhaitez être accréditée par l'INPG ?
        </p>
        <a
          routerLink="/accreditation"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity no-underline shrink-0"
        >
          Déposer une demande
        </a>
      </div>

      <app-ui-spinner *ngIf="chargement" message="Chargement de la liste…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && ecoles.length === 0"
        icone="school"
        titre="Aucune école accréditée pour l'instant"
        description="Cette liste s'enrichira au fur et à mesure des accréditations délivrées par l'INPG."
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && ecoles.length > 0" class="grid sm:grid-cols-2 gap-base">
        <li
          *ngFor="let e of ecoles"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg flex items-start gap-3"
        >
          <span class="material-symbols-outlined text-[28px] shrink-0"
                [class.text-secondary]="!e.sousReserve" [class.text-warning-strong]="e.sousReserve">
            {{ e.sousReserve ? 'verified_user' : 'verified' }}
          </span>
          <div class="min-w-0">
            <p class="font-label-md text-label-md text-on-surface">
              {{ e.intituleProgramme || 'Programme non précisé' }}
            </p>
            <p class="font-body-sm text-body-sm text-on-surface-variant">
              {{ e.nomEcole }}<span *ngIf="e.niveauProgramme"> · {{ libellesNiveau[e.niveauProgramme] }}</span>
            </p>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
              Accrédité le {{ e.dateAccreditation | date: 'd MMMM y' }}<span *ngIf="e.dateExpiration">, valable jusqu'au {{ e.dateExpiration | date: 'd MMMM y' }}</span>
            </p>
            <span *ngIf="e.sousReserve"
                  class="inline-block mt-2 px-2 py-0.5 rounded-full bg-warning/15 text-warning-strong font-label-sm text-label-sm">
              Sous réserve (plan d'actions en cours)
            </span>
          </div>
        </li>
      </ul>
    </div>
  `,
})
export class EcolesAccrediteesComponent implements OnInit {
  private accreditationService = inject(AccreditationService);
  private auth = inject(Auth);

  ecoles: EcoleAccreditee[] = [];
  chargement = true;
  libellesNiveau = LIBELLES_NIVEAU_PROGRAMME;

  get retourUrl(): string {
    return this.auth.getHomeUrl() ?? '/accueil';
  }

  ngOnInit(): void {
    this.accreditationService.getEcolesAccreditees().subscribe({
      next: (ecoles) => {
        this.ecoles = ecoles;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }
}
