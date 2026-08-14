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
  imageSelectionnee: File | null = null;
  imageApercu: string | null = null;

  constructor(private fb: FormBuilder, private router: Router, private coursService: CoursService) {
    this.creerCoursForm = this.fb.group({
      code: ['', Validators.required],
      titre: ['', Validators.required],
      semestre: ['', Validators.required],
      description: [''],
      volumeHoraire: [0, [Validators.required, Validators.min(0)]]
    });
  }

  onImageSelectionnee(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.imageSelectionnee = input.files[0];
      const reader = new FileReader();
      reader.onload = () => this.imageApercu = reader.result as string;
      reader.readAsDataURL(this.imageSelectionnee);
    }
  }

  onSubmit() {
    if (this.creerCoursForm.invalid) {
      this.creerCoursForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.errorMessage = '';
    this.coursService.create(this.creerCoursForm.getRawValue()).subscribe({
      next: (coursCree) => {
        if (this.imageSelectionnee) {
          this.coursService.uploadImage(coursCree.id, this.imageSelectionnee).subscribe({
            next: () => this.router.navigate(['/espace-etudiant/formateur/mes-cours']),
            error: () => {
              // Le cours est créé même si l'image échoue : on n'en fait pas un blocage.
              console.error("L'image n'a pas pu être associée au cours.");
              this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
            }
          });
        } else {
          this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
        }
      },
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