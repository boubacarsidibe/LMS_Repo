import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../core/auth/services/authService';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  // NonNullableFormBuilder évite les valeurs 'null' non désirées à la réinitialisation
  private fb = inject(NonNullableFormBuilder);
  private userService = inject(Auth);
  showResetPassword = false;
  forgotPasswordForm = this.fb.group({
    email: [
      '',
      [
        Validators.required,
        Validators.email
      ]
    ]
  });

  // Getter direct vers le contrôle Email
  get email() {
    return this.forgotPasswordForm.controls.email;
  }

  // Utilisation du getter 'email' déjà défini
  get isEmailInvalid(): boolean {
    return this.email.invalid && (this.email.dirty || this.email.touched);
  }

  onSubmit(): void {
    if (this.forgotPasswordForm.invalid) {
      
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }
    // value.email est maintenant garanti comme étant une string (grâce à NonNullableFormBuilder)
    const { email } = this.forgotPasswordForm.getRawValue();
    const user = this.userService.getUserByEmail(email).subscribe();
    this.showResetPassword = true;
    if (!user) {
      console.error('Utilisateur non trouvé pour l\'email :', email);
      return;
    }

    console.log('Email soumis :', email);
    

    // TODO: Appel API backend
  }
}