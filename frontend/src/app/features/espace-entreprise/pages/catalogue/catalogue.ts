import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { FormationCatalogue } from '../../models/entreprise.models';
import { EntrepriseService } from '../../services/entreprise.service';

/** Catalogue des formations publiées, consultable par les entreprises (TDR §H). */
@Component({
  selector: 'app-catalogue-entreprise',
  standalone: true,
  imports: [CommonModule, UiSpinnerComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Espace Entreprise
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Catalogue des formations</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">
          Parcourez l'offre de formation de l'INPG. Pour personnaliser un programme ou demander un devis,
          rendez-vous dans « Mes demandes ».
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement du catalogue…"></app-ui-spinner>

      <div *ngIf="!chargement" class="grid sm:grid-cols-2 lg:grid-cols-3 gap-stack-md">
        <div
          *ngFor="let f of catalogue"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg"
        >
          <span
            *ngIf="f.domaine"
            class="inline-block px-2.5 py-1 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm mb-2"
          >
            {{ f.domaine }}
          </span>
          <h2 class="font-headline-sm text-headline-sm text-primary">{{ f.intitule }}</h2>
          <p class="font-body-sm text-body-sm text-on-surface-variant mt-2 line-clamp-4">
            {{ f.description || 'Aucune description disponible.' }}
          </p>
        </div>
      </div>
      <p *ngIf="!chargement && catalogue.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">
        Aucune formation publiée pour l'instant.
      </p>
    </div>
  `,
})
export class CatalogueEntreprise implements OnInit {
  private entrepriseService = inject(EntrepriseService);

  chargement = true;
  catalogue: FormationCatalogue[] = [];

  ngOnInit(): void {
    this.entrepriseService.getCatalogue().subscribe({
      next: (formations) => {
        this.catalogue = formations || [];
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }
}
