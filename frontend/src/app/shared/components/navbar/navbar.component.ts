import { Component, OnInit, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
})
export class NavbarComponent implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);

  dropdownOpen = false;
  userReady = false; // devient true après vérification du token

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (!user) {
        this.dropdownOpen = false;
      }
    });
  }

  ngOnInit(): void {
    // Vérifier le token au démarrage
    if (this.auth.isLoggedIn()) {
      // Un token existe : tenter de récupérer l'utilisateur courant
      this.auth.getCurrentUser().subscribe({
        next: (user) => {
          if (!user) {
            // Token invalide/expiré → nettoyer
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

  get isLoggedIn(): boolean {
    // Vérifie à la fois le token ET l'utilisateur courant (signal)
    return this.auth.isLoggedIn() && this.auth.currentUser() != null;
  }

  get userInitials(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
    // user.username est au format "prenom_nom"
    const parts = (user as any).username ? (user as any).username.split('_') : [user.email.charAt(0)];
    return parts.length >= 2
      ? (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
      : (parts[0].charAt(0) + parts[0].charAt(1) || '').toUpperCase();
  }

  get userDisplayName(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
    if ((user as any).username) {
      return (user as any).username.replace('_', ' ');
    }
    return user.email;
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
    this.router.navigate(['/recrutement/catalogue']);
  }

  goToMesCandidatures(): void {
    this.dropdownOpen = false;
    this.router.navigate(['/recrutement/suivi']);
  }
}