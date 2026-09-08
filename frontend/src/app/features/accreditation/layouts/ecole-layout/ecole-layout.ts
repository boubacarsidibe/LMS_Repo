import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Cadre de l'espace École de formation (TDR §F).
 *
 * <p>N'existait pas : la page de dépôt de la demande d'accréditation s'affichait
 * seule, sans en-tête ni moyen de se déconnecter ou de changer son mot de passe —
 * un utilisateur qui y arrivait n'avait littéralement aucun autre endroit où
 * aller. Même patron que {@code EvaluateurLayout} : une seule rubrique, un
 * cercle de profil qui mène à `mon-profil` (identité + mot de passe, endpoint
 * générique). Décision produit du 2026-08-25 (uniformisation de la page profil
 * sur tous les rôles, LOG_BLOQUANT.md).
 */
@Component({
  selector: 'app-ecole-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div *ngIf="sidebarMobileOpen" class="fixed inset-0 bg-primary/20 backdrop-blur-sm z-40 md:hidden" (click)="toggleSidebarMobile()"></div>

    <aside class="fixed left-0 top-0 h-full w-72 bg-surface-container-lowest border-r border-outline-variant z-50 flex flex-col shadow-sm transition-transform duration-200 md:translate-x-0"
      [class.-translate-x-full]="!sidebarMobileOpen"
      [class.translate-x-0]="sidebarMobileOpen">
      <div class="p-margin-desktop mb-4 flex items-center">
        <span class="text-2xl font-extrabold tracking-tight text-primary">INPG<span class="text-brand"> LMS</span></span>
      </div>

      <nav class="flex-1 px-4 space-y-base overflow-y-auto" aria-label="Navigation de l'espace École de formation">
        <a
          routerLink="/accreditation"
          routerLinkActive="bg-primary-container text-on-primary-container shadow-sm"
          [routerLinkActiveOptions]="{ exact: true }"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all font-label-md text-label-md text-on-surface-variant hover:bg-surface-container hover:text-on-surface"
        >
          <span class="material-symbols-outlined">school</span>
          <span class="flex-1 text-label-md">Ma demande</span>
        </a>
        <a
          routerLink="/accreditation/rendez-vous"
          routerLinkActive="bg-primary-container text-on-primary-container shadow-sm"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all font-label-md text-label-md text-on-surface-variant hover:bg-surface-container hover:text-on-surface"
        >
          <span class="material-symbols-outlined">event</span>
          <span class="flex-1 text-label-md">Rendez-vous</span>
        </a>
      </nav>

      <div class="p-4 border-t border-outline-variant">
        <div class="bg-surface-container-low p-4 rounded-xl">
          <span class="text-label-sm text-on-surface-variant">Connecté</span>
          <p class="font-label-md text-label-md text-on-surface tracking-tight truncate">
            {{ auth.currentUser()?.username || 'École de formation' }}
          </p>
        </div>
      </div>
    </aside>

    <div class="md:pl-72">
      <header class="fixed top-0 left-0 md:left-72 right-0 h-16 bg-surface/80 backdrop-blur-xl border-b border-outline-variant z-30 flex items-center justify-between px-4 md:px-margin-desktop">
        <div class="flex items-center gap-3">
          <button type="button" (click)="toggleSidebarMobile()" class="md:hidden p-2 text-on-surface-variant hover:bg-surface-container rounded-full transition-colors" aria-label="Ouvrir le menu">
            <span class="material-symbols-outlined">menu</span>
          </button>
          <p class="font-label-md text-label-md text-on-surface-variant hidden sm:block">Espace École de formation — Accréditation</p>
        </div>
        <div class="flex items-center gap-3">
          <a
            routerLink="/accreditation/mon-profil"
            class="flex items-center gap-1 group cursor-pointer"
            aria-label="Voir mon profil"
            title="Mon profil">
            <div class="w-9 h-9 rounded-full bg-primary flex items-center justify-center shadow-sm group-hover:opacity-90 transition-opacity">
              <span class="text-white font-label-md text-label-md">{{ initiales }}</span>
            </div>
            <span class="material-symbols-outlined text-on-surface-variant text-[20px] group-hover:text-primary transition-colors">expand_more</span>
          </a>
          <button
            type="button"
            (click)="deconnexion()"
            class="p-2 text-on-surface-variant hover:bg-error-container hover:text-on-error-container rounded-full transition-colors"
            aria-label="Se déconnecter"
            title="Se déconnecter"
          >
            <span class="material-symbols-outlined">logout</span>
          </button>
        </div>
      </header>

      <main class="relative pt-16 min-h-screen bg-background">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
})
export class EcoleLayout {
  auth = inject(Auth);
  private router = inject(Router);
  sidebarMobileOpen = false;

  get initiales(): string {
    const nom = this.auth.currentUser()?.username || '';
    const morceaux = nom.trim().split(/[\s_.-]+/).filter(Boolean);
    if (morceaux.length === 0) {
      return '?';
    }
    return morceaux.slice(0, 2).map((m) => m[0]).join('').toUpperCase();
  }

  deconnexion(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/accueil']),
      error: () => this.router.navigate(['/accueil']),
    });
  }

  toggleSidebarMobile(): void {
    this.sidebarMobileOpen = !this.sidebarMobileOpen;
  }
}
