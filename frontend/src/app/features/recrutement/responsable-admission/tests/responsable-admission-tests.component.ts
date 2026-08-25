import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  ConvocationTestGroupeeRequest,
  RecrutementService,
  SessionCandidatureResponse,
  TestAcademiqueRequest,
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

  drawerOpen = false;
  testEnEdition: TestAcademiqueResponse | null = null;
  testForm: FormGroup;
  isSubmitting = false;
  formError = '';

  convocationGroupeeOpen = false;
  selectionCandidatIds = new Set<number>();
  convocationForm: FormGroup;
  envoiGroupeEnCours = false;
  convocationFormError = '';

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder) {
    this.testForm = this.fb.group({
      candidatId: ['', Validators.required],
      nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      noteObtenue: [null],
      dateEpreuve: ['', Validators.required],
    });
    this.convocationForm = this.fb.group({
      nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      dateEpreuve: ['', Validators.required],
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
        // Note en dessous du seuil de réussite de la session : proposer le rejet immédiat
        // du dossier avec notification au candidat (TDR : "envoyer un mail automatique
        // aux candidats non retenus à l'entretien").
        if (request.noteObtenue !== null && request.noteObtenue < this.seuilPour(request.candidatId)) {
          this.proposerRejetSousSeuil(request.candidatId, request.noteObtenue);
        }
        this.charger();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err.error?.message || "Échec de l'enregistrement du test.";
      },
    });
  }

  private proposerRejetSousSeuil(candidatId: number, note: number): void {
    const candidature = this.candidatureDe(candidatId);
    if (!candidature || candidature.statut === 'REJETE') return;
    const seuil = this.seuilPour(candidatId);
    const confirme = confirm(
      `${candidature.candidatPrenom} ${candidature.candidatNom} obtient ${note}/20, en dessous du seuil de réussite (${seuil}/20).\n\n` +
      `Rejeter automatiquement son dossier et l'en informer par email ?`
    );
    if (!confirme) return;
    this.recrutementService.rejeterDossier(candidature.id, `Note insuffisante au test académique (${note}/20, seuil requis : ${seuil}/20).`)
      .subscribe({
        next: () => this.charger(),
        error: () => alert('Échec du rejet automatique du dossier.'),
      });
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
    this.convocationForm.reset({ nomEpreuve: '', coefficient: 1, dateEpreuve: '' });
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
