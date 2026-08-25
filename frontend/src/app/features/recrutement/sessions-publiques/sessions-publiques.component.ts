import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RecrutementService, SessionCandidatureResponse } from '../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-sessions-publiques',
  standalone: true,
  imports: [CommonModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './sessions-publiques.component.html',
})
export class SessionsPubliquesComponent implements OnInit {
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  errorMessage = '';

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
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
