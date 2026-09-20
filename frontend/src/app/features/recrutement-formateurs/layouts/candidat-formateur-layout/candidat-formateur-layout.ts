import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ContexteNavbarService } from '../../../../shared/components/navbar/contexte-navbar.service';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';

/**
 * Cadre de l'espace Candidat formateur (procédure QMS-007 §3).
 *
 * <p>Réutilise le navbar partagé (`app-navbar`, contexte « formateur ») au
 * lieu d'une barre propre : retour de recette — le candidat formateur ne
 * voyait plus que « Accueil » et « Offres de recrutement » une fois dans
 * son espace, alors que les onglets doivent rester identiques tout au long
 * de son parcours (Institut, Formations, Offres de recrutement, Contact),
 * comme pour l'étudiant qui candidate. Le menu utilisateur du navbar mène à
 * « Ma candidature » et « Mon profil ».
 */
@Component({
  selector: 'app-candidat-formateur-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>
    <main class="min-h-screen bg-background">
      <router-outlet></router-outlet>
    </main>
  `,
})
export class CandidatFormateurLayout implements OnInit {
  private contexteNavbar = inject(ContexteNavbarService);

  ngOnInit(): void {
    this.contexteNavbar.contexte.set('formateur');
  }
}
