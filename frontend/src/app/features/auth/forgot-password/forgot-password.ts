import { passwordPolicy } from '../../../core/auth/validators/password-policy';
import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(Auth);
  private router = inject(Router);

  showResetPassword = false;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  // Étape 1 : demande de code par email
  emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  // Étape 2 : code reçu + nouveau mot de passe
  resetForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    password: ['', [Validators.required, passwordPolicy]],
    confirmPassword: ['', [Validators.required]],
  });

  get email() {
    return this.emailForm.controls.email;
  }

  get isEmailInvalid(): boolean {
    return this.email.invalid && (this.email.dirty || this.email.touched);
  }

  get passwordsMismatch(): boolean {
    const { password, confirmPassword } = this.resetForm.getRawValue();
    return !!confirmPassword && password !== confirmPassword;
  }

  /** Étape 1 : envoie le code OTP à l'email saisi */
  onSubmitEmail(): void {
    this.errorMessage = '';
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }

    const { email } = this.emailForm.getRawValue();
    this.isLoading = true;

    this.authService.getOtp(email).subscribe({
      next: () => {
        this.isLoading = false;
        this.showResetPassword = true;
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage =
          error.error?.message || "Impossible d'envoyer le code. Vérifiez l'adresse email saisie.";
      },
    });
  }

  /** Étape 2 : vérifie le code et applique le nouveau mot de passe */
  onSubmitReset(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.resetForm.invalid || this.passwordsMismatch) {
      this.resetForm.markAllAsTouched();
      if (this.resetForm.controls.password.hasError('passwordPolicy')) {
        this.errorMessage = 'Le mot de passe doit contenir au moins 12 caractères et ne pas dépasser 72 octets UTF-8.';
      }
      if (this.passwordsMismatch) {
        this.errorMessage = 'Les mots de passe ne correspondent pas.';
      }
      return;
    }

    const { email } = this.emailForm.getRawValue();
    const { code, password } = this.resetForm.getRawValue();
    this.isLoading = true;

    this.authService.resetPassword(email, code, password).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Mot de passe réinitialisé avec succès. Redirection vers la connexion...';
        setTimeout(() => this.router.navigate(['/auth/login']), 2000);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Code invalide ou expiré. Veuillez réessayer.';
      },
    });
  }
}
