import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';

interface EntreeNavigation {
  libelle: string;
  lien: string;
}

/**
 * Cadre de l'espace Entreprise (TDR §H).
 *
 * <p>Reconstruit en barre de navigation horizontale (2026-09-16), à l'image de
 * l'espace candidat : une entreprise partenaire vit une relation ponctuelle
 * avec l'INPG (demandes, devis, rendez-vous), pas le pilotage quotidien d'un
 * outil interne — la barre latérale façon outil interne ne correspondait pas
 * à cet usage. Remplace l'ancien cadre à barre latérale.
 */
@Component({
  selector: 'app-entreprise-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './entreprise-layout.html',
})
export class EntrepriseLayout implements OnInit {
  auth = inject(Auth);
  private router = inject(Router);
  dropdownOpen = false;
  mobileMenuOpen = false;

  readonly navigation: EntreeNavigation[] = [
    { libelle: 'Mes demandes', lien: 'nouvelle-demande' },
    { libelle: 'Catalogue des formations', lien: 'catalogue' },
    { libelle: 'Messagerie', lien: 'messagerie' },
  ];

  /** Le signal `currentUser` n'est pas toujours déjà peuplé à l'arrivée sur ce layout. */
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
