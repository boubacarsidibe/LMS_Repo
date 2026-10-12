import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';
import { ClocheNotificationsService, NotificationCloche } from '../../../shared/services/cloche-notifications.service';
import { LogoInpgComponent } from '../../../shared/components/logo-inpg/logo-inpg.component';

interface EntreeNavigation {
  libelle: string;
  lien: string;
  icone: string;
}

/**
 * Shell de l'espace du responsable pédagogique.
 *
 * <p>Les deux écrans de cet espace portaient chacun leur propre barre horizontale,
 * dupliquée et bornée à 1280 px : sur un écran large, les éléments de droite
 * s'arrêtaient loin du bord. Ils partagent désormais une barre latérale, comme
 * l'espace formateur, ce qui règle l'alignement et laisse la place aux rubriques
 * à venir.
 */
@Component({
  selector: 'app-pedagogie-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, LogoInpgComponent],
  templateUrl: './pedagogie-layout.component.html',
})
export class PedagogieLayoutComponent implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);
  private cloche = inject(ClocheNotificationsService);

  nomAffiche = '';
  emailAffiche = '';
  initiales = '?';
  sidebarMobileOuverte = false;
  menuProfilOuvert = false;
  menuNotificationOuvert = false;
  notifications: NotificationCloche[] = [];

  readonly navigation: EntreeNavigation[] = [
    { libelle: 'Vue d’ensemble', lien: 'vue-ensemble', icone: 'dashboard' },
    { libelle: 'Cours', lien: 'cours', icone: 'menu_book' },
    { libelle: 'Formations', lien: 'formations', icone: 'school' },
    { libelle: 'Promotions', lien: 'promotions', icone: 'groups' },
    { libelle: 'Étudiants', lien: 'etudiants', icone: 'badge' },
    { libelle: 'Formateurs', lien: 'formateurs', icone: 'co_present' },
    { libelle: 'Relevés', lien: 'releves', icone: 'description' },
    { libelle: 'Certificats', lien: 'certificats', icone: 'workspace_premium' },
    { libelle: 'Suivi qualité', lien: 'qualite', icone: 'analytics' },
    { libelle: 'Entreprises', lien: 'entreprises', icone: 'domain' },
    { libelle: 'Activités du personnel', lien: 'activites-personnel', icone: 'apartment' },
    { libelle: 'Rendez-vous', lien: 'rendez-vous', icone: 'event' },
  ];

  ngOnInit(): void {
    const utilisateur = this.auth.currentUser();
    this.nomAffiche = utilisateur?.username || 'Responsable pédagogique';
    this.emailAffiche = utilisateur?.email || '';
    this.initiales = this.calculerInitiales(this.nomAffiche);
    this.cloche.lister().subscribe({
      next: (liste) => (this.notifications = (liste || []).filter((n) => !n.lu)),
      error: () => {},
    });
  }

  /** Une seule liste déroulante ouverte à la fois, comme dans l'espace admission. */
  basculerMenu(type: 'notification' | 'profil'): void {
    const notificationEtaitOuverte = this.menuNotificationOuvert;
    const profilEtaitOuvert = this.menuProfilOuvert;
    this.fermerMenus();
    if (type === 'notification') {
      this.menuNotificationOuvert = !notificationEtaitOuverte;
    } else {
      this.menuProfilOuvert = !profilEtaitOuvert;
    }
  }

  private fermerMenus(): void {
    this.menuNotificationOuvert = false;
    this.menuProfilOuvert = false;
  }

  /** Ouvre la cible et marque la notification lue, sans quoi la cloche répète un événement traité. */
  ouvrirNotification(notification: NotificationCloche): void {
    this.cloche.marquerLue(notification.id).subscribe({
      next: () => (this.notifications = this.notifications.filter((n) => n.id !== notification.id)),
      error: () => {},
    });
    this.fermerMenus();
    if (notification.lien) {
      this.router.navigateByUrl(notification.lien);
    }
  }

  @HostListener('document:click', ['$event'])
  auClicDocument(evenement: MouseEvent): void {
    const cible = evenement.target as HTMLElement;
    if (cible.closest('#notification-btn') || cible.closest('#profile-btn')
        || cible.closest('#notification-dropdown') || cible.closest('#profile-dropdown')) {
      return;
    }
    this.fermerMenus();
  }

  private calculerInitiales(nom: string): string {
    const morceaux = nom.trim().split(/[\s_.-]+/).filter(Boolean);
    if (morceaux.length === 0) {
      return '?';
    }
    const deux = morceaux.slice(0, 2).map((m) => m[0]).join('');
    return deux.toUpperCase();
  }

  basculerSidebarMobile(): void {
    this.sidebarMobileOuverte = !this.sidebarMobileOuverte;
  }

  seDeconnecter(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => this.router.navigate(['/auth/login']),
    });
  }

  /**
   * Referme la barre latérale mobile dès qu'on choisit une rubrique. Branché sur la barre entière (délégation) :
   * un clic sur la page déjà ouverte ne déclenche aucune navigation, donc un simple écouteur de
   * `NavigationEnd` laisserait la barre ouverte devant le contenu.
   */
  fermerSidebarSiLien(evenement: Event): void {
    if ((evenement.target as HTMLElement).closest('a')) {
      this.sidebarMobileOuverte = false;
    }
  }
}
