import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  RecrutementService,
  TestAcademiqueRequest,
  TestAcademiqueResponse,
} from '../../../../../core/recrutement/services/recrutement.service';

/**
 * Page dédiée à la modification d'une épreuve déjà planifiée (date, lieu,
 * coefficient) — remplace le tiroir latéral.
 *
 * <p>Ne gère plus la création ni la note : un test est toujours planifié pour un
 * ou plusieurs candidats à la fois via « Convocation groupée » (liste des tests),
 * jamais un par un depuis un formulaire isolé — cette page ne fait plus que
 * corriger les modalités d'une épreuve déjà créée, pour le candidat auquel elle
 * est déjà rattachée. La note se saisit directement dans la liste (grille de
 * notation), au même endroit pour tous les candidats d'une même épreuve.
 */
@Component({
  selector: 'app-test-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './test-form.component.html',
})
export class TestFormComponent implements OnInit {
  testId: number | null = null;
  test: TestAcademiqueResponse | null = null;
  candidatures: CandidatureResponse[] = [];

  loading = false;
  isSubmitting = false;
  formError = '';

  testForm: FormGroup;

  constructor(
    private recrutementService: RecrutementService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.testForm = this.fb.group({
      nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      dateEpreuve: ['', Validators.required],
      lieuOuLien: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('id');
    if (!param) {
      // Plus de mode création isolée : sans id, rien à modifier.
      this.retourListe();
      return;
    }
    this.testId = Number(param);
    this.loading = true;

    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => (this.candidatures = []),
    });

    this.recrutementService.getTest(this.testId).subscribe({
      next: (test) => {
        this.test = test;
        this.testForm.setValue({
          nomEpreuve: test.nomEpreuve,
          coefficient: test.coefficient,
          dateEpreuve: test.dateEpreuve ? test.dateEpreuve.slice(0, 16) : '',
          lieuOuLien: test.lieuOuLien ?? '',
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.formError = 'Impossible de charger ce test.';
      },
    });
  }

  /** Candidat auquel cette épreuve est rattachée — fixe, non modifiable ici. */
  get candidatAffiche(): string {
    const c = this.candidatures.find((cand) => cand.candidatId === this.test?.candidatId);
    return c ? `${c.candidatPrenom} ${c.candidatNom} — ${c.numeroDossier}` : '—';
  }

  get sessionAffichee(): string | null {
    return this.test?.sessionCandidatureLibelle ?? null;
  }

  enregistrer(): void {
    if (this.testForm.invalid || !this.testId || !this.test) {
      this.testForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.formError = '';
    const brut = this.testForm.getRawValue();
    const request: TestAcademiqueRequest = {
      ...brut,
      candidatId: this.test.candidatId,
      coefficient: Number(brut.coefficient),
      noteObtenue: this.test.noteObtenue ?? null,
    };

    this.recrutementService.modifierTest(this.testId, request).subscribe({
      next: () => this.retourListe(),
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err.error?.message || "Échec de l'enregistrement du test.";
      },
    });
  }

  annuler(): void {
    this.retourListe();
  }

  private retourListe(): void {
    this.router.navigate(['/responsable-admission/tests']);
  }
}
