import { Component, OnInit, OnDestroy, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { Auth } from '../../../core/auth/services/authService';
import { RecrutementService, CandidatureResponse, NotificationCandidatResponse } from '../../../core/recrutement/services/recrutement.service';

/**
 * Navbar dédiée à l'espace candidat authentifié (candidature, paiement, suivi).
 * Distincte de NavbarComponent (pages publiques) et de EspaceNavbarComponent
 * (espace étudiant/formateur) : un compte ne pouvant détenir qu'un seul rôle
 * à la fois, chaque espace a désormais sa propre navigation dédiée.
 */
@Component({
  selector: 'app-candidat-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './candidat-navbar.component.html',
})
export class CandidatNavbarComponent implements OnInit, OnDestroy {
  private auth = inject(Auth);
  private router = inject(Router);
  private recrutementService = inject(RecrutementService);

  dropdownOpen = false;
  notifDropdownOpen = false;
  userReady = false;
  // Tant que la candidature n'est pas soumise (ROLE_CANDIDAT pas encore accordé),
  // "Ma candidature" doit renvoyer vers la liste des sessions plutôt que /recrutement/suivi
  // (protégée par candidatGuard), sinon le lien bounce l'utilisateur vers /accueil.
  candidatureUrl = '/recrutement/sessions';

  notifications: NotificationCandidatResponse[] = [];
  nbNonLues = 0;
  private pollingSub: Subscription | null = null;

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (!user) {
        this.dropdownOpen = false;
        this.notifDropdownOpen = false;
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
          this.checkCandidatures();
          this.startNotificationPolling();
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

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
  }

  private checkCandidatures(): void {
    this.recrutementService.getMesCandidatures().subscribe({
      next: (candidatures: CandidatureResponse[]) => {
        const soumises = (candidatures || []).filter(c => c.statut !== 'BROUILLON');
        this.candidatureUrl = soumises.length > 0 ? '/recrutement/suivi' : '/recrutement/sessions';
      },
      error: () => {
        this.candidatureUrl = '/recrutement/sessions';
      }
    });
  }

  private startNotificationPolling(): void {
    this.pollingSub = interval(30000).pipe(
      startWith(0),
      switchMap(() => this.recrutementService.getNotifications())
    ).subscribe({
      next: (notifs) => {
        this.notifications = notifs;
        this.nbNonLues = notifs.filter(n => !n.lu).length;
      }
    });
  }

  get userInitials(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
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

  /** Formatte la date relative : "il y a 2 min", "il y a 1 jour" */
  getRelativeTime(dateStr: string): string {
    const now = new Date();
    const date = new Date(dateStr);
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return "À l'instant";
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return `Il y a ${diffHours}h`;
    const diffDays = Math.floor(diffHours / 24);
    return `Il y a ${diffDays} jour${diffDays > 1 ? 's' : ''}`;
  }

  getNotifIcon(type: string): string {
    switch (type) {
      case 'STATUT_CHANGE': return 'sync';
      case 'TEST_CONVOCATION': return 'quiz';
      case 'ENTRETIEN_CONVOCATION': return 'groups';
      case 'DOCUMENT': return 'upload_file';
      case 'PAIEMENT': return 'payments';
      case 'ADMISSION': return 'school';
      default: return 'notifications';
    }
  }

  toggleDropdown(): void {
    this.notifDropdownOpen = false;
    this.dropdownOpen = !this.dropdownOpen;
  }

  toggleNotifDropdown(): void {
    this.dropdownOpen = false;
    this.notifDropdownOpen = !this.notifDropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  closeNotifDropdown(): void {
    this.notifDropdownOpen = false;
  }

  onNotificationClick(notif: NotificationCandidatResponse): void {
    if (!notif.lu) {
      this.recrutementService.marquerNotificationLue(notif.id).subscribe(() => {
        this.nbNonLues = Math.max(0, this.nbNonLues - 1);
      });
      notif.lu = true;
    }
    this.notifDropdownOpen = false;
    this.router.navigate([notif.lien || this.candidatureUrl]);
  }

  marquerToutLu(): void {
    this.recrutementService.marquerToutesNotificationsLues().subscribe(() => {
      this.notifications.forEach(n => n.lu = true);
      this.nbNonLues = 0;
    });
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.dropdownOpen = false;
        this.notifDropdownOpen = false;
        this.router.navigate(['/accueil']);
      },
      error: () => {
        this.auth.clearSession();
        this.dropdownOpen = false;
        this.notifDropdownOpen = false;
        this.router.navigate(['/accueil']);
      },
    });
  }

  goToDashboard(): void {
    this.dropdownOpen = false;
    this.router.navigate([this.candidatureUrl]);
  }
}
