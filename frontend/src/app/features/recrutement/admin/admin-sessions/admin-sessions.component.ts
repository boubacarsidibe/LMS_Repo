import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RecrutementService, SessionCandidatureResponse, SessionCandidatureRequest } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-admin-sessions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-sessions.component.html'
})
export class AdminSessionsComponent implements OnInit {
  sessions: SessionCandidatureResponse[] = [];
  libelle = '';
  dateOuverture = '';
  dateFermeture = '';
  estOuverte = true;

  constructor(private service: RecrutementService) { }

  ngOnInit(): void {
    this.service.getAllSessions().subscribe(data => this.sessions = data);
  }

  creer(): void {
    const request: SessionCandidatureRequest = {
      libelle: this.libelle,
      dateOuverture: this.dateOuverture,
      dateFermeture: this.dateFermeture,
      estOuverte: this.estOuverte
    };
    this.service.creerSession(request).subscribe(() => {
      this.libelle = ''; this.dateOuverture = ''; this.dateFermeture = '';
      this.ngOnInit();
    });
  }

  supprimer(id: number): void {
    if (confirm('Supprimer cette session ?')) {
      this.service.supprimerSession(id).subscribe(() => this.ngOnInit());
    }
  }
}