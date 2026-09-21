import { Component, OnInit, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { Auth } from '../../../core/auth/services/authService';
import { ProfilCompte } from '../../../core/auth/models/user.model';
import { RecrutementService, CandidatureResponse, NotificationCandidatResponse } from '../../../core/recrutement/services/recrutement.service';
import { ContexteNavbarService } from './contexte-navbar.service';

/** Une entrée du menu du cercle de profil (ou un onglet du centre du navbar). */
interface EntreeNavigation {
  libelle: string;
  icone: string;
  lien: string;
}

/**
 * Le seul onglet qui change d'un public à l'autre, en troisième position (entre
 * « Formations » et « Contact ») : celui qui mène à ce que le public vient
 * chercher ici. Constantes, pas des objets recréés à chaque lecture : le
 * gabarit les lit à chaque cycle de détection.
 */
const ONGLET_PAR_CONTEXTE: Record<ProfilCompte | 'etudiant', Omit<EntreeNavigation, 'icone'>> = {
  etudiant: { libelle: 'Sessions ouvertes', lien: '/recrutement/sessions' },
  formateur: { libelle: 'Offres de recrutement', lien: '/offres-formateurs' },
  ecole: { libelle: 'Accréditation', lien: '/accreditation' },
  entreprise: { libelle: 'Espace entreprise', lien: '/espace-entreprise' },
};

/** Menu du cercle de profil des comptes qui ont leur propre espace. */
const MENU_PAR_PROFIL: Record<ProfilCompte, EntreeNavigation[]> = {
  ecole: [
    { libelle: 'Mon espace', icone: 'school', lien: '/accreditation' },
    { libelle: 'Mon profil', icone: 'account_circle', lien: '/accreditation/mon-profil' },
  ],
  entreprise: [
    { libelle: 'Mon espace', icone: 'business_center', lien: '/espace-entreprise' },
    { libelle: 'Mon profil', icone: 'account_circle', lien: '/espace-entreprise/mon-profil' },
  ],
  formateur: [
    { libelle: 'Ma candidature', icone: 'assignment_ind', lien: '/candidature-formateur' },
    { libelle: 'Mon profil', icone: 'account_circle', lien: '/candidature-formateur/mon-profil' },
  ],
};

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
  private contexteService = inject(ContexteNavbarService);

  /**
   * Ce navbar est partagé par le catalogue de formations (recrutement étudiant)
   * ET par la vitrine publique des offres de recrutement formateur — deux
   * publics différents. Lu depuis {@link ContexteNavbarService} plutôt qu'un
   * @Input() désormais : /institut et /contact sont des destinations
   * communes aux deux parcours, où aucune page ne sait par elle-même
   * "repasser" la bonne valeur à ce composant qui, avant le passage à un
   * layout persistant, se recréait de toute façon à chaque navigation.
   * 'etudiant' par défaut ; 'formateur' tant qu'un visiteur consulte les
   * offres de recrutement (mis à jour par offres-publiques.ts et par les
   * pages étudiantes qui ramènent la valeur inverse).
   *
   * <p>Le type du compte prime sur la page : une école, une entreprise ou un
   * candidat formateur garde ses onglets partout — rechargement direct sur
   * /institut compris — et jamais ceux des admissions étudiantes, même tant
   * qu'il n'a rien soumis (voir {@link Auth.getProfilCompte}).
   */
  get contexte(): ProfilCompte | 'etudiant' {
    return this.profil ?? this.contexteService.contexte();
  }

  /** Type du compte connecté ; null pour un candidat/étudiant, le personnel ou un visiteur. */
  get profil(): ProfilCompte | null {
    return this.auth.getProfilCompte();
  }

  /** Onglet propre au public, en troisième position du centre du navbar. */
  get ongletSpecifique(): Omit<EntreeNavigation, 'icone'> {
    return ONGLET_PAR_CONTEXTE[this.contexte];
  }

  /**
   * Menu du cercle de profil, selon le type du compte. Il suit le compte, pas la
   * page : un étudiant qui consulte les offres de recrutement garde SON menu.
   * `computed` plutôt qu'un getter : le tableau doit garder la même référence
   * d'un cycle de détection à l'autre, sans quoi Angular signale, en mode
   * développement, une valeur « modifiée après vérification ».
   */
  readonly entreesMenu = computed<EntreeNavigation[]>(() => {
    const profil = this.auth.getProfilCompte();
    if (profil) {
      return MENU_PAR_PROFIL[profil];
    }
    return [
      { libelle: 'Tableau de bord', icone: 'dashboard', lien: '/recrutement/catalogue' },
      { libelle: 'Mes candidatures', icone: 'description', lien: this.candidatureUrl() },
    ];
  });

  dropdownOpen = false;
  notifDropdownOpen = false;
  mobileMenuOpen = false;
  userReady = false;
  hasCandidature = false;
  candidatureUrl = signal('/recrutement/sessions');

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
        this.mobileMenuOpen = false;
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
          // Notifications et candidatures étudiantes : sans objet pour une école,
          // une entreprise ou un candidat formateur (autres parcours, autres endpoints).
          if (!this.profil) {
            this.checkCandidatures();
            this.startNotificationPolling();
          }
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
        // Sans candidature existante, on ne peut pas postuler sans avoir d'abord choisi
        // une session : on renvoie vers la liste des sessions plutôt que le formulaire seul.
        this.candidatureUrl.set(this.hasCandidature ? '/recrutement/suivi' : '/recrutement/sessions');
      },
      error: () => {
        this.hasCandidature = false;
        this.candidatureUrl.set('/recrutement/sessions');
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

  getUserRoleLabel(): string {
    // Le type du compte d'abord : tant qu'aucun rôle métier n'est accordé (rien
    // encore soumis), la liste des rôles est vide et le libellé restait vide.
    switch (this.profil) {
      case 'ecole': return 'ÉCOLE DE FORMATION';
      case 'entreprise': return 'ENTREPRISE PARTENAIRE';
      case 'formateur': return 'CANDIDAT FORMATEUR';
    }
    const roles = this.auth.getUserRoles();
    if (!roles || roles.length === 0) return '';
    const r = roles[0];
    switch (r) {
      case 'ROLE_RESPONSABLE_ADMISSION': return 'RESPONSABLE ADMISSION';
      case 'ROLE_RESPONSABLE_PEDAGOGIQUE': return 'RESPONSABLE PÉDAGOGIQUE';
      case 'ROLE_CANDIDAT': return 'CANDIDAT';
      case 'ROLE_ETUDIANT': return 'ETUDIANT';
      case 'ROLE_FORMATEUR': return 'FORMATEUR';
      case 'ROLE_EVALUATEUR_ACCREDITATION': return 'ÉVALUATEUR ACCRÉDITATION';
      default: return r.replace('ROLE_', '');
    }
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
    this.mobileMenuOpen = false;
    this.dropdownOpen = !this.dropdownOpen;
  }

  toggleNotifDropdown(): void {
    this.dropdownOpen = false;
    this.mobileMenuOpen = false;
    this.notifDropdownOpen = !this.notifDropdownOpen;
  }

  toggleMobileMenu(): void {
    this.dropdownOpen = false;
    this.notifDropdownOpen = false;
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  closeNotifDropdown(): void {
    this.notifDropdownOpen = false;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
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
}