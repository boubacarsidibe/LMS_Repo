import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Cadre de l'espace Entreprise (TDR §H) : le fond de page, rien de plus.
 *
 * <p>Cet espace est déclaré sous `PublicNavbarLayoutComponent` (voir
 * app.routes.ts), dont l'unique `app-navbar` reste en place d'une page à l'autre.
 * Il portait auparavant sa propre barre (Accueil, Catalogue des formations,
 * Messagerie), différente de celle du site : passer de « Institut » à « Espace
 * entreprise » remplaçait d'un coup tous les onglets (retour de recette du
 * 2026-09-21). L'espace s'atteint désormais par l'onglet « Espace entreprise » ;
 * la messagerie et le profil sont dans le menu du cercle de profil.
 */
@Component({
  selector: 'app-entreprise-layout',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './entreprise-layout.html',
})
export class EntrepriseLayout {}
