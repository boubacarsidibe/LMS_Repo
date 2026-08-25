import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
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

  errorMessage: string = '';
  isLoading: boolean = false;

  /**
   * Un compte tout juste créé n'a encore aucun rôle métier (attribué
   * uniquement à la soumission réelle d'une candidature ou d'une demande
   * d'accréditation) : sans ce choix, redirectByRole renvoyait aussi bien un
   * futur candidat qu'un représentant d'école vers l'accueil générique, sans
   * aucune indication sur la suite à donner.
   */
  typeInscription: 'CANDIDAT' | 'ECOLE' = 'CANDIDAT';

  // Déclaration de tous les contrôles liés au HTML
  registerForm: FormGroup = this.fb.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', Validators.required]
  });

  choisirTypeInscription(type: 'CANDIDAT' | 'ECOLE'): void {
    this.typeInscription = type;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const { prenom, nom, email, password } = this.registerForm.value;

    // Génération automatique d'un username à partir du prénom/nom si ton backend en a besoin
    const username = `${prenom.trim()}_${nom.trim()}`.toLowerCase();

    this.authService.register(username, email, password, this.typeInscription).subscribe({
      next: () => {
        this.isLoading = false;
        if (this.typeInscription === 'ECOLE') {
          this.router.navigate(['/accreditation']);
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