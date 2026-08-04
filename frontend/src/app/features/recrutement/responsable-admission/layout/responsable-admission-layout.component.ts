import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  filled?: boolean;
}

@Component({
  selector: 'app-responsable-admission-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './responsable-admission-layout.component.html',
})
export class ResponsableAdmissionLayoutComponent {
  notificationOpen = false;
  appsOpen = false;
  profileOpen = false;
  activeRoute = '';

  navItems: NavItem[] = [
    { label: 'Vue d\'ensemble', icon: 'dashboard', route: '/responsable-admission' },
    { label: 'Sessions', icon: 'calendar_today', route: '/responsable-admission/sessions' },
    { label: 'Candidatures', icon: 'folder_shared', route: '/responsable-admission/candidatures' },
    { label: 'Entretiens', icon: 'groups', route: '/responsable-admission/entretiens' },
    { label: 'Délibération', icon: 'gavel', route: '/responsable-admission/deliberation', filled: true },
    { label: 'Statistiques', icon: 'analytics', route: '/responsable-admission/statistiques' },
  ];

  constructor(private router: Router) {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        this.activeRoute = e.urlAfterRedirects;
      });
  }

  isActive(route: string): boolean {
    if (route === '/responsable-admission') {
      return this.activeRoute === '/responsable-admission';
    }
    return this.activeRoute.startsWith(route);
  }

  private closeAll(): void {
    this.notificationOpen = false;
    this.appsOpen = false;
    this.profileOpen = false;
  }

  toggleDropdown(type: 'notification' | 'apps' | 'profile'): void {
    if (type === 'notification') {
      const wasOpen = this.notificationOpen;
      this.closeAll();
      this.notificationOpen = !wasOpen;
    } else if (type === 'apps') {
      const wasOpen = this.appsOpen;
      this.closeAll();
      this.appsOpen = !wasOpen;
    } else if (type === 'profile') {
      const wasOpen = this.profileOpen;
      this.closeAll();
      this.profileOpen = !wasOpen;
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (
      target.closest('#notification-btn') ||
      target.closest('#apps-btn') ||
      target.closest('#profile-btn')
    ) {
      return;
    }
    if (
      target.closest('#notification-dropdown') ||
      target.closest('#apps-dropdown') ||
      target.closest('#profile-dropdown')
    ) {
      return;
    }
    this.closeAll();
  }
}