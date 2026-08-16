import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export type VarianteBadge = 'neutre' | 'info' | 'succes' | 'alerte' | 'erreur';

/**
 * Pastille d'état, déclinée sur les jetons du thème.
 *
 * <p>Centralise les correspondances couleur ↔ sens, jusqu'ici réécrites dans
 * chaque composant sous forme de méthodes `classeStatut()`.
 */
@Component({
  selector: 'app-ui-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full font-label-sm text-label-sm whitespace-nowrap"
      [ngClass]="classes()"
    >
      <span *ngIf="icone" class="material-symbols-outlined text-[14px]" aria-hidden="true">{{
        icone
      }}</span>
      <ng-content></ng-content>
    </span>
  `,
})
export class UiBadgeComponent {
  @Input() variante: VarianteBadge = 'neutre';
  @Input() icone = '';

  classes(): string {
    switch (this.variante) {
      case 'info':
        return 'bg-secondary-container text-on-secondary-container';
      case 'succes':
        return 'bg-primary-fixed text-on-primary-container';
      case 'alerte':
        return 'bg-surface-container-high text-on-surface-variant';
      case 'erreur':
        return 'bg-error-container text-on-error-container';
      default:
        return 'bg-surface-container text-on-surface-variant';
    }
  }
}
