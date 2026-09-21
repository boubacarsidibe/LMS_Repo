import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Cadre de l'espace École de formation : le fond de page, rien de plus.
 *
 * <p>Cet espace est déclaré sous `PublicNavbarLayoutComponent` (voir
 * app.routes.ts), dont l'unique `app-navbar` reste en place d'une page à l'autre.
 * Il portait auparavant sa propre barre, différente de celle du site : passer de
 * « Institut » à « Accréditation » remplaçait d'un coup tous les onglets
 * (retour de recette du 2026-09-21). L'espace de l'école s'atteint désormais par
 * l'onglet « Accréditation » et par le menu du cercle de profil du navbar commun.
 */
@Component({
  selector: 'app-ecole-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <main class="min-h-screen bg-background">
      <router-outlet></router-outlet>
    </main>
  `,
})
export class EcoleLayout {}
