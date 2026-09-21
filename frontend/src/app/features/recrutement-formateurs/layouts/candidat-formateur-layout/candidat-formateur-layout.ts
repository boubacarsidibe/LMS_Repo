import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ContexteNavbarService } from '../../../../shared/components/navbar/contexte-navbar.service';

/**
 * Cadre de l'espace Candidat formateur (procédure QMS-007 §3) : le fond de page.
 *
 * <p>Cet espace est déclaré sous `PublicNavbarLayoutComponent` (voir
 * app.routes.ts) : le même `app-navbar` reste en place quand on passe de la
 * vitrine des offres à « Ma candidature » — mêmes onglets (Institut, Formations,
 * Offres de recrutement, Contact) tout au long du parcours, comme pour l'étudiant
 * qui candidate. Le menu du cercle de profil mène à « Ma candidature » et
 * « Mon profil ».
 */
@Component({
  selector: 'app-candidat-formateur-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
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
