import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Auth } from '../../../../core/auth/services/authService';
import { RecrutementService, SessionCandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  filled?: boolean;
}

interface Alerte {
  icone: string;
  message: string;
  route: string;
}

@Component({
  selector: 'app-responsable-admission-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './responsable-admission-layout.component.html',
})
export class ResponsableAdmissionLayoutComponent implements OnInit {
  notificationOpen = false;
  profileOpen = false;
  sidebarMobileOpen = false;
  activeRoute = '';

  private candidaturesSoumises = 0;
  private sessionsBientotFermees: SessionCandidatureResponse[] = [];

  navItems: NavItem[] = [
    { label: "Vue d'ensemble", icon: 'dashboard', route: '/responsable-admission' },
    { label: 'Statistiques', icon: 'bar_chart', route: '/responsable-admission/statistiques' },
    { label: 'Sessions', icon: 'calendar_today', route: '/responsable-admission/sessions' },
    { label: 'Candidatures', icon: 'folder_shared', route: '/responsable-admission/candidatures' },
    { label: 'Critères auto', icon: 'filter_alt', route: '/responsable-admission/criteres' },
    { label: 'Tests Académiques', icon: 'quiz', route: '/responsable-admission/tests' },
    { label: 'Entretiens', icon: 'groups', route: '/responsable-admission/entretiens' },
    { label: 'Délibération', icon: 'gavel', route: '/responsable-admission/deliberation', filled: true },
  ];

  constructor(
    private router: Router,
    private auth: Auth,
    private recrutementService: RecrutementService
  ) {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        this.activeRoute = e.urlAfterRedirects;
        this.sidebarMobileOpen = false;
      });
  }

  ngOnInit(): void {
    // Il n'existe pas de système de notifications admin persistées côté backend :
    // ces alertes sont calculées à partir des vraies candidatures/sessions déjà
    // exposées par l'API, pas des notifications stockées.
    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => {
        this.candidaturesSoumises = (data || []).filter((c) => c.statut === 'SOUMIS').length;
      },
      error: () => {},
    });
    this.recrutementService.getAllSessions().subscribe({
      next: (data) => {
        const aujourdhui = new Date().toISOString().slice(0, 10);
        const dansSeptJours = new Date();
        dansSeptJours.setDate(dansSeptJours.getDate() + 7);
        const limite = dansSeptJours.toISOString().slice(0, 10);
        this.sessionsBientotFermees = (data || []).filter(
          (s) => s.estOuverte && s.dateFermeture >= aujourdhui && s.dateFermeture <= limite
        );
      },
      error: () => {},
    });
  }

  get alertes(): Alerte[] {
    const items: Alerte[] = [];
    if (this.candidaturesSoumises > 0) {
      items.push({
        icone: 'inbox',
        message: `${this.candidaturesSoumises} candidature(s) en attente de traitement`,
        route: '/responsable-admission/candidatures',
      });
    }
    for (const s of this.sessionsBientotFermees) {
      items.push({
        icone: 'timer',
        message: `« ${s.libelle} » ferme le ${new Date(s.dateFermeture).toLocaleDateString('fr-FR')}`,
        route: '/responsable-admission/sessions',
      });
    }
    return items;
  }

  isActive(route: string): boolean {
    if (route === '/responsable-admission') {
      return this.activeRoute === '/responsable-admission';
    }
    return this.activeRoute.startsWith(route);
  }

  get userInitials(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
    const parts = (user as any).username ? (user as any).username.split('_') : [user.email.charAt(0)];
    return parts.length >= 2
      ? (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
      : (parts[0].charAt(0) + (parts[0].charAt(1) || '')).toUpperCase();
  }

  get userDisplayName(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
    return (user as any).username ? (user as any).username.replace('_', ' ') : user.email;
  }

  get userEmail(): string {
    return this.auth.currentUser()?.email || '';
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.closeAll();
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.auth.clearSession();
        this.closeAll();
        this.router.navigate(['/auth/login']);
      },
    });
  }

  private closeAll(): void {
    this.notificationOpen = false;
    this.profileOpen = false;
  }

  toggleDropdown(type: 'notification' | 'profile'): void {
    if (type === 'notification') {
      const wasOpen = this.notificationOpen;
      this.closeAll();
      this.notificationOpen = !wasOpen;
    } else {
      const wasOpen = this.profileOpen;
      this.closeAll();
      this.profileOpen = !wasOpen;
    }
  }

  toggleSidebarMobile(): void {
    this.sidebarMobileOpen = !this.sidebarMobileOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (
      target.closest('#notification-btn') ||
      target.closest('#profile-btn') ||
      target.closest('#mobile-menu-btn')
    ) {
      return;
    }
    if (
      target.closest('#notification-dropdown') ||
      target.closest('#profile-dropdown') ||
      target.closest('#mobile-sidebar')
    ) {
      return;
    }
    this.closeAll();
    this.sidebarMobileOpen = false;
  }
}
