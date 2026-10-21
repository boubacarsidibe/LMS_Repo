import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  RecrutementService,
  SessionCandidatureResponse,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';
import { ColonneExport, ExportColumnsModalComponent } from '../../../../shared/components/export-columns-modal/export-columns-modal.component';

@Component({
  selector: 'app-responsable-admission-sessions',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ExportColumnsModalComponent],
  templateUrl: './responsable-admission-sessions.component.html',
})
export class ResponsableAdmissionSessionsComponent implements OnInit {
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  errorMessage = '';

  recherche = '';
  filtreStatut: 'TOUTES' | 'OUVERTES' | 'A_VENIR' | 'TERMINEES' = 'TOUTES';

  exporting = false;
  exportModalOpen = false;

  readonly colonnesExport: ColonneExport[] = [
    { key: 'libelle', label: 'Libellé' },
    { key: 'dateOuverture', label: "Date d'ouverture" },
    { key: 'dateFermeture', label: 'Date de fermeture' },
    { key: 'estOuverte', label: 'Ouverte' },
    { key: 'estActive', label: 'Active' },
    { key: 'nbCandidatures', label: 'Nb candidatures' },
  ];
  readonly colonnesExportParDefaut = ['libelle', 'dateOuverture', 'dateFermeture', 'estOuverte', 'estActive', 'nbCandidatures'];

  triColonne: 'libelle' | 'dateOuverture' | 'dateFermeture' | 'nbCandidatures' = 'dateOuverture';
  triDesc = true;

  sessionSelectionnee: SessionCandidatureResponse | null = null;

  constructor(private recrutementService: RecrutementService, private router: Router,
              private notifications: NotificationService, private confirmation: ConfirmationService) {}

  ngOnInit(): void {
    this.chargerSessions();
  }

  private chargerSessions(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getAllSessions().subscribe({
      next: (data) => {
        this.sessions = data || [];
        if (this.sessionSelectionnee && !this.sessions.some((s) => s.id === this.sessionSelectionnee?.id)) {
          this.sessionSelectionnee = null;
        }
        this.loading = false;
      },
      error: () => {
        this.sessions = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger les sessions.';
      },
    });
  }

  get sessionsFiltrees(): SessionCandidatureResponse[] {
    const q = this.recherche.trim().toLowerCase();
    const aujourdHui = new Date().toISOString().slice(0, 10);
    const filtrees = this.sessions.filter((s) => {
      if (q && !s.libelle.toLowerCase().includes(q)) return false;
      switch (this.filtreStatut) {
        case 'OUVERTES':
          return s.estOuverte && s.dateOuverture <= aujourdHui && s.dateFermeture >= aujourdHui;
        case 'A_VENIR':
          return s.dateOuverture > aujourdHui;
        case 'TERMINEES':
          return s.dateFermeture < aujourdHui;
        default:
          return true;
      }
    });
    const sens = this.triDesc ? -1 : 1;
    return [...filtrees].sort((a, b) => {
      const colonne = this.triColonne;
      if (colonne === 'nbCandidatures') return (a.nbCandidatures - b.nbCandidatures) * sens;
      return a[colonne].localeCompare(b[colonne]) * sens;
    });
  }

  trierPar(colonne: 'libelle' | 'dateOuverture' | 'dateFermeture' | 'nbCandidatures'): void {
    if (this.triColonne === colonne) {
      this.triDesc = !this.triDesc;
    } else {
      this.triColonne = colonne;
      this.triDesc = false;
    }
  }

  get totalSessions(): number {
    return this.sessions.length;
  }

  get nbOuvertes(): number {
    return this.sessions.filter((s) => s.estOuverte).length;
  }

  get nbActives(): number {
    return this.sessions.filter((s) => s.estActive).length;
  }

  /** Session ouverte dont la date de fermeture est la plus proche dans le futur. */
  get prochaineFermeture(): SessionCandidatureResponse | null {
    const aujourdhui = new Date().toISOString().slice(0, 10);
    const candidates = this.sessions
      .filter((s) => s.estOuverte && s.dateFermeture >= aujourdhui)
      .sort((a, b) => a.dateFermeture.localeCompare(b.dateFermeture));
    return candidates.length > 0 ? candidates[0] : null;
  }

  statutSession(session: SessionCandidatureResponse): 'OUVERTE' | 'A_VENIR' | 'TERMINEE' {
    const aujourdHui = new Date().toISOString().slice(0, 10);
    if (session.dateOuverture > aujourdHui) return 'A_VENIR';
    if (session.dateFermeture < aujourdHui || !session.estOuverte) return 'TERMINEE';
    return 'OUVERTE';
  }

  libelleStatut(session: SessionCandidatureResponse): string {
    const statut = this.statutSession(session);
    return statut === 'OUVERTE' ? 'Ouverte' : statut === 'A_VENIR' ? 'À venir' : 'Terminée';
  }

  classeStatut(session: SessionCandidatureResponse): string {
    const statut = this.statutSession(session);
    return statut === 'OUVERTE'
      ? 'bg-green-100 text-green-800'
      : statut === 'A_VENIR'
        ? 'bg-primary-container text-on-primary-container'
        : 'bg-surface-container text-on-surface-variant';
  }

  nouvelleSession(): void {
    this.router.navigate(['/responsable-admission/sessions/nouvelle']);
  }

  voirSession(session: SessionCandidatureResponse): void {
    this.modifierSession(session);
  }

  modifierSession(session: SessionCandidatureResponse): void {
    this.router.navigate(['/responsable-admission/sessions', session.id]);
  }

  supprimer(session: SessionCandidatureResponse): void {
    this.confirmation.demander({
      titre: 'Supprimer la session',
      message: `La session « ${session.libelle} » sera définitivement supprimée.`,
      libelleConfirmer: 'Supprimer',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.recrutementService.supprimerSession(session.id).subscribe({
        next: () => this.chargerSessions(),
        error: () => this.notifications.erreur('Échec de la suppression de la session.'),
      });
    });
  }

  estSelectionne(id: number): boolean {
    return this.sessionSelectionnee?.id === id;
  }

  exporter(colonnes: string[]): void {
    this.exporting = true;
    this.recrutementService.exporterSessions(colonnes).subscribe({
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
