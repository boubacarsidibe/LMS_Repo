import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Cadre de l'espace Candidat formateur (procédure QMS-007 §3).
 *
 * <p>Même patron que {@code EcoleLayout} : une barre latérale, un cercle de
 * profil menant à `mon-profil` (identité + mot de passe, endpoint générique),
 * un bouton de déconnexion.
 */
@Component({
  selector: 'app-candidat-formateur-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <aside class="fixed left-0 top-0 h-full w-64 bg-surface-container-lowest border-r border-outline-variant z-50 flex flex-col shadow-sm">
      <div class="p-margin-desktop mb-4 flex items-center">
        <span class="text-2xl font-extrabold tracking-tight text-primary">INPG<span class="text-brand"> LMS</span></span>
      </div>

      <nav class="flex-1 px-4 space-y-base overflow-y-auto" aria-label="Navigation de l'espace Candidat formateur">
        <a
          routerLink="/candidature-formateur"
          routerLinkActive="bg-primary-container text-on-primary-container! font-medium"
          [routerLinkActiveOptions]="{ exact: true }"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all font-label-md text-label-md text-on-surface-variant hover:bg-surface-container hover:text-on-surface"
        >
          <span class="material-symbols-outlined">assignment_ind</span>
          <span class="flex-1 text-label-md">Ma candidature</span>
        </a>
        <a
          routerLink="/offres-formateurs"
          routerLinkActive="bg-primary-container text-on-primary-container! font-medium"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all font-label-md text-label-md text-on-surface-variant hover:bg-surface-container hover:text-on-surface"
        >
          <span class="material-symbols-outlined">work</span>
          <span class="flex-1 text-label-md">Offres de recrutement</span>
        </a>
      </nav>

      <div class="p-4 border-t border-outline-variant">
        <div class="bg-surface-container-low p-4 rounded-xl">
          <span class="text-label-sm text-on-surface-variant">Connecté</span>
          <p class="font-label-md text-label-md text-on-surface tracking-tight truncate">
            {{ auth.currentUser()?.username || 'Candidat formateur' }}
          </p>
        </div>
      </div>
    </aside>

    <div class="pl-64">
      <header class="fixed top-0 left-64 right-0 h-16 bg-surface-container-lowest/80 backdrop-blur-md border-b border-outline-variant z-40 flex items-center justify-between px-margin-desktop">
        <p class="font-label-md text-label-md text-on-surface-variant">Espace Candidat formateur — Recrutement des formateurs (QMS-007)</p>
        <div class="flex items-center gap-3">
          <a
            routerLink="/candidature-formateur/mon-profil"
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
export class CandidatFormateurLayout {
  auth = inject(Auth);
  private router = inject(Router);

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
}
