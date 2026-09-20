import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RecrutementService, SessionCandidatureResponse } from '../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { ContexteNavbarService } from '../../../shared/components/navbar/contexte-navbar.service';

@Component({
  selector: 'app-sessions-publiques',
  standalone: true,
  imports: [CommonModule, RouterModule, FooterComponent],
  templateUrl: './sessions-publiques.component.html',
})
export class SessionsPubliquesComponent implements OnInit {
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  errorMessage = '';
  private contexteNavbar = inject(ContexteNavbarService);

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.contexteNavbar.contexte.set('etudiant');
    this.loading = true;
    this.recrutementService.getCataloguePublic().subscribe({
      next: (data) => {
        // Le backend renvoie les sessions marquées "ouvertes" par l'admin ; on
        // vérifie en plus les dates (estActive), comme le fait déjà le formulaire
        // de candidature, pour ne jamais proposer une session en réalité expirée.
        this.sessions = (data || []).filter((s) => s.estActive);
        this.loading = false;
      },
      error: () => {
        this.sessions = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger les sessions pour le moment.';
      },
    });
  }

  joursRestants(dateFermeture: string): number {
    const fin = new Date(dateFermeture);
    const maintenant = new Date();
    return Math.max(0, Math.ceil((fin.getTime() - maintenant.getTime()) / (1000 * 60 * 60 * 24)));
  }

  bientotFermee(s: SessionCandidatureResponse): boolean {
    return this.joursRestants(s.dateFermeture) <= 7;
  }
}
