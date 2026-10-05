import { passwordPolicy } from '../../../../core/auth/validators/password-policy';
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Profil de l'évaluateur accréditation.
 *
 * <p>Même patron que {@code PedagogieMonProfilComponent} : pas de fiche dédiée pour
 * ce rôle, l'identité vient donc de {@link Auth.currentUser}, et la seule action
 * utile est le changement de mot de passe par l'endpoint générique
 * `/api/auth/change-password`, qui sert tous les rôles.
 */
@Component({
  selector: 'app-evaluateur-mon-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './evaluateur-mon-profil.component.html',
})
export class EvaluateurMonProfilComponent {
  private auth = inject(Auth);
  private fb = inject(FormBuilder);

  utilisateur = this.auth.currentUser();

  enregistrement = false;
  messageSucces = '';
  messageErreur = '';

  readonly form: FormGroup = this.fb.group({
    motDePasseActuel: ['', [Validators.required]],
    nouveauMotDePasse: ['', [Validators.required, passwordPolicy]],
    confirmation: ['', [Validators.required]],
  });

  get roleAffiche(): string {
    const role = this.utilisateur?.roles?.[0] || '';
    return role === 'ROLE_EVALUATEUR_ACCREDITATION' ? 'Évaluateur accréditation' : role;
  }

  changerMotDePasse(): void {
    this.messageSucces = '';
    this.messageErreur = '';
    if (this.form.invalid) {
      if (this.form.get('nouveauMotDePasse')?.hasError('passwordPolicy')) {
        this.messageErreur = 'Le mot de passe doit contenir au moins 8 caractères.';
      }
      this.form.markAllAsTouched();
      return;
    }
    const { motDePasseActuel, nouveauMotDePasse, confirmation } = this.form.getRawValue();
    if (nouveauMotDePasse !== confirmation) {
      this.messageErreur = 'La confirmation ne correspond pas au nouveau mot de passe.';
      return;
    }
    this.enregistrement = true;
    this.auth.ChangePassword(motDePasseActuel, nouveauMotDePasse).subscribe({
      next: () => {
        this.enregistrement = false;
        this.messageSucces = 'Mot de passe modifié.';
        this.form.reset();
      },
      error: (err) => {
        this.enregistrement = false;
        this.messageErreur = err?.error?.message || 'Le mot de passe actuel est incorrect.';
      },
    });
  }
}
