import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Input, Output, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Menu du compte des espaces à barre latérale : le cercle d'initiales ouvre une
 * liste déroulante (accès à la page profil, déconnexion) et son chevron se
 * retourne tant qu'elle est ouverte. La déconnexion n'est plus un bouton isolé
 * de l'en-tête, et la page profil n'a plus d'entrée dans la barre latérale.
 */
@Component({
  selector: 'app-profile-menu',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="relative">
      <button
        type="button"
        (click)="basculer()"
        class="flex items-center gap-1 group cursor-pointer"
        aria-haspopup="menu"
        [attr.aria-expanded]="ouvert"
        aria-label="Menu du compte"
      >
        <div class="w-9 h-9 rounded-full bg-primary flex items-center justify-center shadow-sm group-hover:opacity-90 transition-opacity">
          <span class="text-white font-label-md text-label-md">{{ initiales }}</span>
        </div>
      </button>

      <div
        *ngIf="ouvert"
        role="menu"
        class="absolute right-0 mt-2 w-56 bg-surface border border-outline-variant rounded-xl shadow-lg py-1 z-50"
      >
        <div class="px-4 py-3 border-b border-outline-variant">
          <p class="font-label-md text-label-md text-on-surface truncate">{{ nom }}</p>
          <p *ngIf="email" class="font-body-sm text-body-sm text-on-surface-variant truncate">{{ email }}</p>
        </div>
        <a
          role="menuitem"
          [routerLink]="lienProfil"
          (click)="ouvert = false"
          class="flex items-center gap-3 px-4 py-2.5 font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low hover:text-primary cursor-pointer transition-colors"
        >
          <span class="material-symbols-outlined text-[20px]">account_circle</span> Mon profil
        </a>
        <hr class="my-1 border-outline-variant" />
        <button
          type="button"
          role="menuitem"
          (click)="seDeconnecter()"
          class="w-full text-left flex items-center gap-3 px-4 py-2.5 font-label-md text-label-md text-on-surface-variant hover:bg-error-container hover:text-error cursor-pointer transition-colors"
        >
          <span class="material-symbols-outlined text-[20px]">logout</span> Déconnexion
        </button>
      </div>
    </div>
  `,
})
export class ProfileMenuComponent {
  @Input() initiales = '';
  @Input() nom = '';
  @Input() email = '';
  @Input() lienProfil: string | string[] = 'mon-profil';
  @Output() deconnexion = new EventEmitter<void>();

  ouvert = false;

  private hote = inject(ElementRef<HTMLElement>);

  basculer(): void {
    this.ouvert = !this.ouvert;
  }

  seDeconnecter(): void {
    this.ouvert = false;
    this.deconnexion.emit();
  }

  @HostListener('document:click', ['$event'])
  fermerSiClicExterieur(event: MouseEvent): void {
    if (this.ouvert && !this.hote.nativeElement.contains(event.target as Node)) {
      this.ouvert = false;
    }
  }

  @HostListener('document:keydown.escape')
  fermerAvecEchap(): void {
    this.ouvert = false;
  }
}
