import { Component, OnInit, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';

/**
 * Navbar de l'espace étudiant authentifié (pages accessibles aux étudiants
 * et/ou formateurs connectés). Contrairement à NavbarComponent (pages
 * publiques de recrutement), elle n'a jamais besoin de gérer un visiteur non
 * connecté ni des notifications de candidature : chaque route qui l'utilise
 * est déjà garantie ROLE_ETUDIANT et/ou ROLE_FORMATEUR par son guard.
 */
@Component({
  selector: 'app-espace-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './espace-navbar.component.html',
})
export class EspaceNavbarComponent implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);

  dropdownOpen = false;
  userReady = false;

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (!user) {
        this.dropdownOpen = false;
      }
    });
  }

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      this.auth.getCurrentUser().subscribe({
        next: (user) => {
          if (!user) {
            this.auth.clearSession();
          }
          this.userReady = true;
        },
        error: () => {
          this.auth.clearSession();
          this.userReady = true;
        },
      });
    } else {
      this.userReady = true;
    }
  }

  get userInitials(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
    const parts = (user as any).username ? (user as any).username.split('_') : [user.email.charAt(0)];
    return parts.length >= 2
      ? (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
      : (parts[0].charAt(0) + parts[0].charAt(1) || '').toUpperCase();
  }

  get isEtudiantRole(): boolean {
    return this.auth.getUserRoles().includes('ROLE_ETUDIANT');
  }

  get isFormateurRole(): boolean {
    return this.auth.getUserRoles().includes('ROLE_FORMATEUR');
  }

  get userDisplayName(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
    if ((user as any).username) {
      return (user as any).username.replace('_', ' ');
    }
    return user.email;
  }

  getUserRoleLabel(): string {
    const roles = this.auth.getUserRoles();
    if (!roles || roles.length === 0) return '';
    const r = roles[0];
    switch (r) {
      case 'ROLE_RESPONSABLE_ADMISSION': return 'RESPONSABLE ADMISSION';
      case 'ROLE_CANDIDAT': return 'CANDIDAT';
      case 'ROLE_ETUDIANT': return 'ETUDIANT';
      case 'ROLE_FORMATEUR': return 'FORMATEUR';
      default: return r.replace('ROLE_', '');
    }
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.dropdownOpen = false;
        this.router.navigate(['/accueil']);
      },
      error: () => {
        this.auth.clearSession();
        this.dropdownOpen = false;
        this.router.navigate(['/accueil']);
      },
    });
  }

  goToDashboard(): void {
    this.dropdownOpen = false;
    this.router.navigate(['/espace-etudiant/accueil']);
  }
}
