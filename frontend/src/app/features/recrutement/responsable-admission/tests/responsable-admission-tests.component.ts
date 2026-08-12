import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  RecrutementService,
  TestAcademiqueRequest,
  TestAcademiqueResponse,
} from '../../../../core/recrutement/services/recrutement.service';

interface TestAffiche extends TestAcademiqueResponse {
  candidatNom: string;
  candidatPrenom: string;
  numeroDossier: string;
}

/** Seuil d'admissibilité utilisé pour l'affichage du résultat (note sur 20). */
const SEUIL_ADMISSIBILITE = 10;

@Component({
  selector: 'app-responsable-admission-tests',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './responsable-admission-tests.component.html',
})
export class ResponsableAdmissionTestsComponent implements OnInit {
  tests: TestAcademiqueResponse[] = [];
  candidatures: CandidatureResponse[] = [];
  loading = false;
  errorMessage = '';
  private chargementsEnCours = 0;

  recherche = '';

  drawerOpen = false;
  testEnEdition: TestAcademiqueResponse | null = null;
  testForm: FormGroup;
  isSubmitting = false;
  formError = '';

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder) {
    this.testForm = this.fb.group({
      candidatId: ['', Validators.required],
      nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      noteObtenue: [null],
      dateEpreuve: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.chargementsEnCours = 2;

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
    return this.tests.filter((t) => (t.noteObtenue ?? -1) >= SEUIL_ADMISSIBILITE).length;
  }

  resultat(t: TestAcademiqueResponse): 'ADMISSIBLE' | 'INSUFFISANT' | 'EN_ATTENTE' {
    if (t.noteObtenue === null || t.noteObtenue === undefined) return 'EN_ATTENTE';
    return t.noteObtenue >= SEUIL_ADMISSIBILITE ? 'ADMISSIBLE' : 'INSUFFISANT';
  }

  openDrawer(test?: TestAcademiqueResponse): void {
    this.formError = '';
    if (test) {
      this.testEnEdition = test;
      this.testForm.setValue({
        candidatId: test.candidatId,
        nomEpreuve: test.nomEpreuve,
        coefficient: test.coefficient,
        noteObtenue: test.noteObtenue ?? null,
        dateEpreuve: this.versInputDatetimeLocal(test.dateEpreuve),
      });
    } else {
      this.testEnEdition = null;
      this.testForm.reset({
        candidatId: '',
        nomEpreuve: '',
        coefficient: 1,
        noteObtenue: null,
        dateEpreuve: '',
      });
    }
    this.drawerOpen = true;
  }

  closeDrawer(): void {
    this.drawerOpen = false;
  }

  private versInputDatetimeLocal(iso: string): string {
    return iso ? iso.slice(0, 16) : '';
  }

  enregistrer(): void {
    if (this.testForm.invalid) {
      this.testForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.formError = '';
    const brut = this.testForm.getRawValue();
    const request: TestAcademiqueRequest = {
      ...brut,
      candidatId: Number(brut.candidatId),
      coefficient: Number(brut.coefficient),
      noteObtenue: brut.noteObtenue === null || brut.noteObtenue === '' ? null : Number(brut.noteObtenue),
    };

    const obs = this.testEnEdition
      ? this.recrutementService.modifierTest(this.testEnEdition.id, request)
      : this.recrutementService.creerTest(request);

    obs.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.closeDrawer();
        this.charger();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err.error?.message || "Échec de l'enregistrement du test.";
      },
    });
  }

  supprimer(test: TestAffiche): void {
    if (!confirm(`Supprimer le test "${test.nomEpreuve}" de ${test.candidatPrenom} ${test.candidatNom} ?`)) return;
    this.recrutementService.supprimerTest(test.id).subscribe({
      next: () => this.charger(),
      error: () => alert('Échec de la suppression du test.'),
    });
  }
}
