import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  RecrutementService,
  SessionCandidatureResponse,
  TestAcademiqueRequest,
  TestAcademiqueResponse,
} from '../../../../../core/recrutement/services/recrutement.service';

/** Seuil d'admissibilité par défaut si la session du candidat n'est pas identifiable. */
const SEUIL_ADMISSIBILITE_DEFAUT = 10;

/**
 * Page dédiée de création/édition d'un test académique — remplace le tiroir latéral.
 * Le TDR exige que lieu, date, heure et session soient précisés clairement à la
 * convocation : la session n'était visible nulle part dans l'ancien tiroir (elle
 * s'y déduisait silencieusement du candidat choisi), et le lieu n'existait pas.
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
  sessions: SessionCandidatureResponse[] = [];

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
      candidatId: ['', Validators.required],
      nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      noteObtenue: [null],
      dateEpreuve: ['', Validators.required],
      lieuOuLien: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => (this.candidatures = []),
    });
    this.recrutementService.getAllSessions().subscribe({
      next: (data) => (this.sessions = data || []),
      error: () => (this.sessions = []),
    });

    // Abonnement au paramMap : Angular réutilise l'instance du composant en
    // naviguant entre deux tests sur la même route, sans rappeler ngOnInit.
    this.route.paramMap.subscribe((params) => {
      const param = params.get('id');
      this.formError = '';
      if (!param) {
        this.testId = null;
        this.test = null;
        this.testForm.reset({
          candidatId: '', nomEpreuve: '', coefficient: 1, noteObtenue: null, dateEpreuve: '', lieuOuLien: '',
        });
        this.loading = false;
        return;
      }
      this.testId = Number(param);
      this.loading = true;
      this.recrutementService.getTest(this.testId).subscribe({
        next: (test) => {
          this.test = test;
          this.testForm.setValue({
            candidatId: test.candidatId,
            nomEpreuve: test.nomEpreuve,
            coefficient: test.coefficient,
            noteObtenue: test.noteObtenue ?? null,
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
    });
  }

  /** Candidats admis au Test Académique (ADMIS1), éligibles pour une planification d'épreuve. */
  get candidatsEligibles(): CandidatureResponse[] {
    return this.candidatures.filter((c) => c.statut === 'ADMIS1' || c.candidatId === this.test?.candidatId);
  }

  /** Session déduite du candidat choisi — affichée pour que l'admin la voie clairement avant d'enregistrer. */
  get sessionAffichee(): string | null {
    if (this.test && this.test.candidatId === Number(this.testForm.value.candidatId)) {
      return this.test.sessionCandidatureLibelle;
    }
    const candidature = this.candidatures.find((c) => c.candidatId === Number(this.testForm.value.candidatId));
    return candidature?.sessionCandidatureLibelle ?? null;
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

    const obs = this.testId
      ? this.recrutementService.modifierTest(this.testId, request)
      : this.recrutementService.creerTest(request);

    obs.subscribe({
      next: () => {
        // Note en dessous du seuil de réussite de la session : proposer le rejet
        // immédiat du dossier avec notification au candidat (TDR : "envoyer un mail
        // automatique aux candidats non retenus à l'entretien").
        if (request.noteObtenue !== null && request.noteObtenue < this.seuilPour(request.candidatId)) {
          this.proposerRejetSousSeuil(request.candidatId, request.noteObtenue);
          return;
        }
        this.retourListe();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err.error?.message || "Échec de l'enregistrement du test.";
      },
    });
  }

  annuler(): void {
    this.retourListe();
  }

  /** Seuil de réussite (note/20) de la session à laquelle appartient le candidat. */
  private seuilPour(candidatId: number): number {
    const candidature = this.candidatures.find((c) => c.candidatId === candidatId);
    const session = candidature ? this.sessions.find((s) => s.id === candidature.sessionCandidatureId) : undefined;
    return session?.seuilReussiteTest ?? SEUIL_ADMISSIBILITE_DEFAUT;
  }

  private proposerRejetSousSeuil(candidatId: number, note: number): void {
    const candidature = this.candidatures.find((c) => c.candidatId === candidatId);
    if (!candidature || candidature.statut === 'REJETE') {
      this.retourListe();
      return;
    }
    const seuil = this.seuilPour(candidatId);
    const confirme = confirm(
      `${candidature.candidatPrenom} ${candidature.candidatNom} obtient ${note}/20, en dessous du seuil de réussite (${seuil}/20).\n\n` +
      `Rejeter automatiquement son dossier et l'en informer par email ?`
    );
    if (!confirme) {
      this.retourListe();
      return;
    }
    this.recrutementService.rejeterDossier(candidature.id, `Note insuffisante au test académique (${note}/20, seuil requis : ${seuil}/20).`)
      .subscribe({
        next: () => this.retourListe(),
        error: () => {
          alert('Échec du rejet automatique du dossier.');
          this.retourListe();
        },
      });
  }

  private retourListe(): void {
    this.router.navigate(['/responsable-admission/tests']);
  }
}
