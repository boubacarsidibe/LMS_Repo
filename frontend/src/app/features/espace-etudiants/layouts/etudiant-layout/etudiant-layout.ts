import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import { Auth } from '../../../../core/auth/services/authService';
import { EtudiantProfile, EtudiantService } from '../../services/etudiant.service';
import { CommunicationService } from '../../services/communication.service';
import { ScolariteService } from '../../services/scolarite.service';

interface EntreeNavigation {
  libelle: string;
  lien: string;
  icone: string;
}

/**
 * Cadre de l'espace étudiant : navigation latérale et en-tête.
 *
 * <p>L'espace comptant désormais neuf rubriques, la barre horizontale précédente
 * ne suffisait plus. Ce cadre reprend la structure de l'espace formateur pour
 * que les deux espaces se ressemblent.
 */
@Component({
  selector: 'app-etudiant-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './etudiant-layout.html',
})
export class EtudiantLayout implements OnInit, OnDestroy {
  profile: EtudiantProfile | null = null;
  matricule = '';
  promotionLibelle = '';
  chargement = true;
  nbAnnoncesNonLues = 0;

  readonly navigation: EntreeNavigation[] = [
    { libelle: 'Tableau de bord', lien: 'dashboard', icone: 'dashboard' },
    { libelle: 'Mes cours', lien: 'mes-cours', icone: 'menu_book' },
    { libelle: 'Calendrier', lien: 'calendrier', icone: 'calendar_today' },
    { libelle: 'Annonces', lien: 'annonces', icone: 'campaign' },
    { libelle: 'Forum', lien: 'forum', icone: 'forum' },
    { libelle: 'Photothèque', lien: 'photheque', icone: 'photo_library' },
    { libelle: 'Ma promotion', lien: 'ma-promotion', icone: 'groups' },
    { libelle: 'Mes évaluations', lien: 'mes-evaluations', icone: 'reviews' },
    { libelle: 'Mon profil', lien: 'mon-profil', icone: 'account_circle' },
  ];

  private sondage?: Subscription;

  constructor(
    private etudiantService: EtudiantService,
    private scolariteService: ScolariteService,
    private communicationService: CommunicationService,
    private auth: Auth,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.etudiantService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });

    this.scolariteService.getMaPromotion().subscribe({
      next: (reponse) => {
        this.matricule = reponse.matricule;
        const promotion = reponse.promotion as { libelle?: string };
        this.promotionLibelle = promotion?.libelle ?? '';
      },
      // Un étudiant sans promotion reste un cas normal : l'en-tête affiche
      // simplement moins d'informations.
      error: () => undefined,
    });

    // Même cadence que la cloche de l'espace candidat, pour un comportement
    // homogène d'un espace à l'autre.
    this.sondage = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.communicationService.countAnnoncesNonLues())
      )
      .subscribe({
        next: (reponse) => (this.nbAnnoncesNonLues = reponse.count),
        error: () => undefined,
      });
  }

  ngOnDestroy(): void {
    this.sondage?.unsubscribe();
  }

  get nomAffiche(): string {
    if (!this.profile) {
      return 'Étudiant';
    }
    const etudiant = this.profile.etudiant;
    return etudiant ? `${etudiant.prenom} ${etudiant.nom}` : this.profile.username;
  }

  get initiales(): string {
    const etudiant = this.profile?.etudiant;
    if (!etudiant) {
      return (this.profile?.username ?? '?').charAt(0).toUpperCase();
    }
    return (etudiant.prenom.charAt(0) + etudiant.nom.charAt(0)).toUpperCase();
  }

  deconnexion(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/accueil']),
      error: () => this.router.navigate(['/accueil']),
    });
  }
}
