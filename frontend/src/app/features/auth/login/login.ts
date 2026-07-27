import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, throwError } from 'rxjs';
import { finalize, switchMap, tap, catchError } from 'rxjs/operators';



import { Auth } from '../../../core/auth/services/authService';
import { AuthResponse } from '../../../core/auth/models/auth-response';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Login {

  form: FormGroup;

  errorMessage: string = '';

  email = '';

  token: string | null = null;

  showOtpField = false;

  loginLoading = false;

  otpLoading = false;


  // Déclaration de la variable activeTab utilisée dans le HTML
  activeTab: 'login' | 'register' = 'login';

  // Déclaration de la fonction switchTab appelée au clic
  switchTab(tab: 'login' | 'register'): void {
    this.activeTab = tab;
  }






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

    this.errorMessage = '';

    this.loginLoading = true;

    this.email = this.form.value.email;

    const password = this.form.value.password;

    this.loginLoading = true;
    this.errorMessage = '';

    this.authService.login(this.email, password)
      .pipe(

        catchError((err) => {
          if (err.status === 401) {
            this.errorMessage = 'Adresse e-mail ou mot de passe incorrect.';
            console.warn('Erreur de connexion :', err.error?.message || err.message);
          } else {
            this.errorMessage = 'Une erreur est survenue. Veuillez réessayer plus tard.';
          }
          this.cdr.markForCheck();
          // On retourne EMPTY pour stopper le flux proprement
          return EMPTY;
        }),

        // Enchaîne sur la récupération de l'OTP
        switchMap(() => this.authService.getOtp(this.email)),
        // catchError attrape les erreurs de login ET de getOtp

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
          } else {
            this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
          }
          this.cdr.markForCheck();
          return throwError(() => err);
        }),

        finalize(() => {
          this.otpLoading = false;
          this.cdr.markForCheck();
        })

      )
      .subscribe({
        next: (isVerified: boolean) => {
          
          if (!isVerified) {
            this.errorMessage =
              'Le code OTP est invalide ou a expiré.';

            return;
          }
          this.router.navigate(['/test1']);
          this.authService.setToken(this.token);
        },

        error: () => {
          this.errorMessage =
            'Une erreur est survenue lors de la vérification du code.';
        }

      });

  }

}