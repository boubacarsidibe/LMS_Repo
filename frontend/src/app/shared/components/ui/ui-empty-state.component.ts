import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * État vide.
 *
 * <p>Une base vierge doit produire un message clair, jamais des chiffres inventés
 * pour « remplir » l'écran : c'est précisément ce que ce composant remplace.
 */
@Component({
  selector: 'app-ui-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="flex flex-col items-center justify-center text-center gap-2 py-14 px-6 rounded-xl border border-dashed border-outline-variant bg-surface-container-lowest"
    >
      <span class="material-symbols-outlined text-[40px] text-outline" aria-hidden="true">{{
        icone
      }}</span>
      <p class="font-label-md text-label-md text-on-surface">{{ titre }}</p>
      <p *ngIf="description" class="font-body-sm text-body-sm text-on-surface-variant max-w-md">
        {{ description }}
      </p>
      <button
        *ngIf="actionLibelle"
        type="button"
        (click)="action.emit()"
        class="mt-2 px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity"
      >
        {{ actionLibelle }}
      </button>
    </div>
  `,
})
export class UiEmptyStateComponent {
  @Input() icone = 'inbox';
  @Input() titre = 'Rien à afficher';
  @Input() description = '';
  @Input() actionLibelle = '';
  @Output() action = new EventEmitter<void>();
}
