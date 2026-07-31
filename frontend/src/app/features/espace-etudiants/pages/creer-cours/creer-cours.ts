import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-creer-cours',
  standalone: false,
  templateUrl: './creer-cours.html',
  styleUrl: './creer-cours.css',
})
export class CreerCours {
  creerCoursForm: FormGroup;

  constructor(private fb: FormBuilder, private router: Router) {
    this.creerCoursForm = this.fb.group({
      code: ['', Validators.required],
      titre: ['', Validators.required],
      semestre: ['', Validators.required],
      description: ['']
    });
  }

  onSubmit() {
    if (this.creerCoursForm.invalid) {
      this.creerCoursForm.markAllAsTouched();
      return;
    }
    console.log('Nouveau cours à créer :', this.creerCoursForm.value);
    // TODO : appel API réel vers le backend une fois l'endpoint disponible
    this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
  }

  annuler() {
    this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
  }
}