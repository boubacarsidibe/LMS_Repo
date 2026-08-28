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
 * Cadre de l'espace Entreprise (TDR §H) : mêmes conventions visuelles que
 * {@code AlumniLayout}/{@code EcoleLayout} (charte graphique CLAUDE.md §1).
 * Plusieurs rubriques (contrairement à l'école, une seule demande possible) :
 * une entreprise peut soumettre plusieurs besoins/devis au fil du temps.
 */
@Component({
  selector: 'app-entreprise-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './entreprise-layout.html',
})
export class EntrepriseLayout {
  auth = inject(Auth);
  private router = inject(Router);

  readonly navigation: EntreeNavigation[] = [
    { libelle: 'Mes demandes', lien: 'nouvelle-demande', icone: 'assignment' },
    { libelle: 'Catalogue des formations', lien: 'catalogue', icone: 'menu_book' },
    { libelle: 'Messagerie', lien: 'messagerie', icone: 'forum' },
    { libelle: 'Rendez-vous', lien: 'rendez-vous', icone: 'event' },
  ];

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
