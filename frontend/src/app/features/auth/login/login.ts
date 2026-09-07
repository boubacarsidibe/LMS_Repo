import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { EMPTY } from 'rxjs';
import { finalize, tap, catchError } from 'rxjs/operators';
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

  resendLoading = false;

  resendCountdown = 0;

  constructor(
    private formBuilder: FormBuilder,
    private authService: Auth,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {

    // Redirigé ici par l'intercepteur quand le jeton a expiré en cours de session.
    if (this.route.snapshot.queryParamMap.has('expired')) {
      this.errorMessage = 'Votre session a expiré. Merci de vous reconnecter.';
    }

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
        tap((response: AuthResponse) => {
          this.authService.setSession(response);
        }),
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
            this.authService.redirectByRole(this.router);
        },
        error: () => {
          this.errorMessage = 'Une erreur inattendue est survenue.';
          this.loginLoading = false;
          this.cdr.markForCheck();
        }
      });

  }

  resendOtp(): void {
    if (this.resendLoading || this.resendCountdown > 0) return;

    this.resendLoading = true;
    this.errorMessage = '';

    this.authService.getOtp(this.email).subscribe({
      next: () => {
        this.resendLoading = false;
        this.resendCountdown = 60;
        const interval = setInterval(() => {
          this.resendCountdown--;
          this.cdr.markForCheck();
          if (this.resendCountdown <= 0) {
            clearInterval(interval);
          }
        }, 1000);
      },
      error: (err) => {
        this.resendLoading = false;
        if (err.status === 0) {
          this.errorMessage = 'Impossible de contacter le serveur.';
        } else {
          this.errorMessage = 'Échec de l\'envoi du code. Veuillez réessayer.';
        }
        this.cdr.markForCheck();
      },
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
          this.authService.redirectByRole(this.router);
        },
        error: () => {
          this.errorMessage = 'Une erreur inattendue est survenue lors de la vérification.';
          this.otpLoading = false;
          this.cdr.markForCheck();
        }
      });

  }
}