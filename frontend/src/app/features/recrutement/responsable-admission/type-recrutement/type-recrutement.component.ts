import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Les deux recrutements que conduit le responsable admission. */
export type TypeRecrutement = 'ETUDIANT' | 'FORMATEUR';

interface Segment {
  type: TypeRecrutement;
  libelle: string;
  icone: string;
  route: string;
  aide: string;
}

/** Même ordre et mêmes routes partout : c'est ce qui distingue les deux recrutements à l'écran. */
export const SEGMENTS_RECRUTEMENT: readonly Segment[] = [
  {
    type: 'ETUDIANT',
    libelle: 'Étudiants',
    icone: 'school',
    route: '/responsable-admission/sessions',
    aide: 'Sessions d’admission : quota, critères de présélection, tests et entretiens',
  },
  {
    type: 'FORMATEUR',
    libelle: 'Formateurs',
    icone: 'co_present',
    route: '/responsable-admission/recrutement-formateurs/offres',
    aide: 'Offres de poste de formateur (procédure QMS-007) et leurs candidatures',
  },
];

/**
 * Sélecteur du type de recrutement, posé en tête des pages de sessions (étudiants) et
 * d'offres (formateurs).
 *
 * <p>Les deux recrutements n'ont ni le même modèle ni le même parcours : une session
 * d'étudiants porte un quota, des critères et des tests ; une offre de formateur est une
 * fiche de poste. On ne les fusionne donc pas dans une table, on les distingue ici —
 * le segment actif dit où l'on est, l'autre mène à l'autre recrutement.
 */
@Component({
  selector: 'app-type-recrutement',
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav
      aria-label="Type de recrutement"
      class="inline-flex gap-1 p-1 rounded-lg bg-surface-container-low border border-outline-variant"
    >
      @for (s of segments; track s.type) {
        <a
          [routerLink]="s.route"
          [title]="s.aide"
          [attr.aria-current]="s.type === type() ? 'page' : null"
          class="inline-flex items-center gap-2 h-9 px-4 rounded-lg font-label-md text-label-md no-underline transition-colors"
          [class]="
            s.type === type()
              ? 'bg-primary-container text-on-primary-container font-medium'
              : 'text-on-surface-variant hover:bg-surface-container'
          "
        >
          <span class="material-symbols-outlined text-[18px]" aria-hidden="true">{{ s.icone }}</span>
          {{ s.libelle }}
        </a>
      }
    </nav>
  `,
})
export class TypeRecrutementComponent {
  readonly type = input.required<TypeRecrutement>();
  protected readonly segments = SEGMENTS_RECRUTEMENT;
}
