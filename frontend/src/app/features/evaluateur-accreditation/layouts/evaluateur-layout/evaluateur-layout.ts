import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';

interface EntreeNavigation {
  libelle: string;
  lien: string;
  icone: string;
}

/**
 * Cadre de l'espace Évaluateur Accréditation : mêmes conventions visuelles que
 * {@code AlumniLayout} (charte graphique CLAUDE.md §1) — deux rubriques, pas de
 * cercle de profil (aucune fiche dédiée pour ce rôle, cf. CLAUDE.md §3 sur le
 * responsable pédagogique : mieux vaut l'omettre qu'en poser un qui ne mène
 * nulle part).
 */
@Component({
  selector: 'app-evaluateur-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <aside class="fixed left-0 top-0 h-full w-72 bg-surface-container-lowest border-r border-outline-variant z-50 flex flex-col shadow-sm">
      <div class="p-margin-desktop mb-4 flex items-center">
        <span class="text-2xl font-extrabold tracking-tight text-primary">INPG<span class="text-brand"> LMS</span></span>
      </div>

      <nav class="flex-1 px-4 space-y-base overflow-y-auto" aria-label="Navigation de l'espace Évaluateur">
        <a
          *ngFor="let entree of navigation"
          [routerLink]="entree.lien"
          routerLinkActive="bg-primary-container text-on-primary-container shadow-sm"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all font-label-md text-label-md text-on-surface-variant hover:bg-surface-container hover:text-on-surface"
        >
          <span class="material-symbols-outlined">{{ entree.icone }}</span>
          <span class="flex-1 text-label-md">{{ entree.libelle }}</span>
        </a>
      </nav>

      <div class="p-4 border-t border-outline-variant">
        <div class="bg-surface-container-low p-4 rounded-xl">
          <span class="text-label-sm text-on-surface-variant">Connecté</span>
          <p class="font-label-md text-label-md text-on-surface tracking-tight truncate">
            {{ auth.currentUser()?.username || 'Évaluateur' }}
          </p>
        </div>
      </div>
    </aside>

    <div class="pl-72">
      <header class="fixed top-0 left-72 right-0 h-16 bg-surface/80 backdrop-blur-xl border-b border-outline-variant z-40 flex items-center justify-between px-margin-desktop">
        <p class="font-label-md text-label-md text-on-surface-variant">Espace Évaluateur — Accréditation des écoles</p>
        <button
          type="button"
          (click)="deconnexion()"
          class="p-2 text-on-surface-variant hover:bg-error-container hover:text-on-error-container rounded-full transition-colors"
          aria-label="Se déconnecter"
          title="Se déconnecter"
        >
          <span class="material-symbols-outlined">logout</span>
        </button>
      </header>

      <main class="relative pt-16 min-h-screen bg-background">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
})
export class EvaluateurLayout {
  auth = inject(Auth);
  private router = inject(Router);

  readonly navigation: EntreeNavigation[] = [
    { libelle: 'Demandes', lien: 'demandes', icone: 'fact_check' },
    { libelle: 'Critères', lien: 'criteres', icone: 'checklist' },
  ];

  deconnexion(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/accueil']),
      error: () => this.router.navigate(['/accueil']),
    });
  }
}
