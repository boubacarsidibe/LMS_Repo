import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { CandidatureResponse, PieceJustificativeResponse, RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { ColonneExport, ExportColumnsModalComponent } from '../../../../shared/components/export-columns-modal/export-columns-modal.component';

type StatutFiltre = 'TOUS' | 'SOUMIS' | 'EN_COURS_DE_PRESELECTION' | 'ADMIS1' | 'ADMIS2' | 'ADMIS3' | 'REJETE' | 'BROUILLON';

@Component({
  selector: 'app-responsable-admission-candidatures',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ExportColumnsModalComponent],
  templateUrl: './responsable-admission-candidatures.component.html',
})
export class ResponsableAdmissionCandidaturesComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  loading = false;
  errorMessage = '';

  recherche = '';
  filtreStatut: StatutFiltre = 'TOUS';

  candidatureSelectionnee: CandidatureResponse | null = null;
  actionEnCours = false;
  exporting = false;
  exportModalOpen = false;

  readonly colonnesExport: ColonneExport[] = [
    { key: 'numeroDossier', label: 'N° Dossier' },
    { key: 'nom', label: 'Nom' },
    { key: 'prenom', label: 'Prénom' },
    { key: 'email', label: 'Email' },
    { key: 'session', label: 'Session' },
    { key: 'statut', label: 'Statut' },
    { key: 'dateSoumission', label: 'Date de soumission' },
    { key: 'motifRejet', label: 'Motif de rejet' },
    { key: 'genre', label: 'Genre' },
    { key: 'niveauEtude', label: "Niveau d'études" },
    { key: 'profession', label: 'Profession' },
    { key: 'anneesExperience', label: "Années d'expérience" },
  ];
  readonly colonnesExportParDefaut = ['numeroDossier', 'nom', 'prenom', 'email', 'session', 'statut', 'dateSoumission', 'motifRejet'];

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.chargerCandidatures();
  }

  private chargerCandidatures(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => {
        this.candidatures = data || [];
        this.loading = false;
      },
      error: () => {
        this.candidatures = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger les candidatures.';
      },
    });
  }

  get candidaturesFiltrees(): CandidatureResponse[] {
    const q = this.recherche.trim().toLowerCase();
    return this.candidatures.filter((c) => {
      if (this.filtreStatut !== 'TOUS' && c.statut !== this.filtreStatut) return false;
      if (!q) return true;
      return (
        (c.candidatNom || '').toLowerCase().includes(q) ||
        (c.candidatPrenom || '').toLowerCase().includes(q) ||
        (c.candidatEmail || '').toLowerCase().includes(q) ||
        (c.numeroDossier || '').toLowerCase().includes(q)
      );
    });
  }

  compter(statut: StatutFiltre): number {
    if (statut === 'TOUS') return this.candidatures.length;
    return this.candidatures.filter((c) => c.statut === statut).length;
  }

  libelleStatut(statut: string): string {
    switch (statut) {
      case 'BROUILLON': return 'Brouillon';
      case 'SOUMIS': return 'Soumis';
      case 'EN_COURS_DE_PRESELECTION': return 'En présélection';
      case 'ADMIS1': return 'Admis au test';
      case 'ADMIS2': return 'Admis à l\'entretien';
      case 'ADMIS3': return 'Admis (final)';
      case 'REJETE': return 'Rejeté';
      default: return statut;
    }
  }

  classeStatut(statut: string): string {
    switch (statut) {
      case 'REJETE': return 'bg-error-container/20 text-error';
      case 'ADMIS3': return 'bg-green-100 text-green-800';
      case 'ADMIS1':
      case 'ADMIS2': return 'bg-primary-fixed text-on-primary-fixed-variant';
      case 'SOUMIS':
      case 'EN_COURS_DE_PRESELECTION': return 'bg-secondary-container/30 text-on-secondary-container';
      default: return 'bg-surface-container text-on-surface-variant';
    }
  }

  /** Libellé + capacité d'action selon l'étape actuelle du workflow (cf. CandidatureService#estTransitionValide côté backend). */
  actionSuivante(c: CandidatureResponse): { libelle: string; action: () => void } | null {
    switch (c.statut) {
      case 'SOUMIS':
        return { libelle: 'Valider présélection', action: () => this.validerPreselection(c) };
      case 'EN_COURS_DE_PRESELECTION':
        return { libelle: 'Admettre au test', action: () => this.admettreTest(c) };
      case 'ADMIS1':
        return { libelle: 'Admettre à l\'entretien', action: () => this.admettreEntretien(c) };
      case 'ADMIS2':
        return { libelle: 'Admettre (final)', action: () => this.admettreFinal(c) };
      default:
        return null;
    }
  }

  peutRejeter(c: CandidatureResponse): boolean {
    return c.statut !== 'ADMIS3' && c.statut !== 'REJETE' && c.statut !== 'BROUILLON';
  }

  voirDetail(c: CandidatureResponse): void {
    this.candidatureSelectionnee = c;
  }

  fermerDetail(): void {
    this.candidatureSelectionnee = null;
  }

  private executer(obs: Observable<CandidatureResponse>): void {
    this.actionEnCours = true;
    obs.subscribe({
      next: () => {
        this.actionEnCours = false;
        this.candidatureSelectionnee = null;
        this.chargerCandidatures();
      },
      error: (err) => {
        this.actionEnCours = false;
        alert(err.error?.message || "Échec de l'action sur la candidature.");
      },
    });
  }

  validerPreselection(c: CandidatureResponse): void {
    this.executer(this.recrutementService.validerPreselection(c.id));
  }

  admettreTest(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreTestAcademique(c.id));
  }

  admettreEntretien(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreEntretien(c.id));
  }

  admettreFinal(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreFinal(c.id));
  }

  rejeter(c: CandidatureResponse): void {
    const motif = window.prompt(`Motif de rejet pour le dossier ${c.numeroDossier} :`);
    if (motif === null) return; // annulé
    if (!motif.trim()) {
      alert('Un motif de rejet est requis.');
      return;
    }
    this.actionEnCours = true;
    this.recrutementService.rejeterDossier(c.id, motif.trim()).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.candidatureSelectionnee = null;
        this.chargerCandidatures();
      },
      error: (err) => {
        this.actionEnCours = false;
        alert(err.error?.message || 'Échec du rejet de la candidature.');
      },
    });
  }

  telechargerPiece(piece: PieceJustificativeResponse): void {
    if (piece.fichierStockeId == null) return;
    this.recrutementService.telechargerPiece(piece.id).subscribe({
      next: (blob) => downloadBlob(blob, piece.fichierNomOriginal || `document_${piece.id}`),
      error: () => alert('Échec du téléchargement du document.'),
    });
  }

  exporter(colonnes: string[]): void {
    this.exporting = true;
    const statut = this.filtreStatut === 'TOUS' ? undefined : this.filtreStatut;
    this.recrutementService.exporterCandidatures(statut, colonnes).subscribe({
      next: (blob) => {
        downloadBlob(blob, `candidatures_recrutement_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        alert("Échec de l'export Excel.");
      },
    });
  }
}
