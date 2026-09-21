import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Cadre de l'espace École de formation (TDR §F).
 *
 * <p>Reconstruit en barre de navigation horizontale (2026-09-16), à l'image de
 * l'espace candidat : une école qui dépose un dossier d'accréditation vit la
 * même relation « candidature suivie dans le temps » qu'un candidat, pas le
 * pilotage au quotidien d'un formateur ou d'un responsable — la barre latérale
 * façon outil interne, identique à celle de l'étudiant ou de la pédagogie, ne
 * correspondait pas à cet usage ponctuel. Remplace l'ancien cadre à barre
 * latérale (même structure que {@code EtudiantLayout}), jugé trop
 * « administrateur » pour un public externe.
 */
@Component({
  selector: 'app-ecole-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="bg-white/95 backdrop-blur-sm border-b border-outline-variant/40 fixed top-0 left-0 right-0 z-50">
      <div class="flex justify-between items-center w-full max-w-[1280px] mx-auto px-4 md:px-10 h-[72px]">
        <a routerLink="/accreditation" class="text-2xl font-extrabold tracking-tight text-primary no-underline hover:opacity-80 transition-opacity">
          INPG<span class="text-brand"> LMS</span>
        </a>

        <!-- « Accueil » plutôt que « Ma demande » (2026-09-20, retour explicite) : cette
             page EST déjà la page d'accueil de l'espace (route racine) ; ajouter un
             second onglet menant au même endroit aurait été redondant. Même renommage
             pour le repli mobile ci-dessous. Pas d'onglet « Rendez-vous » (2026-09-21) :
             une prise de rendez-vous est un geste ponctuel, un bouton de cette page y mène. -->
        <nav class="hidden md:flex flex-1 justify-center items-center gap-6">
          <a routerLink="/accreditation" [routerLinkActiveOptions]="{ exact: true }" routerLinkActive="text-primary border-b-2 border-primary"
             class="px-3 py-2 text-sm font-medium text-on-surface-variant hover:text-primary">Accueil</a>
        </nav>

        <div class="flex items-center gap-3">
          <button type="button" (click)="toggleMobileMenu()"
                  class="md:hidden p-2 rounded-full hover:bg-surface-container-low transition-colors cursor-pointer border-0 bg-transparent"
                  aria-label="Ouvrir le menu">
            <span class="material-symbols-outlined text-on-surface-variant text-[24px]">{{ mobileMenuOpen ? 'close' : 'menu' }}</span>
          </button>

          <div class="hidden sm:block h-8 w-px bg-outline-variant/40 mx-1"></div>

          <div class="relative">
            <button type="button" (click)="toggleDropdown()"
                    class="flex items-center gap-3 p-1.5 rounded-full hover:bg-surface-container-low transition-colors cursor-pointer border-0 bg-transparent"
                    aria-label="Menu utilisateur">
              <div class="w-9 h-9 rounded-full bg-primary text-white flex items-center justify-center text-sm font-semibold shadow-sm">
                {{ initiales }}
              </div>
              <div class="hidden sm:flex flex-col items-start leading-tight">
                <span class="text-sm font-medium text-on-surface truncate max-w-[160px]">{{ auth.currentUser()?.username || 'École de formation' }}</span>
                <span class="text-xs uppercase text-outline">École de formation</span>
              </div>
              <span class="material-symbols-outlined text-outline text-[20px]">{{ dropdownOpen ? 'expand_less' : 'expand_more' }}</span>
            </button>

            <div *ngIf="dropdownOpen" class="absolute right-0 mt-2 w-56 bg-white border border-outline-variant rounded-xl shadow-lg py-1 z-50 origin-top-right">
              <a routerLink="/accreditation/mon-profil" (click)="dropdownOpen = false"
                 class="flex items-center gap-3 px-4 py-2.5 text-sm text-on-surface-variant hover:bg-surface-container-low hover:text-primary cursor-pointer transition-colors no-underline">
                <span class="material-symbols-outlined text-[20px]">account_circle</span>
                Mon profil
              </a>
              <hr class="my-1 border-outline-variant/40">
              <a (click)="deconnexion()"
                 class="flex items-center gap-3 px-4 py-2.5 text-sm text-on-surface-variant hover:bg-error-container hover:text-error cursor-pointer transition-colors">
                <span class="material-symbols-outlined text-[20px]">logout</span>
                Déconnexion
              </a>
            </div>
          </div>
        </div>
      </div>

      <nav *ngIf="mobileMenuOpen" class="md:hidden border-t border-outline-variant/40 bg-white px-4 py-3 space-y-1" aria-label="Navigation">
        <a routerLink="/accreditation" (click)="mobileMenuOpen = false" [routerLinkActiveOptions]="{ exact: true }" routerLinkActive="text-primary bg-surface-container-low"
           class="block px-3 py-2.5 rounded-lg text-sm font-medium text-on-surface-variant hover:bg-surface-container-low hover:text-primary">Accueil</a>
      </nav>
    </header>
    <div class="h-[72px]"></div>

    <div *ngIf="dropdownOpen" class="fixed inset-0 z-40" (click)="dropdownOpen = false"></div>

    <main class="min-h-screen bg-background">
      <router-outlet></router-outlet>
    </main>
  `,
})
export class EcoleLayout implements OnInit {
  auth = inject(Auth);
  private router = inject(Router);
  dropdownOpen = false;
  mobileMenuOpen = false;

  /** Le signal `currentUser` n'est pas toujours déjà peuplé à l'arrivée sur ce
   * layout (ex. juste après une connexion) — l'identifiant affiché resterait
   * vide (repli « École de formation ») sans ce chargement explicite. */
  ngOnInit(): void {
    if (!this.auth.currentUser()) {
      this.auth.getCurrentUser().subscribe();
    }
  }

  get initiales(): string {
    const nom = this.auth.currentUser()?.username || '';
    const morceaux = nom.trim().split(/[\s_.-]+/).filter(Boolean);
    if (morceaux.length === 0) {
      return '?';
    }
    return morceaux.slice(0, 2).map((m) => m[0]).join('').toUpperCase();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  deconnexion(): void {
    this.dropdownOpen = false;
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/accueil']),
      error: () => this.router.navigate(['/accueil']),
    });
  }
}
