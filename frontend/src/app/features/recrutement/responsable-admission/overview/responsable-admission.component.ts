import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  RecrutementService,
  SessionCandidatureResponse,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { DashboardPreferences, DashboardPreferencesService } from '../services/dashboard-preferences.service';

@Component({
  selector: 'app-responsable-admission',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './responsable-admission.component.html',
})
export class ResponsableAdmissionComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  exporting = false;
  prefs: DashboardPreferences;

  constructor(
    private recrutementService: RecrutementService,
    private prefsService: DashboardPreferencesService
  ) {
    this.prefs = this.prefsService.charger();
  }

  ngOnInit(): void {
    this.loading = true;
    // Toujours chargées : nbCandidatures() en a besoin pour le tableau des sessions actives.
    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => (this.candidatures = []),
      complete: () => (this.loading = false),
    });
    this.recrutementService.getAllSessions().subscribe({
      next: (data) => (this.sessions = data || []),
      error: () => (this.sessions = []),
    });
  }

  get sessionsActives(): SessionCandidatureResponse[] {
    return this.sessions.filter((s) => s.estActive);
  }

  /** Nombre de candidatures rattachées à une session donnée (donnée réelle, pas de quota inventé). */
  nbCandidatures(sessionId: number): number {
    return this.candidatures.filter((c) => c.sessionCandidatureId === sessionId).length;
  }

  exporterSessions(): void {
    this.exporting = true;
    this.recrutementService.exporterSessions().subscribe({
      next: (blob) => {
        downloadBlob(blob, `sessions_recrutement_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        alert("Échec de l'export Excel.");
      },
    });
  }
}
