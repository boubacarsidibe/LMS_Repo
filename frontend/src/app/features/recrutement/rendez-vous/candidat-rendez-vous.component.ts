import { Component } from '@angular/core';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { RendezVousComponent } from '../../rendez-vous/pages/rendez-vous/rendez-vous.component';

/**
 * Rendez-vous dans l'espace candidat (TDR §J, « chaque utilisateur pourra
 * [...] envoyer une demande de rendez-vous »).
 *
 * <p>Les quatre autres espaces enveloppent {@link RendezVousComponent} dans le
 * cadre de leur barre latérale, par une route enfant. L'espace candidat n'a pas
 * de cadre : chacune de ses pages compose elle-même sa barre supérieure
 * ({@code NavbarComponent}) et son pied de page — voir
 * {@code SuiviComponent}. Ce composant suit ce patron plutôt que d'introduire
 * un cadre à part pour une seule page.
 */
@Component({
  selector: 'app-candidat-rendez-vous',
  standalone: true,
  imports: [FooterComponent, RendezVousComponent],
  template: `
    <div class="min-h-screen flex flex-col bg-surface">
      <main class="flex-grow">
        <app-rendez-vous></app-rendez-vous>
      </main>
      <app-footer></app-footer>
    </div>
  `,
})
export class CandidatRendezVousComponent {}
