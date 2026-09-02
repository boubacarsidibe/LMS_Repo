import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  RecrutementService,
  SessionCandidatureResponse,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
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
  filtreStatut: 'TOUTES' | 'OUVERTES' | 'FERMEES' | 'ACTIVES' = 'TOUTES';

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

  constructor(private recrutementService: RecrutementService, private router: Router) {}

  ngOnInit(): void {
    this.chargerSessions();
  }

  private chargerSessions(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getAllSessions().subscribe({
      next: (data) => {
        this.sessions = data || [];
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
    const filtrees = this.sessions.filter((s) => {
      if (q && !s.libelle.toLowerCase().includes(q)) return false;
      switch (this.filtreStatut) {
        case 'OUVERTES':
          return s.estOuverte;
        case 'FERMEES':
          return !s.estOuverte;
        case 'ACTIVES':
          return s.estActive;
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

  nouvelleSession(): void {
    this.router.navigate(['/responsable-admission/sessions/nouvelle']);
  }

  voirSession(session: SessionCandidatureResponse): void {
    this.router.navigate(['/responsable-admission/sessions', session.id]);
  }

  supprimer(session: SessionCandidatureResponse): void {
    if (!confirm(`Supprimer définitivement la session "${session.libelle}" ?`)) return;
    this.recrutementService.supprimerSession(session.id).subscribe({
      next: () => this.chargerSessions(),
      error: () => alert('Échec de la suppression de la session.'),
    });
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
        alert("Échec de l'export Excel.");
      },
    });
  }
}
