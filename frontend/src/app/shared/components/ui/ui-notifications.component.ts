import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NotificationService } from './notification.service';

/**
 * Pile de notifications affichée par {@link NotificationService}.
 *
 * <p>Monté une seule fois à la racine de l'application, comme
 * {@code app-ui-confirmation}.
 */
@Component({
  selector: 'app-ui-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="fixed bottom-4 right-4 z-[110] flex flex-col gap-2 w-[min(24rem,calc(100vw-2rem))]"
      aria-live="polite"
      aria-atomic="false"
    >
      @for (notification of service.notifications(); track notification.id) {
        <div
          class="flex items-start gap-3 p-4 rounded-lg border shadow-md bg-surface"
          [class.border-error]="notification.ton === 'erreur'"
          [class.border-secondary]="notification.ton === 'succes'"
          [class.border-outline-variant]="notification.ton === 'info'"
          [attr.role]="notification.ton === 'erreur' ? 'alert' : 'status'"
        >
          <span
            class="material-symbols-outlined text-[20px] shrink-0"
            [class.text-error]="notification.ton === 'erreur'"
            [class.text-secondary]="notification.ton === 'succes'"
            [class.text-on-surface-variant]="notification.ton === 'info'"
            aria-hidden="true"
          >
            {{
              notification.ton === 'erreur'
                ? 'error'
                : notification.ton === 'succes'
                  ? 'check_circle'
                  : 'info'
            }}
          </span>
          <p class="flex-1 font-body-sm text-body-sm text-on-surface">{{ notification.message }}</p>
          <button
            type="button"
            (click)="service.fermer(notification.id)"
            class="shrink-0 p-1 rounded text-on-surface-variant hover:bg-surface-container transition-colors"
            aria-label="Fermer la notification"
          >
            <span class="material-symbols-outlined text-[18px]" aria-hidden="true">close</span>
          </button>
        </div>
      }
    </div>
  `,
})
export class UiNotificationsComponent {
  readonly service = inject(NotificationService);
}
