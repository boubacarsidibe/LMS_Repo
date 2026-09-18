import { CommonModule } from '@angular/common';
import { Component, HostListener, inject } from '@angular/core';
import { ConfirmationService } from './confirmation.service';

/**
 * Boîte de confirmation affichée par {@link ConfirmationService}.
 *
 * <p>Monté une seule fois à la racine de l'application : les pages n'ont pas à
 * l'importer, elles injectent seulement le service.
 */
@Component({
  selector: 'app-ui-confirmation',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (confirmation.demandeEnCours(); as demande) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40"
        (click)="annulerDepuisFond($event)"
      >
        <div
          class="w-full max-w-md bg-surface rounded-xl border border-outline-variant shadow-lg overflow-hidden"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="confirmation-titre"
          aria-describedby="confirmation-message"
        >
          <div class="p-6 flex flex-col gap-2">
            <h2 id="confirmation-titre" class="font-headline-sm text-headline-sm text-on-surface">
              {{ demande.titre }}
            </h2>
            <p id="confirmation-message" class="font-body-md text-body-md text-on-surface-variant whitespace-pre-line">
              {{ demande.message }}
            </p>
          </div>
          <div class="px-6 py-4 bg-surface-container-low border-t border-outline-variant flex justify-end gap-3">
            <button
              type="button"
              (click)="confirmation.repondre(false)"
              class="px-4 py-2 rounded-lg border border-outline-variant text-on-surface font-label-md text-label-md hover:bg-surface-container transition-colors"
            >
              {{ demande.libelleAnnuler || 'Annuler' }}
            </button>
            <button
              type="button"
              #boutonConfirmer
              (click)="confirmation.repondre(true)"
              class="px-4 py-2 rounded-lg font-label-md text-label-md text-white transition-colors"
              [class.bg-error]="demande.destructif"
              [class.hover:bg-error/90]="demande.destructif"
              [class.bg-secondary]="!demande.destructif"
              [class.hover:bg-secondary/90]="!demande.destructif"
            >
              {{ demande.libelleConfirmer || 'Confirmer' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class UiConfirmationComponent {
  readonly confirmation = inject(ConfirmationService);

  /** Échap annule : comportement attendu de toute boîte modale. */
  @HostListener('document:keydown.escape')
  surEchap(): void {
    if (this.confirmation.demandeEnCours()) {
      this.confirmation.repondre(false);
    }
  }

  /** Un clic sur le fond annule, mais pas un clic dans la boîte elle-même. */
  annulerDepuisFond(evenement: MouseEvent): void {
    if (evenement.target === evenement.currentTarget) {
      this.confirmation.repondre(false);
    }
  }
}
