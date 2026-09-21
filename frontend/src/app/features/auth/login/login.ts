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

  loginLoading = false;

  /** Les erreurs de champ ne s'affichent qu'après une tentative de connexion — pas au survol/blur. */
  tentativeEnvoi = false;

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
      ]

    });

  }

  /** Un champ n'affiche son erreur qu'après une tentative de connexion, jamais avant. */
  champEnErreur(nom: string): boolean {
    const controle = this.form.get(nom);
    return !!controle && controle.invalid && this.tentativeEnvoi;
  }

  onSubmit(): void {
    this.tentativeEnvoi = true;
    this.cdr.markForCheck();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loginLoading = true;
    this.errorMessage = '';
    const email = this.form.value.email;
    const password = this.form.value.password;

    this.authService.login(email, password)
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

}
