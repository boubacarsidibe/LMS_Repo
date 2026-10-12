import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { clotureImminente, dateLongue, estOuverte, joursRestants } from '../../delai-offre';
import { LIBELLES_TYPE_CONTRAT, OffreFormateur } from '../../models/recrutement-formateur.models';
import { RecrutementFormateurService } from '../../services/recrutement-formateur.service';

/** Au-delà de cette longueur, un texte est replié derrière « Lire la suite ». */
const SEUIL_TEXTE_LONG = 240;

/**
 * Vitrine publique des offres de recrutement de formateurs (procédure QMS-007 §2).
 *
 * <p>Ouverte à tous, sans connexion : c'est la page où un visiteur découvre les postes, comme
 * « Sessions de candidature ouvertes » l'est pour les étudiants — même mise en page, mêmes
 * cartes. Seule la candidature exige un compte. Les offres dont la date limite est passée ne
 * sont pas listées : le serveur refuserait la candidature.
 */
@Component({
  selector: 'app-offres-formateurs',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent, FooterComponent],
  template: `
    <div class="min-h-screen flex flex-col bg-surface">
      <app-navbar></app-navbar>

      <main class="flex-grow w-full px-4 md:px-10 py-8 max-w-5xl mx-auto">
        <header class="mb-8">
          <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
            Recrutement des formateurs
          </p>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Offres de recrutement</h1>
          <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
            L'INPG recrute des formateurs de spécialité. Consultez les postes ouverts, téléchargez la fiche de poste
            et déposez votre candidature en ligne.
          </p>
          <p class="font-body-sm text-body-sm text-on-surface-variant mt-3">
            Vous souhaitez plutôt suivre une formation ?
            <a routerLink="/recrutement/sessions" class="text-primary underline">Voir les sessions d'admission</a>
          </p>
        </header>

        <div
          *ngIf="!connecte"
          class="mb-6 p-4 rounded-xl border border-outline-variant bg-surface-container-low flex flex-wrap items-center justify-between gap-3"
        >
          <p class="font-body-sm text-body-sm text-on-surface-variant flex items-center gap-2">
            <span class="material-symbols-outlined text-[20px] text-secondary" aria-hidden="true">info</span>
            Un compte est nécessaire pour déposer une candidature ; la consultation des offres est libre.
          </p>
          <div class="flex gap-2">
            <a
              routerLink="/auth/login"
              class="px-4 py-2 rounded-lg bg-surface-container-lowest border border-outline-variant text-on-surface font-label-md text-label-md hover:bg-surface-container-low transition-colors no-underline"
              >Se connecter</a
            >
            <a
              routerLink="/auth/register"
              class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity no-underline"
              >Créer un compte</a
            >
          </div>
        </div>

        <div *ngIf="chargement" class="py-20 text-center text-on-surface-variant" role="status">
          <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
          Chargement des offres…
        </div>

        <div
          *ngIf="erreur"
          class="p-4 bg-error-container text-on-error-container rounded-lg font-body-sm text-body-sm mb-6"
          role="alert"
        >
          {{ erreur }}
        </div>

        <div
          *ngIf="!chargement && !erreur && offres.length === 0"
          class="py-20 text-center bg-surface-container-lowest border border-outline-variant rounded-xl shadow-sm"
        >
          <span class="material-symbols-outlined text-5xl text-outline-variant mb-3 block" aria-hidden="true">work_off</span>
          <p class="text-on-surface-variant font-medium">Aucune offre de recrutement n'est ouverte pour le moment.</p>
          <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
            Revenez bientôt, ou <a routerLink="/contact" class="text-primary underline">contactez l'INPG</a> pour être
            informé(e) de la prochaine ouverture.
          </p>
        </div>

        <ul *ngIf="!chargement && offres.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-5">
          <li
            *ngFor="let o of offres"
            class="bg-surface-container-lowest border border-outline-variant rounded-xl shadow-sm p-6 flex flex-col gap-4 hover:shadow-md transition-shadow"
          >
            <div class="flex items-start justify-between gap-3">
              <h2 class="font-body-lg text-body-lg font-semibold text-on-surface leading-tight">{{ o.intitule }}</h2>
              <span
                class="shrink-0 px-2.5 py-1 rounded-full font-label-sm text-label-sm"
                [ngClass]="
                  imminente(o)
                    ? 'bg-error-container text-on-error-container'
                    : 'bg-primary-container text-on-primary-container'
                "
              >
                {{ imminente(o) ? 'Clôture imminente' : 'Ouverte' }}
              </span>
            </div>

            <ul class="flex flex-wrap gap-2" aria-label="Caractéristiques du poste">
              <li class="px-2.5 py-1 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm">
                {{ libellesContrat[o.typeContrat] }}
              </li>
              <li
                *ngIf="o.domaine"
                class="px-2.5 py-1 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm"
              >
                {{ o.domaine }}
              </li>
            </ul>

            <div class="flex flex-col gap-1.5 font-body-sm text-body-sm text-on-surface-variant">
              <div *ngIf="o.lieu" class="flex items-center gap-2">
                <span class="material-symbols-outlined text-[18px] text-outline" aria-hidden="true">location_on</span>
                {{ o.lieu }}
              </div>
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-[18px] text-outline" aria-hidden="true">groups</span>
                {{ o.nbPostes }} poste{{ o.nbPostes > 1 ? 's' : '' }} à pourvoir
              </div>
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-[18px] text-outline" aria-hidden="true">timer</span>
                {{ delai(o) }}
              </div>
            </div>

            <div *ngIf="o.descriptionMissions">
              <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Missions</p>
              <p
                class="font-body-sm text-body-sm text-on-surface whitespace-pre-line"
                [class.line-clamp-4]="!estDeplie(o.id) && estLong(o.descriptionMissions)"
              >
                {{ o.descriptionMissions }}
              </p>
            </div>
            <div *ngIf="o.profilRecherche">
              <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">
                Profil recherché
              </p>
              <p
                class="font-body-sm text-body-sm text-on-surface whitespace-pre-line"
                [class.line-clamp-4]="!estDeplie(o.id) && estLong(o.profilRecherche)"
              >
                {{ o.profilRecherche }}
              </p>
            </div>
            <button
              *ngIf="estLong(o.descriptionMissions) || estLong(o.profilRecherche)"
              type="button"
              (click)="basculer(o.id)"
              class="self-start font-label-md text-label-md text-primary underline"
            >
              {{ estDeplie(o.id) ? 'Réduire' : 'Lire la suite' }}
            </button>

            <div class="mt-auto flex flex-col sm:flex-row gap-2">
              <a
                routerLink="/candidature-formateur"
                [queryParams]="{ offre: o.id }"
                class="flex-1 inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity no-underline"
              >
                Postuler à cette offre
                <span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_forward</span>
              </a>
              <a
                *ngIf="o.fichePosteNom"
                [href]="urlFichePoste(o.id)"
                target="_blank"
                rel="noopener"
                class="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-surface-container-lowest border border-outline-variant text-on-surface font-label-md text-label-md rounded-lg hover:bg-surface-container-low transition-colors no-underline"
              >
                <span class="material-symbols-outlined text-[18px]" aria-hidden="true">description</span>
                Fiche de poste (PDF)
              </a>
            </div>
          </li>
        </ul>
      </main>

      <app-footer></app-footer>
    </div>
  `,
})
export class OffresPubliquesComponent implements OnInit {
  private service = inject(RecrutementFormateurService);
  private auth = inject(Auth);

