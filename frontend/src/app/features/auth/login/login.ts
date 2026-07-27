import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { switchMap, tap } from 'rxjs/operators';
import { Auth } from '../../../core/auth/services/authService';
import { AuthResponse } from '../../../core/auth/models/auth-response';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  errorMessage: string = '';
  form: FormGroup;
  token: string | null = null;
  showOtpField: boolean = false;
  textButton: string = 'Obtenir le code';
  email: string = '';


  // Déclaration de la variable activeTab utilisée dans le HTML
  activeTab: 'login' | 'register' = 'login';

  // Déclaration de la fonction switchTab appelée au clic
  switchTab(tab: 'login' | 'register'): void {
    this.activeTab = tab;
  }






  constructor(
    private formBuilder: FormBuilder,
    private authService: Auth,
    private router: Router
  ) {
    this.form = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      code: [''] // Champ pour le code OTP
    });
  }

  onSubmit() {
    this.showOtpField = true;
    if (this.form.invalid) {
      this.form.markAllAsTouched(); // Optionnel : affiche les erreurs visuelles si le formulaire est soumis vide
      return;
    }

    const { password } = this.form.value;
    this.email = this.form.value.email; // Stocke l'email pour l'utiliser dans la vérification OTP

    this.authService.login(this.email, password).pipe(
      
      tap(response => {
        
        this.token = response.token;
      }),

      switchMap((response: AuthResponse) => {
        
        return this.authService.getOtp(this.email);
      })
    ).subscribe({
      next: (otpResponse) => {

        // this.router.navigate(['/verify-otp'], { queryParams: { email, token: this.token } });

        this.textButton = 'Connecter';
      },
      error: (err) => {
        if (err.status === 401) {
          this.errorMessage = 'Email ou mot de passe incorrect.';
        } else {
          this.errorMessage = 'Une erreur est survenue. Veuillez réessayer plus tard.';
        }
      }
    });
  }
  verifyOtp() {
    const { code } = this.form.value;
    this.authService.verifyOtp(this.email, code).subscribe({
      next: (isVerified) => {
        if (!isVerified) {
          this.errorMessage = 'Code OTP invalide ou expiré.';
          return;
        }
        this.authService.setToken(this.token);
        this.router.navigate(['/test1']);
      },
      error: (err) => {
        if (err.status === 400 || err.status === 401) {
          this.errorMessage = 'Code OTP invalide ou expiré.';

        } else {
          this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
        }
      }
    });
  }
}
