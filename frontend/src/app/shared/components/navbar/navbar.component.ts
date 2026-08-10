import { Component, OnInit, OnDestroy, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { Auth } from '../../../core/auth/services/authService';
import { RecrutementService, CandidatureResponse, NotificationCandidatResponse } from '../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
})
export class NavbarComponent implements OnInit, OnDestroy {
  private auth = inject(Auth);
  private router = inject(Router);
  private recrutementService = inject(RecrutementService);

  dropdownOpen = false;
  notifDropdownOpen = false;
  userReady = false;
  hasCandidature = false;
  candidatureUrl = '/recrutement/candidature';

  // Notifications
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
        this.hasCandidature = soumises.length > 0;
        this.candidatureUrl = this.hasCandidature ? '/recrutement/suivi' : '/recrutement/candidature';
      },
      error: () => {
        this.hasCandidature = false;
        this.candidatureUrl = '/recrutement/candidature';
      }
    });
  }

  private startNotificationPolling(): void {
    // Poll toutes les 30 secondes
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

  get isLoggedIn(): boolean {
    return this.auth.isLoggedIn() && this.auth.currentUser() != null;
  }

  get isResponsableAdmissionRole(): boolean {
    return this.auth.isResponsableAdmission();
  }

  get isEtudiantRole(): boolean {
    const roles = this.auth.getUserRoles();
    return roles.includes('ROLE_ETUDIANT');
  }

  get isFormateurRole(): boolean {
    const roles = this.auth.getUserRoles();
    return roles.includes('ROLE_FORMATEUR');
  }

  get isCandidatRole(): boolean {
    const roles = this.auth.getUserRoles();
    return roles.includes('ROLE_CANDIDAT');
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

  /** Retourne l'icône material selon le type de notification */
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
    // Marquer comme lue
    if (!notif.lu) {
      this.recrutementService.marquerNotificationLue(notif.id).subscribe(() => {
        this.nbNonLues = Math.max(0, this.nbNonLues - 1);
      });
      notif.lu = true;
    }
    this.notifDropdownOpen = false;
    // Rediriger vers le lien si présent
    if (notif.lien) {
      this.router.navigate([notif.lien]);
    } else {
      this.router.navigate(['/recrutement/suivi']);
    }
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
    this.router.navigate(['/recrutement/catalogue']);
  }

  goToMesCandidatures(): void {
    this.dropdownOpen = false;
    this.router.navigate([this.candidatureUrl]);
  }
}