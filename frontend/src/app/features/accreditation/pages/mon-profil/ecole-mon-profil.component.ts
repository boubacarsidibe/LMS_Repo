import { passwordPolicy } from '../../../../core/auth/validators/password-policy';
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Profil de l'école de formation. Même patron que
 * {@code PedagogieMonProfilComponent} et {@code EvaluateurMonProfilComponent} :
 * identité lue depuis {@link Auth.currentUser}, changement de mot de passe par
 * l'endpoint générique `/api/auth/change-password`.
 */
@Component({
  selector: 'app-ecole-mon-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ecole-mon-profil.component.html',
})
export class EcoleMonProfilComponent {
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
    return role === 'ROLE_ECOLE_FORMATION' ? 'École de formation' : role;
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
