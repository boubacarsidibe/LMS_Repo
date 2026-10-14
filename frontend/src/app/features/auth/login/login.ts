import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { EMPTY } from 'rxjs';
import { finalize, tap, catchError } from 'rxjs/operators';
import { Auth } from '../../../core/auth/services/authService';
import { AuthResponse } from '../../../core/auth/models/auth-response';

/**
 * Pages qu'un visiteur peut demander avant de se connecter (`/auth/login?retour=/chemin`), avec la raison
 * affichée. Liste fermée, jamais une adresse libre : `retour` vient de l'URL, donc de n'importe qui — une valeur
 * inconnue est ignorée (ni message, ni redirection), et l'utilisateur arrive à l'accueil de son rôle.
 */
const RETOURS_AUTORISES = new Map<string, string>([
  [
    '/ecoles-accreditees',
    'La liste des écoles accréditées est réservée aux comptes connectés. Connectez-vous pour la consulter.',
  ],
]);

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

  /** Pourquoi on passe par la connexion quand ce n'est pas une panne (voir `RETOURS_AUTORISES`). */
  infoMessage: string = '';

  /** Page à rouvrir une fois connecté ; sans elle, l'accueil du rôle. */
  private retour: string | null = null;

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

    if (this.route.snapshot.queryParamMap.has('passwordChanged')) {
      this.errorMessage = 'Votre mot de passe a été modifié. Connectez-vous avec votre nouveau mot de passe.';
    }

    // Venu d'une page réservée aux comptes (bouton de l'accueil) : lui dire pourquoi, puis l'y ramener.
    const retour = this.route.snapshot.queryParamMap.get('retour') ?? '';
    const raison = RETOURS_AUTORISES.get(retour);
    if (raison) {
      this.retour = retour;
      this.infoMessage = raison;
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
          } else if (err.status === 429) {
            this.errorMessage = 'Trop de tentatives de connexion. Réessayez dans 15 minutes.';
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
          if (this.retour) {
            this.router.navigateByUrl(this.retour);
          } else {
            this.authService.redirectByRole(this.router);
          }
        },
        error: () => {
          this.errorMessage = 'Une erreur inattendue est survenue.';
          this.loginLoading = false;
          this.cdr.markForCheck();
        }
      });

  }

}
