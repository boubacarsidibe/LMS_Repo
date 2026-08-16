import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Indicateur de chargement.
 *
 * <p>Remplace les « Chargement… » en texte brut disséminés dans les pages, pour
 * que l'attente ait la même apparence partout.
 */
@Component({
  selector: 'app-ui-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center justify-center gap-3 py-10 text-on-surface-variant" role="status">
      <span
        class="inline-block rounded-full border-2 border-outline-variant border-t-secondary animate-spin"
        [style.width.px]="taille"
        [style.height.px]="taille"
        aria-hidden="true"
      ></span>
      <span class="font-body-sm text-body-sm">{{ message }}</span>
    </div>
  `,
})
export class UiSpinnerComponent {
  @Input() message = 'Chargement…';
  @Input() taille = 20;
}
