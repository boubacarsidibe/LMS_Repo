import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  ConvocationTestGroupeeRequest,
  RecrutementService,
  SessionCandidatureResponse,
  TestAcademiqueResponse,
} from '../../../../core/recrutement/services/recrutement.service';

interface TestAffiche extends TestAcademiqueResponse {
  candidatNom: string;
  candidatPrenom: string;
  numeroDossier: string;
}

/** Seuil d'admissibilité par défaut si la session du candidat n'est pas identifiable. */
const SEUIL_ADMISSIBILITE_DEFAUT = 10;

@Component({
  selector: 'app-responsable-admission-tests',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './responsable-admission-tests.component.html',
})
export class ResponsableAdmissionTestsComponent implements OnInit {
  tests: TestAcademiqueResponse[] = [];
  candidatures: CandidatureResponse[] = [];
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  errorMessage = '';
  private chargementsEnCours = 0;

  recherche = '';

  convocationGroupeeOpen = false;
  selectionCandidatIds = new Set<number>();
  convocationForm: FormGroup;
  envoiGroupeEnCours = false;
  convocationFormError = '';

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder, private router: Router) {
    this.convocationForm = this.fb.group({
      nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      dateEpreuve: ['', Validators.required],
      lieuOuLien: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.chargementsEnCours = 3;

    this.recrutementService.getAllTests().subscribe({
      next: (data) => (this.tests = data || []),
      error: () => {
        this.tests = [];
        this.errorMessage = 'Impossible de charger les tests académiques.';
      },
      complete: () => this.finDeChargement(),
    });

    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => (this.candidatures = []),
      complete: () => this.finDeChargement(),
    });

    this.recrutementService.getAllSessions().subscribe({
      next: (data) => (this.sessions = data || []),
      error: () => (this.sessions = []),
      complete: () => this.finDeChargement(),
    });
  }

  /** Seuil de réussite (note/20) de la session à laquelle appartient le candidat. */
  private seuilPour(candidatId: number): number {
    const candidature = this.candidatureDe(candidatId);
    const session = candidature ? this.sessions.find((s) => s.id === candidature.sessionCandidatureId) : undefined;
    return session?.seuilReussiteTest ?? SEUIL_ADMISSIBILITE_DEFAUT;
  }

  private finDeChargement(): void {
    this.chargementsEnCours--;
    if (this.chargementsEnCours <= 0) {
      this.loading = false;
    }
  }

  private candidatureDe(candidatId: number): CandidatureResponse | undefined {
    return this.candidatures.find((c) => c.candidatId === candidatId);
  }

  get testsAffiches(): TestAffiche[] {
    return this.tests.map((t) => {
      const c = this.candidatureDe(t.candidatId);
      return {
        ...t,
        candidatNom: c?.candidatNom || '—',
        candidatPrenom: c?.candidatPrenom || '',
        numeroDossier: c?.numeroDossier || '',
      };
    });
  }

  get testsFiltres(): TestAffiche[] {
    const q = this.recherche.trim().toLowerCase();
    if (!q) return this.testsAffiches;
    return this.testsAffiches.filter((t) => {
      const texte = `${t.candidatPrenom} ${t.candidatNom} ${t.nomEpreuve} ${t.numeroDossier}`.toLowerCase();
      return texte.includes(q);
    });
  }

  /** Candidats admis au Test Académique (ADMIS1), éligibles pour une planification d'épreuve. */
  get candidatsEligibles(): CandidatureResponse[] {
    return this.candidatures.filter((c) => c.statut === 'ADMIS1');
  }

  get totalTests(): number {
    return this.tests.length;
  }

  get nbNotes(): number {
    return this.tests.filter((t) => t.noteObtenue !== null && t.noteObtenue !== undefined).length;
  }

  get nbAdmissibles(): number {
    return this.tests.filter((t) => t.noteObtenue !== null && t.noteObtenue !== undefined
      && t.noteObtenue >= this.seuilPour(t.candidatId)).length;
  }

  resultat(t: TestAcademiqueResponse): 'ADMISSIBLE' | 'INSUFFISANT' | 'EN_ATTENTE' {
    if (t.noteObtenue === null || t.noteObtenue === undefined) return 'EN_ATTENTE';
    return t.noteObtenue >= this.seuilPour(t.candidatId) ? 'ADMISSIBLE' : 'INSUFFISANT';
  }

  nouveauTest(): void {
    this.router.navigate(['/responsable-admission/tests/nouveau']);
  }

  voirTest(test: TestAcademiqueResponse): void {
    this.router.navigate(['/responsable-admission/tests', test.id]);
  }

  supprimer(test: TestAffiche): void {
    if (!confirm(`Supprimer le test "${test.nomEpreuve}" de ${test.candidatPrenom} ${test.candidatNom} ?`)) return;
    this.recrutementService.supprimerTest(test.id).subscribe({
      next: () => this.charger(),
      error: () => alert('Échec de la suppression du test.'),
    });
  }

  // ===================== Convocation groupée =====================

  ouvrirConvocationGroupee(): void {
    this.convocationFormError = '';
    this.selectionCandidatIds.clear();
    this.convocationForm.reset({ nomEpreuve: '', coefficient: 1, dateEpreuve: '', lieuOuLien: '' });
    this.convocationGroupeeOpen = true;
  }

  fermerConvocationGroupee(): void {
    this.convocationGroupeeOpen = false;
  }

  basculerSelection(candidatId: number): void {
    if (this.selectionCandidatIds.has(candidatId)) {
      this.selectionCandidatIds.delete(candidatId);
    } else {
      this.selectionCandidatIds.add(candidatId);
    }
  }

  toutSelectionner(): void {
    if (this.selectionCandidatIds.size === this.candidatsEligibles.length) {
      this.selectionCandidatIds.clear();
    } else {
      this.candidatsEligibles.forEach((c) => this.selectionCandidatIds.add(c.candidatId));
    }
  }

  envoyerConvocationGroupee(): void {
    if (this.convocationForm.invalid || this.selectionCandidatIds.size === 0) {
      this.convocationForm.markAllAsTouched();
      this.convocationFormError = this.selectionCandidatIds.size === 0 ? 'Sélectionnez au moins un candidat.' : '';
      return;
    }
    this.envoiGroupeEnCours = true;
    this.convocationFormError = '';
    const brut = this.convocationForm.getRawValue();
    const request: ConvocationTestGroupeeRequest = {
      candidatIds: Array.from(this.selectionCandidatIds),
      nomEpreuve: brut.nomEpreuve,
      coefficient: Number(brut.coefficient),
      dateEpreuve: brut.dateEpreuve,
      lieuOuLien: brut.lieuOuLien,
    };
    this.recrutementService.convoquerTestsGroupe(request).subscribe({
      next: () => {
        this.envoiGroupeEnCours = false;
        this.fermerConvocationGroupee();
        this.charger();
      },
      error: (err) => {
        this.envoiGroupeEnCours = false;
        this.convocationFormError = err.error?.message || "Échec de l'envoi des convocations.";
      },
    });
  }
}
