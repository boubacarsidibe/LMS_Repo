import { passwordPolicy } from '../../../../core/auth/validators/password-policy';
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Profil du responsable pédagogique.
 *
 * <p>N'existait pas : le cercle d'en-tête n'avait donc aucun endroit où mener, ce qui
 * explique qu'il n'ait jamais été rendu cliquable. Contrairement à l'étudiant et au
 * formateur, le responsable pédagogique n'a pas de fiche dédiée (pas de téléphone à
 * tenir à jour) — l'identité vient donc de {@link Auth.currentUser}, déjà chargée pour
 * l'en-tête, et la seule action utile est le changement de mot de passe, par
 * l'endpoint générique `/api/auth/change-password` qui sert tous les rôles.
 */
@Component({
  selector: 'app-pedagogie-mon-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './pedagogie-mon-profil.component.html',
})
export class PedagogieMonProfilComponent {
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
    return role === 'ROLE_RESPONSABLE_PEDAGOGIQUE' ? 'Responsable pédagogique' : role;
  }

  changerMotDePasse(): void {
    this.messageSucces = '';
    this.messageErreur = '';
    if (this.form.invalid) {
      if (this.form.get('nouveauMotDePasse')?.hasError('passwordPolicy')) {
        this.messageErreur = 'Le mot de passe doit contenir au moins 12 caractères et ne pas dépasser 72 octets UTF-8.';
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
