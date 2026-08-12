import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs/operators';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface Notification {
  id: number;
  message: string;
  type: 'admission' | 'success';
  time: string;
  read: boolean;
}

@Component({
  selector: 'app-responsable-admission-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission-layout.component.html',
})
export class ResponsableAdmissionLayoutComponent {
  searchQuery = '';
  selectedAnneeAcademique = '2026-2027';
  notificationOpen = false;
  toastVisible = false;
  toastMessage = '';
  activeRoute = '';

  navItems: NavItem[] = [
    { label: 'Vue d\'Ensemble', icon: 'dashboard', route: '/responsable-admission' },
    { label: 'Sessions', icon: 'event', route: '/responsable-admission/sessions' },
    { label: 'Candidatures', icon: 'description', route: '/responsable-admission/candidatures' },
    { label: 'Tests Académiques', icon: 'school', route: '/responsable-admission/tests' },
    { label: 'Entretiens', icon: 'groups', route: '/responsable-admission/entretiens' },
    { label: 'Statistiques', icon: 'monitoring', route: '/responsable-admission/statistiques' },
  ];

  notifications: Notification[] = [
    { id: 1, message: 'Nouveau dossier: Amadou Fall', type: 'admission', time: 'Il y a 5 min', read: false },
    { id: 2, message: 'Test complété: Fatou Diop', type: 'success', time: 'Il y a 20 min', read: false },
    { id: 3, message: 'Nouveau dossier: Ousmane Sow', type: 'admission', time: 'Il y a 1h', read: true },
  ];

  get hasUnreadNotifications(): boolean {
    return this.notifications.some(n => !n.read);
  }

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

  toggleDropdown(type: 'notification', event: MouseEvent): void {
    event.stopPropagation();
    this.notificationOpen = !this.notificationOpen;
  }

  markAllRead(): void {
    this.notifications.forEach(n => n.read = true);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.closest('#notification-btn') || target.closest('#notification-dropdown')) return;
    this.notificationOpen = false;
  }

  logout(): void {
    this.router.navigate(['/auth/login']);
  }

  showToast(message: string): void {
    this.toastMessage = message;
    this.toastVisible = true;
    setTimeout(() => { this.toastVisible = false; }, 3000);
  }
}
