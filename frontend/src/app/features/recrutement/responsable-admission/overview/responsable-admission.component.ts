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
import { NotificationService } from '../../../../shared/components/ui/notification.service';

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
    private prefsService: DashboardPreferencesService,
    private notifications: NotificationService
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

  /** Même définition que Statistiques : total, y compris les brouillons non soumis. */
  get totalCandidats(): number {
    return this.candidatures.length;
  }

  get validationEnCours(): number {
    return this.candidatures.filter((c) => c.statut === 'SOUMIS').length;
  }

  get preselectionnes(): number {
    return this.candidatures.filter((c) =>
      ['EN_COURS_DE_PRESELECTION', 'ADMIS1', 'ADMIS2'].includes(c.statut)
    ).length;
  }

  /** Candidatures ayant reçu une décision finale (admises ou rejetées). */
  get candidaturesDecidees(): number {
    return this.candidatures.filter((c) => c.statut === 'ADMIS3' || c.statut === 'REJETE').length;
  }

  /** Taux d'admission parmi les dossiers déjà tranchés (0 si aucune décision encore prise). */
  get tauxAdmission(): number {
    const decidees = this.candidaturesDecidees;
    if (decidees === 0) return 0;
    const admis = this.candidatures.filter((c) => c.statut === 'ADMIS3').length;
    return Math.round((admis / decidees) * 100);
  }

  /** Les 5 candidatures soumises les plus récentes, pour un coup d'œil sans quitter la vue d'ensemble. */
  get candidaturesRecentes(): CandidatureResponse[] {
    return this.candidatures
      .filter((c) => !!c.dateSoumission)
      .slice()
      .sort((a, b) => new Date(b.dateSoumission).getTime() - new Date(a.dateSoumission).getTime())
      .slice(0, 5);
  }

  /** Nombre de candidatures rattachées à une session donnée (donnée réelle, pas de quota inventé). */
  nbCandidatures(sessionId: number): number {
    return this.candidatures.filter((c) => c.sessionCandidatureId === sessionId).length;
  }

  libelleStatut(statut: string): string {
    switch (statut) {
      case 'BROUILLON': return 'Brouillon';
      case 'SOUMIS': return 'Soumis';
      case 'EN_COURS_DE_PRESELECTION': return 'En présélection';
      case 'ADMIS1': return 'Admis au test';
      case 'ADMIS2': return "Admis à l'entretien";
      case 'ADMIS3': return 'Admis (final)';
      case 'REJETE': return 'Rejeté';
      default: return statut;
    }
  }

  classeStatut(statut: string): string {
    switch (statut) {
      case 'REJETE': return 'bg-error-container/20 text-error';
      case 'ADMIS3': return 'bg-success/15 text-success-strong';
      case 'ADMIS1':
      case 'ADMIS2': return 'bg-primary-fixed text-on-primary-fixed-variant';
      case 'SOUMIS':
      case 'EN_COURS_DE_PRESELECTION': return 'bg-secondary-container/30 text-on-secondary-container';
      default: return 'bg-surface-container text-on-surface-variant';
    }
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
        this.notifications.erreur("Échec de l'export Excel.");
      },
    });
  }
}
