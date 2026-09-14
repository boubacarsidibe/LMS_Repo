import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { LIBELLES_TYPE_CONTRAT, OffreFormateur } from '../../models/recrutement-formateur.models';
import { RecrutementFormateurService } from '../../services/recrutement-formateur.service';

/**
 * Vitrine des offres de recrutement de formateurs (procédure QMS-007 §2).
 *
 * <p>Page <strong>publique</strong> : la liste se consulte sans compte, comme
 * sur n'importe quel site d'offres d'emploi (l'endpoint est déjà permitAll).
 * Seul « Postuler » exige la connexion — un visiteur anonyme est alors envoyé
 * vers la connexion avec un {@code returnUrl}, qui le ramène sur l'offre qu'il
 * avait choisie. Elle porte donc la navbar et le pied de page publics, comme
 * les autres pages de la vitrine.
 */
@Component({
  selector: 'app-offres-formateurs',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="min-h-screen flex flex-col bg-background">
      <app-navbar></app-navbar>

      <main class="flex-grow w-full p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
        <header>
          <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
            Recrutement des formateurs
          </p>
          <h1 class="font-display text-display text-on-surface leading-none -ml-1">Offres de recrutement</h1>
          <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
            L'INPG recrute des formateurs de spécialité. Consultez les postes ouverts et déposez votre
            candidature en ligne.
          </p>
        </header>

        <app-ui-spinner *ngIf="chargement" message="Chargement des offres…"></app-ui-spinner>

        <app-ui-empty-state
          *ngIf="!chargement && offres.length === 0"
          icone="work_off"
          titre="Aucune offre ouverte pour l'instant"
          description="Revenez plus tard : cette liste s'enrichit au fil des besoins de l'INPG."
        ></app-ui-empty-state>

        <ul *ngIf="!chargement && offres.length > 0" class="grid md:grid-cols-2 gap-base">
          <li
            *ngFor="let o of offres"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg flex flex-col gap-3"
          >
            <div class="flex items-start justify-between gap-3">
              <h2 class="font-headline-sm text-headline-sm text-primary">{{ o.intitule }}</h2>
              <span class="shrink-0 px-2 py-0.5 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm">
                {{ libellesContrat[o.typeContrat] }}
              </span>
            </div>

            <dl class="flex flex-wrap gap-x-6 gap-y-1 font-body-sm text-body-sm text-on-surface-variant">
              <span *ngIf="o.domaine" class="flex items-center gap-1">
                <span class="material-symbols-outlined text-[18px]">category</span>{{ o.domaine }}
              </span>
              <span *ngIf="o.lieu" class="flex items-center gap-1">
                <span class="material-symbols-outlined text-[18px]">location_on</span>{{ o.lieu }}
              </span>
              <span class="flex items-center gap-1">
                <span class="material-symbols-outlined text-[18px]">groups</span>{{ o.nbPostes }} poste(s)
              </span>
              <span *ngIf="o.dateLimiteCandidature" class="flex items-center gap-1">
                <span class="material-symbols-outlined text-[18px]">event</span>
                Limite : {{ o.dateLimiteCandidature | date: 'd MMMM y' }}
              </span>
            </dl>

            <p *ngIf="o.descriptionMissions" class="font-body-sm text-body-sm text-on-surface line-clamp-3">
              {{ o.descriptionMissions }}
            </p>
            <p *ngIf="o.profilRecherche" class="font-body-sm text-body-sm text-on-surface-variant">
              <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider block">Profil recherché</span>
              {{ o.profilRecherche }}
            </p>

            <button
              type="button"
              (click)="postuler(o)"
              class="mt-auto self-start px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity"
            >
              Postuler
            </button>
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
  private router = inject(Router);

  offres: OffreFormateur[] = [];
  chargement = true;
  libellesContrat = LIBELLES_TYPE_CONTRAT;

  ngOnInit(): void {
    this.service.getOffres().subscribe({
      next: (offres) => {
        this.offres = offres;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  /**
   * Consulter est public, postuler ne l'est pas : un visiteur sans compte part
   * vers la connexion en emportant l'offre visée dans {@code returnUrl}, pour
   * revenir exactement là où il en était plutôt que sur l'accueil générique.
   */
  postuler(offre: OffreFormateur): void {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/candidature-formateur'], { queryParams: { offre: offre.id } });
      return;
    }
    this.router.navigate(['/auth/login'], {
      queryParams: { returnUrl: `/candidature-formateur?offre=${offre.id}` },
    });
  }
}
