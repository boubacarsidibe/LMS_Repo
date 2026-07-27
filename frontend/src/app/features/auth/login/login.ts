import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { EMPTY } from 'rxjs';
import { finalize, switchMap, tap, catchError } from 'rxjs/operators';
import { Auth } from '../../../core/auth/services/authService';
import { AuthResponse } from '../../../core/auth/models/auth-response';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Login {

  form: FormGroup;

  errorMessage: string = '';

  email = '';

  showOtpField = false;

  loginLoading = false;

  otpLoading = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {

    this.form = this.formBuilder.group({

      email: [
        '',
        [
          Validators.required,
          Validators.email
        ]
      ],

      password: [
        '',
        Validators.required
      ],

      otp: ['']

    });

  }

  onSubmit(): void {

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loginLoading = true;
    this.errorMessage = '';
    this.email = this.form.value.email;
    const password = this.form.value.password;

    this.authService.login(this.email, password)
      .pipe(
        // Capture le token et l'utilisateur dès que le login réussit
        tap((response: AuthResponse) => {
          this.authService.setSession(response.user, response.token);
        }),
        // Envoie l'OTP après connexion réussie
        switchMap(() => this.authService.getOtp(this.email)),
        // Gère les erreurs de login() ET de getOtp()
        catchError((err) => {
          if (err.status === 401) {
            this.errorMessage = 'Adresse e-mail ou mot de passe incorrect.';
          } else if (err.status === 0) {
            this.errorMessage = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
          } else {
            this.errorMessage = 'Une erreur est survenue. Veuillez réessayer plus tard.';
          }
          console.warn('Erreur de connexion :', err.status, err.error?.message || err.message);
          this.cdr.markForCheck();
          return EMPTY;
        }),
        finalize(() => {
          this.loginLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.showOtpField = true;
          const otpControl = this.form.get('otp');
          otpControl?.setValidators([
            Validators.required,
            Validators.minLength(6),
            Validators.maxLength(6)
          ]);
          otpControl?.updateValueAndValidity();
          this.cdr.markForCheck();
        },
        error: () => {
          this.errorMessage = 'Une erreur inattendue est survenue.';
          this.loginLoading = false;
          this.cdr.markForCheck();
        }
      });

  }

  verifyOtp(): void {

    if (this.form.get('otp')?.invalid) {
      this.form.get('otp')?.markAsTouched();
      return;
    }

    this.errorMessage = '';

    this.otpLoading = true;

    const otp = this.form.value.otp;

    this.authService
      .verifyOtp(this.email, otp)
      .pipe(
        catchError((err) => {
          if (err.status === 400 || err.status === 401) {
            this.errorMessage = 'Code OTP invalide ou expiré.';
          } else if (err.status === 0) {
            this.errorMessage = 'Impossible de contacter le serveur.';
          } else {
            this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
          }
          console.warn('Erreur OTP :', err.status, err.error?.message || err.message);
          this.cdr.markForCheck();
          return EMPTY;
        }),
        finalize(() => {
          this.otpLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (isVerified: boolean) => {
          if (!isVerified) {
            this.errorMessage = 'Le code OTP est invalide ou a expiré.';
            return;
          }
          // Le token est déjà stocké via setSession() dans onSubmit()
          this.router.navigate(['/test1']);
        },
        error: () => {
          this.errorMessage = 'Une erreur inattendue est survenue lors de la vérification.';
          this.otpLoading = false;
          this.cdr.markForCheck();
        }
      });

  }
}