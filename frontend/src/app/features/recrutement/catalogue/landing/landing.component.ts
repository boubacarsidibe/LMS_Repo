import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { Program } from '../../models/program';
import { Auth } from '../../../../core/auth/services/authService';
import { ProfilCompte } from '../../../../core/auth/models/user.model';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { RecrutementModule } from '../../recrutement.module';

interface ActionAccueil {
  libelle: string;
  icone: string;
  lien: string;
}

interface AccueilProfil {
  surtitre: string;
  accroche: string;
  /** La première est l'action principale (bouton plein), les suivantes sont secondaires. */
  actions: ActionAccueil[];
}

/**
 * Ce que chaque type de compte vient chercher ici. Les boutons génériques de
 * l'accueil (« Découvrir les formations », « Postuler ») parlent aux candidats
 * étudiants : une école, une entreprise ou un candidat formateur connecté n'y
 * trouvait rien qui mène à son espace.
 */
const ACCUEIL_PAR_PROFIL: Record<ProfilCompte, AccueilProfil> = {
  ecole: {
    surtitre: 'Espace école de formation',
    accroche: "Faites accréditer vos programmes par l'INPG et suivez votre demande, du dépôt jusqu'à la décision.",
    actions: [
      { libelle: "Ma demande d'accréditation", icone: 'workspace_premium', lien: '/accreditation' },
      { libelle: 'Prendre rendez-vous', icone: 'event', lien: '/accreditation/rendez-vous' },
      { libelle: 'Voir les écoles accréditées', icone: 'verified', lien: '/ecoles-accreditees' },
    ],
  },
  entreprise: {
    surtitre: 'Espace entreprise partenaire',
    accroche: "Exprimez vos besoins de formation, demandez un devis sur mesure et échangez avec l'équipe pédagogique.",
    actions: [
      // Vers l'accueil de l'espace (tuiles, dernières demandes), pas directement
      // le formulaire : une entreprise qui revient consulter le suivi de ses
      // dossiers tombait sinon toujours sur une page vierge malgré l'ajout
      // d'une vue d'ensemble propre à l'espace lui-même.
      { libelle: 'Accéder à mon espace', icone: 'business_center', lien: '/espace-entreprise' },
      { libelle: 'Déposer une demande', icone: 'request_quote', lien: '/espace-entreprise/nouvelle-demande' },
      { libelle: 'Messagerie', icone: 'forum', lien: '/espace-entreprise/messagerie' },
      { libelle: 'Rendez-vous', icone: 'event', lien: '/espace-entreprise/rendez-vous' },
    ],
  },
  formateur: {
    surtitre: 'Espace candidat formateur',
    accroche: "Rejoignez l'équipe enseignante de l'INPG : consultez les offres de recrutement et suivez votre candidature.",
    actions: [
      { libelle: 'Voir les offres de recrutement', icone: 'work', lien: '/offres-formateurs' },
      { libelle: 'Ma candidature', icone: 'assignment_ind', lien: '/candidature-formateur' },
    ],
  },
};

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NavbarComponent,
    FooterComponent,
    RecrutementModule
  ],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements OnInit {
  /** Programmes mis en avant, issus du catalogue réel et non plus d'un fichier figé. */
  programmesPhares: Program[] = [];

  private static readonly NB_PHARES = 6;

  private auth = inject(Auth);

  /** Mêmes styles que les boutons génériques du héros (plein / à contour). */
  readonly classeActionPrincipale =
    'cursor-pointer inline-flex items-center gap-2 px-6 py-3 bg-primary text-on-primary text-sm leading-4 tracking-[0.01em] font-medium rounded-lg shadow-sm border-t border-primary-fixed-dim hover:brightness-90 transition-all no-underline';
  readonly classeActionSecondaire =
    'cursor-pointer inline-flex items-center gap-2 px-6 py-3 bg-white text-on-surface text-sm leading-4 tracking-[0.01em] font-medium rounded-lg border border-outline-variant shadow-sm hover:bg-surface-container-low transition-all no-underline';

  constructor(private router: Router, private recrutementService: RecrutementService) {}

  /** Accueil propre au type du compte connecté (école, entreprise, candidat formateur), sinon null. */
  get accueilProfil(): AccueilProfil | null {
    const profil = this.auth.getProfilCompte();
    if (!profil) {
      return null;
    }
    const accueil = ACCUEIL_PAR_PROFIL[profil];
    if (profil === 'entreprise' && !this.auth.getUserRoles().includes('ROLE_ENTREPRISE')) {
      // ROLE_ENTREPRISE n'est accordé qu'à la première demande réellement soumise
      // (EntrepriseController.soumettreDemande) : avant ça, la fiche Entreprise
      // n'existe pas encore et l'envoi d'un message échoue en 403 (même correctif
      // que sur le menu du profil, navbar.component.ts).
      return { ...accueil, actions: accueil.actions.filter(a => a.libelle !== 'Messagerie') };
    }
    return accueil;
  }

  /**
   * Jeton présent mais compte pas encore chargé (rechargement de page) : on ne
   * sait pas encore quels boutons montrer. Les masquer évite d'afficher un
   * instant ceux des candidats à une école ou une entreprise.
   */
  get profilEnCours(): boolean {
    return this.auth.isLoggedIn() && !this.auth.currentUser();
  }

  ngOnInit(): void {
    this.recrutementService.getFormationsPubliques().subscribe({
      next: (formations) => {
        this.programmesPhares = formations.slice(0, LandingComponent.NB_PHARES).map((f) => ({
          id: f.id,
          titre: f.intitule,
          description: f.description ?? '',
          imageUrl: f.imageUrl ?? '',
          niveau: f.niveau ?? '',
          duree: f.duree ?? '',
        }));
      },
      // La page d'accueil reste consultable même si le catalogue est indisponible :
      // la section des programmes est simplement vide.
      error: () => (this.programmesPhares = []),
    });
  }

  goToCatalogue(): void {
    sessionStorage.removeItem('catalogue_current_page');
    this.router.navigate(['/recrutement/catalogue']);
  }
}