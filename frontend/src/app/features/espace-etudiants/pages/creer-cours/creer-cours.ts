import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CoursService } from '../../services/cours.service';

@Component({
  selector: 'app-creer-cours',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './creer-cours.html',
  styleUrl: './creer-cours.css',
})
export class CreerCours {
  creerCoursForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';

  constructor(private fb: FormBuilder, private router: Router, private coursService: CoursService) {
    this.creerCoursForm = this.fb.group({
      code: ['', Validators.required],
      titre: ['', Validators.required],
      semestre: ['', Validators.required],
      description: [''],
      volumeHoraire: [0, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit() {
    if (this.creerCoursForm.invalid) {
      this.creerCoursForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.errorMessage = '';
    this.coursService.create(this.creerCoursForm.getRawValue()).subscribe({
      next: () => this.router.navigate(['/espace-etudiant/formateur/mes-cours']),
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || 'La création du cours a échoué.';
      }
    });
  }

  annuler() {
    this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
  }
}
