import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  EntretienResponse,
  RecrutementService,
  TestAcademiqueResponse,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';

interface LigneDeliberation {
  candidature: CandidatureResponse;
  noteTest: number | null;
  noteEntretien: number | null;
  moyenne: number | null;
}

@Component({
  selector: 'app-responsable-admission-deliberation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission-deliberation.component.html',
})
export class ResponsableAdmissionDeliberationComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  entretiens: EntretienResponse[] = [];
  tests: TestAcademiqueResponse[] = [];
  loading = false;
  errorMessage = '';
  private chargementsEnCours = 0;

  recherche = '';
  filtre: 'TOUS' | 'EN_ATTENTE' | 'ADMIS' | 'REJETES' = 'EN_ATTENTE';
  actionEnCours = false;
  exporting = false;

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.chargementsEnCours = 3;

    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => {
        this.candidatures = [];
        this.errorMessage = 'Impossible de charger les candidatures.';
      },
      complete: () => this.finDeChargement(),
    });
    this.recrutementService.getAllEntretiens().subscribe({
      next: (data) => (this.entretiens = data || []),
      error: () => (this.entretiens = []),
      complete: () => this.finDeChargement(),
    });
    this.recrutementService.getAllTests().subscribe({
      next: (data) => (this.tests = data || []),
      error: () => (this.tests = []),
      complete: () => this.finDeChargement(),
    });
  }

  private finDeChargement(): void {
    this.chargementsEnCours--;
    if (this.chargementsEnCours <= 0) this.loading = false;
  }

  /** Moyenne pondérée par coefficient des épreuves du candidat, ou null s'il n'a passé aucun test. */
  private noteTestDe(candidatId: number): number | null {
    const testsDuCandidat = this.tests.filter((t) => t.candidatId === candidatId);
    if (testsDuCandidat.length === 0) return null;
    const sommeCoeffs = testsDuCandidat.reduce((s, t) => s + (t.coefficient || 1), 0);
    const sommePonderee = testsDuCandidat.reduce((s, t) => s + t.noteObtenue * (t.coefficient || 1), 0);
    return sommeCoeffs > 0 ? Math.round((sommePonderee / sommeCoeffs) * 100) / 100 : null;
  }

  /** Note du dernier entretien effectué par le candidat (s'il en a plusieurs). */
  private noteEntretienDe(candidatId: number): number | null {
    const entretiensDuCandidat = this.entretiens
      .filter((e) => e.candidatId === candidatId && e.noteEntretien !== null)
      .sort((a, b) => b.dateHeure.localeCompare(a.dateHeure));
    return entretiensDuCandidat.length > 0 ? entretiensDuCandidat[0].noteEntretien : null;
  }

  get lignes(): LigneDeliberation[] {
    // Seules les candidatures ayant atteint l'étape entretien (ADMIS2), ou déjà décidées
    // à cette étape (ADMIS3 / REJETE), ont leur place en délibération.
    const eligibles = this.candidatures.filter((c) => c.statut === 'ADMIS2' || c.statut === 'ADMIS3' || c.statut === 'REJETE');

    return eligibles
      .map((c) => {
        const noteTest = this.noteTestDe(c.candidatId);
        const noteEntretien = this.noteEntretienDe(c.candidatId);
        const notes = [noteTest, noteEntretien].filter((n): n is number => n !== null);
        const moyenne = notes.length > 0 ? Math.round((notes.reduce((s, n) => s + n, 0) / notes.length) * 100) / 100 : null;
        return { candidature: c, noteTest, noteEntretien, moyenne };
      })
      .sort((a, b) => (b.moyenne ?? -1) - (a.moyenne ?? -1));
  }

  get lignesFiltrees(): LigneDeliberation[] {
    const q = this.recherche.trim().toLowerCase();
    return this.lignes.filter((l) => {
      if (q) {
        const texte = `${l.candidature.candidatPrenom} ${l.candidature.candidatNom} ${l.candidature.numeroDossier}`.toLowerCase();
        if (!texte.includes(q)) return false;
      }
      switch (this.filtre) {
        case 'EN_ATTENTE': return l.candidature.statut === 'ADMIS2';
        case 'ADMIS': return l.candidature.statut === 'ADMIS3';
        case 'REJETES': return l.candidature.statut === 'REJETE';
        default: return true;
      }
    });
  }

  get nbEnAttente(): number {
    return this.lignes.filter((l) => l.candidature.statut === 'ADMIS2').length;
  }

  get nbAdmis(): number {
    return this.lignes.filter((l) => l.candidature.statut === 'ADMIS3').length;
  }

  get nbRejetes(): number {
    return this.lignes.filter((l) => l.candidature.statut === 'REJETE').length;
  }

  admettreFinal(l: LigneDeliberation): void {
    this.actionEnCours = true;
    this.recrutementService.admettreFinal(l.candidature.id).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.charger();
      },
      error: (err) => {
        this.actionEnCours = false;
        alert(err.error?.message || "Échec de l'admission finale.");
      },
    });
  }

  rejeter(l: LigneDeliberation): void {
    const motif = window.prompt(`Motif de rejet pour ${l.candidature.numeroDossier} :`);
    if (motif === null) return;
    if (!motif.trim()) {
      alert('Un motif de rejet est requis.');
      return;
    }
    this.actionEnCours = true;
    this.recrutementService.rejeterDossier(l.candidature.id, motif.trim()).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.charger();
      },
      error: (err) => {
        this.actionEnCours = false;
        alert(err.error?.message || 'Échec du rejet.');
      },
    });
  }

  exporter(): void {
    this.exporting = true;
    this.recrutementService.exporterCandidatures().subscribe({
      next: (blob) => {
        downloadBlob(blob, `deliberation_candidatures_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        alert("Échec de l'export Excel.");
      },
    });
  }
}
