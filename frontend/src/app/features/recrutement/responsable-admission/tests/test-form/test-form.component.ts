import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  CandidatureResponse,
  RecrutementService,
  SessionCandidatureResponse,
  TestAcademiqueRequest,
  TestAcademiqueResponse,
} from '../../../../../core/recrutement/services/recrutement.service';

/** Création groupée ou correction d'une épreuve déjà planifiée. */
@Component({
  selector: 'app-test-form', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './test-form.component.html',
})
export class TestFormComponent implements OnInit {
  testId: number | null = null;
  test: TestAcademiqueResponse | null = null;
  candidatures: CandidatureResponse[] = [];
  sessions: SessionCandidatureResponse[] = [];
  selectionCandidatIds = new Set<number>();
  loading = false;
  isSubmitting = false;
  formError = '';
  creation = false;
  testForm: FormGroup;

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder,
              private route: ActivatedRoute, private router: Router) {
    this.testForm = this.fb.group({
      sessionId: [null, Validators.required], nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      dateEpreuve: ['', Validators.required], lieuOuLien: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('id');
    this.creation = !param;
    this.loading = true;
    if (this.creation) {
      forkJoin({ candidatures: this.recrutementService.getAllCandidatures(), sessions: this.recrutementService.getAllSessions() })
        .subscribe({
          next: ({ candidatures, sessions }) => {
            this.candidatures = candidatures || [];
            this.sessions = sessions || [];
            const session = this.sessions.find((s) => s.estActive) || this.sessions[0];
            if (session) this.testForm.patchValue({ sessionId: session.id });
            this.loading = false;
          },
          error: () => this.echecChargement('Impossible de charger les sessions et les candidats.'),
        });
      return;
    }
    this.testId = Number(param);
    forkJoin({ candidatures: this.recrutementService.getAllCandidatures(), test: this.recrutementService.getTest(this.testId) })
      .subscribe({
        next: ({ candidatures, test }) => {
          this.candidatures = candidatures || [];
          this.test = test;
          this.testForm.patchValue({ nomEpreuve: test.nomEpreuve, coefficient: test.coefficient,
            dateEpreuve: test.dateEpreuve ? test.dateEpreuve.slice(0, 16) : '', lieuOuLien: test.lieuOuLien ?? '' });
          this.testForm.get('sessionId')?.disable();
          this.loading = false;
        },
        error: () => this.echecChargement('Impossible de charger ce test.'),
      });
  }

  get candidatsEligibles(): CandidatureResponse[] {
    const sessionId = Number(this.testForm.getRawValue().sessionId);
    return this.candidatures.filter((c) => c.statut === 'CONVOQUE_TEST' && c.sessionCandidatureId === sessionId);
  }
  get sessionAffichee(): string | null { return this.test?.sessionCandidatureLibelle ?? null; }
  get candidatAffiche(): string {
    const c = this.candidatures.find((cand) => cand.candidatId === this.test?.candidatId);
    return c ? `${c.candidatPrenom} ${c.candidatNom} — ${c.numeroDossier}` : '—';
  }
  changerSession(): void { this.selectionCandidatIds.clear(); }
  basculerSelection(candidatId: number): void {
    this.selectionCandidatIds.has(candidatId) ? this.selectionCandidatIds.delete(candidatId) : this.selectionCandidatIds.add(candidatId);
  }
  toutSelectionner(): void {
    if (this.selectionCandidatIds.size === this.candidatsEligibles.length) this.selectionCandidatIds.clear();
    else this.candidatsEligibles.forEach((c) => this.selectionCandidatIds.add(c.candidatId));
  }

  enregistrer(): void {
    if (this.testForm.invalid || (!this.creation && (!this.testId || !this.test))) { this.testForm.markAllAsTouched(); return; }
    if (this.creation && this.selectionCandidatIds.size === 0) { this.formError = 'Sélectionnez au moins un candidat à convoquer.'; return; }
    this.isSubmitting = true;
    this.formError = '';
    const brut = this.testForm.getRawValue();
    if (this.creation) {
      this.recrutementService.convoquerTestsGroupe({ candidatIds: Array.from(this.selectionCandidatIds),
        nomEpreuve: brut.nomEpreuve, coefficient: Number(brut.coefficient), dateEpreuve: brut.dateEpreuve, lieuOuLien: brut.lieuOuLien })
        .subscribe({ next: () => this.retourListe(), error: (err) => this.echecEnregistrement(err, 'Échec de la création du test.') });
      return;
    }
    const request: TestAcademiqueRequest = { ...brut, candidatId: this.test!.candidatId,
      coefficient: Number(brut.coefficient), noteObtenue: this.test!.noteObtenue ?? null };
    this.recrutementService.modifierTest(this.testId!, request)
      .subscribe({ next: () => this.retourListe(), error: (err) => this.echecEnregistrement(err, "Échec de l'enregistrement du test.") });
  }
  annuler(): void { this.retourListe(); }
  private echecChargement(message: string): void { this.loading = false; this.formError = message; }
  private echecEnregistrement(err: any, fallback: string): void { this.isSubmitting = false; this.formError = err.error?.message || fallback; }
  private retourListe(): void { this.router.navigate(['/responsable-admission/tests']); }
}
