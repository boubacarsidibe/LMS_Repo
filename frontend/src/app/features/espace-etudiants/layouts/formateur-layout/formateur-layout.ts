import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Auth } from '../../../../core/auth/services/authService';
import { EtudiantService } from '../../services/etudiant.service';
import { CoursService } from '../../services/cours.service';
import { SupportService } from '../../services/support.service';
import { Etudiant } from '../../models/etudiant';
import { Cours } from '../../models/cours';
import { Support } from '../../models/support';
import { LogoInpgComponent } from '../../../../shared/components/logo-inpg/logo-inpg.component';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-formateur-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet, LogoInpgComponent],
  templateUrl: './formateur-layout.html',
  styleUrl: './formateur-layout.css',
})
export class FormateurLayout implements OnInit {
  notificationOpen = false;
  profileOpen = false;
  mobileMenuOpen = false;

  searchQuery = '';
  searchOpen = false;
  private tousEtudiants: Etudiant[] = [];
  private tousCours: Cours[] = [];
  private tousSupports: Support[] = [];
  resultatsEtudiants: Etudiant[] = [];
  resultatsCours: Cours[] = [];
  resultatsSupports: Support[] = [];

  navItems: NavItem[] = [
    { label: "Vue d'ensemble", icon: 'dashboard', route: 'dashboard-formateur' },
    { label: 'Mes cours', icon: 'menu_book', route: 'mes-cours' },
    { label: 'Suivi étudiants', icon: 'analytics', route: 'suivi-etudiants' },
    { label: 'Planning', icon: 'calendar_today', route: 'planning-calendrier' },
    { label: 'Forum', icon: 'forum', route: 'forum' },
    { label: 'Messagerie', icon: 'chat', route: 'messagerie' },
    { label: 'Retours & évaluations', icon: 'assignment_turned_in', route: 'retours-evaluations' },
    { label: 'Annonces', icon: 'campaign', route: 'annonces' },
    { label: 'Photothèque', icon: 'photo_library', route: 'phototheque' },
  ];

  constructor(
    private router: Router,
    private auth: Auth,
    private etudiantService: EtudiantService,
    private coursService: CoursService,
    private supportService: SupportService,
  ) {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        this.mobileMenuOpen = false;
      });
  }

  ngOnInit(): void {
    this.etudiantService.getAll().subscribe({ next: (data) => (this.tousEtudiants = data), error: () => {} });
    this.coursService.getAll().subscribe({ next: (data) => (this.tousCours = data), error: () => {} });
    this.supportService.getMesSupports().subscribe({ next: (data) => (this.tousSupports = data), error: () => {} });
  }

  onSearchInput(): void {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      this.searchOpen = false;
      this.resultatsEtudiants = [];
      this.resultatsCours = [];
      this.resultatsSupports = [];
      return;
    }
    this.resultatsEtudiants = this.tousEtudiants.filter(e => e.nom.toLowerCase().includes(q)).slice(0, 5);
    this.resultatsCours = this.tousCours
      .filter(c => c.titre.toLowerCase().includes(q) || c.code.toLowerCase().includes(q))
      .slice(0, 5);
    this.resultatsSupports = this.tousSupports
      .filter(s => (s.titre ?? '').toLowerCase().includes(q))
      .slice(0, 5);
    this.searchOpen = true;
  }

  get aucunResultat(): boolean {
    return this.resultatsEtudiants.length === 0 && this.resultatsCours.length === 0 && this.resultatsSupports.length === 0;
  }

  allerVersEtudiant(etudiant: Etudiant): void {
    this.fermerRecherche();
    this.router.navigate(['/espace-etudiant/formateur/profil-etudiant', etudiant.id]);
  }

  allerVersCoursEtFichiers(): void {
    this.fermerRecherche();
    this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
  }

  private fermerRecherche(): void {
    this.searchOpen = false;
    this.searchQuery = '';
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

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (
      target.closest('#notification-btn') ||
      target.closest('#profile-btn') ||
      target.closest('#mobile-menu-btn') ||
      target.closest('#search-input') ||
      target.closest('#search-results')
    ) {
      return;
    }
    if (
      target.closest('#notification-dropdown') ||
      target.closest('#profile-dropdown') ||
      target.closest('#mobile-nav-menu')
    ) {
      return;
    }
    this.closeAll();
    this.mobileMenuOpen = false;
    this.searchOpen = false;
  }
}
