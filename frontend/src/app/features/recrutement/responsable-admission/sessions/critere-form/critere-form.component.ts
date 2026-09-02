import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  CritereAdmissionRequest,
  EnumTypeCritere,
  RecrutementService,
  SessionCandidatureResponse,
} from '../../../../../core/recrutement/services/recrutement.service';

/** Page dédiée de création/édition d'UN critère personnalisé (remplace l'ancien mini-formulaire replié). */
@Component({
  selector: 'app-critere-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './critere-form.component.html',
})
export class CritereFormComponent implements OnInit {
  sessionId!: number;
  session: SessionCandidatureResponse | null = null;
  critereId: number | null = null;

  loading = false;
  isSubmitting = false;
  formError = '';

  critereForm: FormGroup;

  readonly typesCritere: { valeur: EnumTypeCritere; libelle: string }[] = [
    { valeur: 'TEXTE', libelle: 'Texte libre' },
    { valeur: 'NUMERIQUE', libelle: 'Nombre' },
    { valeur: 'BOOLEEN', libelle: 'Oui / Non' },
    { valeur: 'CHOIX', libelle: 'Choix parmi une liste' },
  ];

  constructor(
    private recrutementService: RecrutementService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.critereForm = this.fb.group({
      libelle: ['', Validators.required],
      type: ['TEXTE' as EnumTypeCritere, Validators.required],
      obligatoire: [true],
      options: [null],
      valeurMinimale: [null],
      valeurAttendue: [null],
      ordre: [0],
    });
  }

  ngOnInit(): void {
    this.sessionId = Number(this.route.snapshot.paramMap.get('id'));
    this.recrutementService.getSession(this.sessionId).subscribe({ next: (s) => (this.session = s) });

    const critereParam = this.route.snapshot.paramMap.get('critereId');
    if (!critereParam) return;
    this.critereId = Number(critereParam);
    this.loading = true;
    this.recrutementService.getCritere(this.critereId).subscribe({
      next: (critere) => {
        this.critereForm.setValue({
          libelle: critere.libelle,
          type: critere.type,
          obligatoire: critere.obligatoire,
          options: critere.options,
          valeurMinimale: critere.valeurMinimale,
          valeurAttendue: critere.valeurAttendue,
          ordre: critere.ordre,
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.formError = 'Impossible de charger ce critère.';
      },
    });
  }

  enregistrer(): void {
    if (this.critereForm.invalid) {
      this.critereForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.formError = '';
    const request: CritereAdmissionRequest = this.critereForm.getRawValue();

    const obs = this.critereId
      ? this.recrutementService.modifierCritere(this.critereId, request)
      : this.recrutementService.creerCritere(this.sessionId, request);

    obs.subscribe({
      next: () => this.retourListe(),
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err.error?.message || "Échec de l'enregistrement du critère.";
      },
    });
  }

  annuler(): void {
    this.retourListe();
  }

  private retourListe(): void {
    this.router.navigate(['/responsable-admission/sessions', this.sessionId, 'criteres']);
  }
}
