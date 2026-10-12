import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NIVEAUX_ETUDES } from '../../../niveaux-etudes';
import {
  RecrutementService,
  SessionCandidatureRequest,
  SessionCandidatureResponse,
} from '../../../../../core/recrutement/services/recrutement.service';

/**
 * Page dédiée de création/édition d'une session — reprend le contenu qui vivait
 * dans le tiroir latéral (voir git blame). La gestion des critères personnalisés,
 * elle, vit désormais sur sa propre page (SessionCriteresComponent) : une session
 * doit d'abord exister pour qu'un critère puisse lui être rattaché.
 */
@Component({
  selector: 'app-session-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './session-form.component.html',
})
export class SessionFormComponent implements OnInit {
  sessionId: number | null = null;
  session: SessionCandidatureResponse | null = null;
  loading = false;
  isSubmitting = false;
  formError = '';

  sessionForm: FormGroup;

  readonly niveaux = NIVEAUX_ETUDES;

  constructor(
    private recrutementService: RecrutementService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    this.sessionForm = this.fb.group({
      libelle: ['', Validators.required],
      dateOuverture: ['', Validators.required],
      dateFermeture: ['', Validators.required],
      estOuverte: [true],
      criteresActifs: [false],
      criteresNiveauEtudeMinimum: [null],
      criteresAgeMinimum: [null],
      criteresAgeMaximum: [null],
      criteresGenre: [null],
      criteresExperienceMinimale: [null],
      seuilReussiteTest: [10],
    });
  }

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('id');
    if (!param) return;
    this.sessionId = Number(param);
    this.loading = true;
    this.recrutementService.getSession(this.sessionId).subscribe({
      next: (session) => {
        this.session = session;
        this.sessionForm.setValue({
          libelle: session.libelle,
          dateOuverture: session.dateOuverture,
          dateFermeture: session.dateFermeture,
          estOuverte: session.estOuverte,
          criteresActifs: session.criteresActifs,
          criteresNiveauEtudeMinimum: session.criteresNiveauEtudeMinimum,
          criteresAgeMinimum: session.criteresAgeMinimum,
          criteresAgeMaximum: session.criteresAgeMaximum,
          criteresGenre: session.criteresGenre,
          criteresExperienceMinimale: session.criteresExperienceMinimale,
          seuilReussiteTest: session.seuilReussiteTest ?? 10,
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.formError = 'Impossible de charger cette session.';
      },
    });
  }

  enregistrer(): void {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.formError = '';
    const request: SessionCandidatureRequest = this.sessionForm.getRawValue();

    if (this.sessionId) {
      this.recrutementService.modifierSession(this.sessionId, request).subscribe({
        next: () => this.router.navigate(['/responsable-admission/sessions']),
        error: (err) => {
          this.isSubmitting = false;
          this.formError = err.error?.message || "Échec de l'enregistrement de la session.";
        },
      });
    } else {
      this.recrutementService.creerSession(request).subscribe({
        // On atterrit sur la page d'édition de la session tout juste créée : c'est
        // seulement à partir de là que « Gérer les critères personnalisés » apparaît.
        next: (response) => this.router.navigate(['/responsable-admission/sessions', response.id], { replaceUrl: true }),
        error: (err) => {
          this.isSubmitting = false;
          this.formError = err.error?.message || "Échec de la création de la session.";
        },
      });
    }
  }

  annuler(): void {
    this.router.navigate(['/responsable-admission/sessions']);
  }
}
