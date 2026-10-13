import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ContexteNavbarService } from '../../../../shared/components/navbar/contexte-navbar.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';

/**
 * Cadre de l'espace Candidat formateur (procédure QMS-007 §3).
 *
 * <p>Même cadre que les pages publiques et que l'espace du candidat étudiant : le menu partagé
 * `app-navbar` (Institut, Formations, Offres de recrutement, Contact), le contexte « formateur » pour son
 * onglet propre, et le pied de page. Le cercle de profil mène à « Ma candidature » et « Mon profil ».
 *
 * <p>Cet espace avait une barre latérale à lui (texte « INPG LMS », entrées « Ma candidature » et « Offres de
 * recrutement »), propre à cet espace seul et différente de tout ce que le candidat voit avant et après :
 * le dossier de candidature, façon portail de recrutement de référence, s'ouvrait dans un cadre d'une autre époque.
 * Elle est remplacée par le cadre commun, comme sur la branche d'origine du dossier enrichi.
 */
@Component({
  selector: 'app-candidat-formateur-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  template: `
    <div class="min-h-screen flex flex-col bg-surface">
      <app-navbar></app-navbar>
      <main class="flex-grow bg-background">
        <router-outlet></router-outlet>
      </main>
      <app-footer></app-footer>
    </div>
  `,
})
export class CandidatFormateurLayout implements OnInit {
  private contexteNavbar = inject(ContexteNavbarService);

  ngOnInit(): void {
    this.contexteNavbar.contexte.set('formateur');
  }
}
