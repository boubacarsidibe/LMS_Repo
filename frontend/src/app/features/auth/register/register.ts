import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';

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

  /**
   * Parcours interrompu à reprendre après l'inscription (transmis par la page de
   * connexion, elle-même appelée depuis « Postuler » sur la vitrine des offres).
   * Prioritaire sur la redirection par {@link typeInscription}, qui ne connaît
   * que les trois parcours historiques.
   */
  private readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl');

  errorMessage: string = '';
  isLoading: boolean = false;

  /**
   * Un compte tout juste créé n'a encore aucun rôle métier (attribué
   * uniquement à la soumission réelle d'une candidature ou d'une demande
   * d'accréditation) : sans ce choix, redirectByRole renvoyait aussi bien un
   * futur candidat qu'un représentant d'école vers l'accueil générique, sans
   * aucune indication sur la suite à donner.
   */
  typeInscription: 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE' | 'FORMATEUR' = 'CANDIDAT';

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
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', Validators.required]
  });

  choisirTypeInscription(type: 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE' | 'FORMATEUR'): void {
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
    if (this.registerForm.invalid) {
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
        // Une URL interne seulement : « // » désignerait un autre site.
        if (this.returnUrl && this.returnUrl.startsWith('/') && !this.returnUrl.startsWith('//')) {
          this.router.navigateByUrl(this.returnUrl);
        } else if (this.typeInscription === 'ECOLE') {
          this.router.navigate(['/accreditation']);
        } else if (this.typeInscription === 'ENTREPRISE') {
          this.router.navigate(['/espace-entreprise']);
        } else if (this.typeInscription === 'FORMATEUR') {
          // Pas d'offre déjà choisie (sinon returnUrl l'aurait emportée ci-dessus) :
          // direction la vitrine pour en sélectionner une.
          this.router.navigate(['/offres-formateurs']);
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