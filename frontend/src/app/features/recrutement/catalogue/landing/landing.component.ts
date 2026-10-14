import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { Program } from '../../models/program';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { Auth } from '../../../../core/auth/services/authService';
import { RecrutementModule } from '../../recrutement.module';

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

  /** Page réservée aux comptes connectés (`authGuard`) ; la connexion y ramène un visiteur (`?retour=`). */
  readonly lienEcolesAccreditees = '/ecoles-accreditees';

  constructor(
    private router: Router,
    private recrutementService: RecrutementService,
    private auth: Auth,
  ) {}

  /** Même définition que le navbar : un jeton et un compte chargé. */
  get connecte(): boolean {
    return this.auth.isLoggedIn() && this.auth.currentUser() != null;
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