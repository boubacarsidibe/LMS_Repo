import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { Auth } from '../../../../core/auth/services/authService';

@Component({
  selector: 'app-institut',
  standalone: true,
  imports: [CommonModule, RouterModule, FooterComponent],
  templateUrl: './institut.component.html',
})
export class InstitutComponent {
  private auth = inject(Auth);

  /**
   * École, entreprise ou candidat formateur connecté : les CTA pensés pour un
   * candidat étudiant (Postuler, dossier de candidature, rejoindre comme
   * formateur) ne le concernent pas — même logique que {@code landing.component}.
   */
  get estCompteOrganisation(): boolean {
    return this.auth.getProfilCompte() !== null;
  }
}