  offres: OffreFormateur[] = [];
  chargement = true;
  erreur = '';
  libellesContrat = LIBELLES_TYPE_CONTRAT;
  private deplies = new Set<number>();

  get connecte(): boolean {
    return this.auth.isLoggedIn();
  }

  ngOnInit(): void {
    this.service.getOffres().subscribe({
      next: (offres) => {
        const maintenant = new Date();
        this.offres = (offres ?? []).filter((o) => estOuverte(o.dateLimiteCandidature, maintenant));
        this.chargement = false;
      },
      error: () => {
        this.erreur = 'Impossible de charger les offres pour le moment. Réessayez dans quelques instants.';
        this.chargement = false;
      },
    });
  }

  urlFichePoste(offreId: number): string {
    return this.service.urlFichePoste(offreId);
  }

  imminente(o: OffreFormateur): boolean {
    return clotureImminente(o.dateLimiteCandidature, new Date());
  }

  /** « Clôture le 12 décembre 2026 (61 jours restants) », ou « Dernier jour », ou « Sans date limite ». */
  delai(o: OffreFormateur): string {
    const restants = joursRestants(o.dateLimiteCandidature, new Date());
    if (restants === null || !o.dateLimiteCandidature) {
      return 'Candidatures ouvertes, sans date limite';
    }
    const date = dateLongue(o.dateLimiteCandidature);
    if (restants === 0) {
      return `Dernier jour pour candidater (${date})`;
    }
    return `Clôture le ${date} (${restants} jour${restants > 1 ? 's' : ''} restant${restants > 1 ? 's' : ''})`;
  }

  estLong(texte: string | null): boolean {
    return !!texte && texte.length > SEUIL_TEXTE_LONG;
  }

  estDeplie(id: number): boolean {
    return this.deplies.has(id);
  }

  basculer(id: number): void {
    if (!this.deplies.delete(id)) {
      this.deplies.add(id);
    }
  }
}
