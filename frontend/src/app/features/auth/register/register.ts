import { passwordPolicy } from '../../../core/auth/validators/password-policy';
import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';

/**
 * confirmPassword n'avait jusqu'ici que Validators.required : un mot de passe
 * de confirmation différent du premier passait la validation sans le
 * moindre avertissement (relevé lors d'un audit externe). Validateur de
 * groupe plutôt que de champ, car il doit lire les deux contrôles à la fois.
 */
function motsDePasseIdentiques(groupe: AbstractControl): ValidationErrors | null {
  const motDePasse = groupe.get('password')?.value;
  const confirmation = groupe.get('confirmPassword')?.value;
  return !confirmation || motDePasse === confirmation ? null : { motsDePasseDifferents: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterLink, 
    RouterLinkActive
  ],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(Auth);
  private router = inject(Router);

  errorMessage: string = '';
  isLoading: boolean = false;

  /**
   * Un compte tout juste créé n'a encore aucun rôle métier (attribué
   * uniquement à la soumission réelle d'une candidature ou d'une demande
   * d'accréditation) : sans ce choix, redirectByRole renvoyait aussi bien un
   * futur candidat qu'un représentant d'école vers l'accueil générique, sans
   * aucune indication sur la suite à donner.
   */
  typeInscription: 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE' = 'CANDIDAT';

  /**
   * École et entreprise s'inscrivent au nom d'une organisation, pas d'une
   * personne : "raisonSociale"/"nomEcole" ne sont recueillis qu'à la
   * soumission réelle de la demande (EntrepriseController, EcoleAccreditationController),
   * jamais à l'inscription. Un seul champ "nom de l'organisation" ici évite
   * de faire deviner un prénom/nom inexistant.
   */
  get estOrganisation(): boolean {
    return this.typeInscription === 'ECOLE' || this.typeInscription === 'ENTREPRISE';
  }

  get libelleNomOrganisation(): string {
    return this.typeInscription === 'ECOLE' ? "Nom de l'école" : "Nom de l'entreprise";
  }

  // Déclaration de tous les contrôles liés au HTML
  registerForm: FormGroup = this.fb.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    nomOrganisation: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, passwordPolicy]],
    confirmPassword: ['', Validators.required]
  }, { validators: motsDePasseIdentiques });

  /** Lu depuis le template (comparaison des mots de passe) : pas de modificateur d'accès. */
  tentativeEnvoi = false;

  /** Un champ n'affiche son erreur qu'après une tentative d'envoi, jamais avant (même en quittant le champ). */
  champEnErreur(nom: string): boolean {
    const controle = this.registerForm.get(nom);
    return !!controle && controle.invalid && this.tentativeEnvoi;
  }

  choisirTypeInscription(type: 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE'): void {
    this.typeInscription = type;
    const prenom = this.registerForm.get('prenom')!;
    const nom = this.registerForm.get('nom')!;
    const nomOrganisation = this.registerForm.get('nomOrganisation')!;

    if (this.estOrganisation) {
      prenom.clearValidators();
      nom.clearValidators();
      nomOrganisation.setValidators(Validators.required);
    } else {
      prenom.setValidators(Validators.required);
      nom.setValidators(Validators.required);
      nomOrganisation.clearValidators();
    }
    prenom.updateValueAndValidity();
    nom.updateValueAndValidity();
    nomOrganisation.updateValueAndValidity();
  }

  onSubmit(): void {
    this.tentativeEnvoi = true;
    if (this.registerForm.invalid) {
      if (this.registerForm.controls['password'].hasError('passwordPolicy')) {
        this.errorMessage = 'Le mot de passe doit contenir au moins 8 caractères.';
      } else if (this.registerForm.hasError('motsDePasseDifferents')) {
        this.errorMessage = 'Les mots de passe ne correspondent pas.';
      }
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const { prenom, nom, nomOrganisation, email, password } = this.registerForm.value;

    // Génération automatique d'un username : à partir du prénom/nom pour un
    // candidat, du nom de l'organisation pour une école ou une entreprise.
    const username = this.estOrganisation
      ? nomOrganisation.trim().toLowerCase().replace(/\s+/g, '_')
      : `${prenom.trim()}_${nom.trim()}`.toLowerCase();

    this.authService.register(username, email, password, this.typeInscription).subscribe({
      next: () => {
        this.isLoading = false;
        if (this.typeInscription === 'ECOLE') {
          this.router.navigate(['/accreditation']);
        } else if (this.typeInscription === 'ENTREPRISE') {
          this.router.navigate(['/espace-entreprise']);
        } else {
          this.router.navigate(['/recrutement/sessions']);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Une erreur est survenue lors de l\'inscription.';
      }
    });
  }
}
