import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './navbar.component';

/**
 * Cadre commun aux pages publiques partageant `app-navbar` (recrutement
 * étudiant et vitrine des offres formateur, cf. ContexteNavbarService) —
 * catalogue, sessions/offres, candidature, suivi, rendez-vous, institut,
 * contact.
 *
 * <p>Avant ce layout, chaque page embarquait son propre `<app-navbar>` :
 * Angular détruisait et recréait le composant à chaque navigation entre ces
 * pages, relançant à chaque clic le chargement des notifications et de
 * l'état de candidature (`ngOnInit`/`interval` de polling repartant de zéro).
 * Visible et signalé en recette : le navbar « clignotait » à chaque clic
 * plutôt que de rester un élément stable de l'écran, alors même que son CSS
 * (`position: fixed`) l'aurait laissé immobile pendant un défilement — deux
 * choses différentes. `app-navbar`, posé ici une seule fois au-dessus d'un
 * `router-outlet`, persiste désormais tout le temps que l'utilisateur reste
 * dans l'une de ces pages.
 */
@Component({
  selector: 'app-public-navbar-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>
    <router-outlet></router-outlet>
  `,
})
export class PublicNavbarLayoutComponent {}
