import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Annonce } from '../../models/annonce';
import { AnnonceService } from '../../services/annonce.service';
import { Cohorte } from '../../models/cohorte';
import { Auth } from '../../../../core/auth/services/authService';

@Component({
  selector: 'app-annonces',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './annonces.html',
  styleUrl: './annonces.css',
})
export class Annonces implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);

  // Cette page est accessible en route partagee (/espace-etudiant/annonces, avec
  // la navbar commune) ET en route imbriquee sous l'espace formateur (sa propre
  // sidebar tient lieu de navigation) : on masque la navbar commune dans ce
  // second cas pour ne pas dupliquer la navigation.

  annonces: Annonce[] = [];
  afficherFormulaire = false;
  annonceForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';

  mesCohortes: Cohorte[] = [];
  cohorteIdsSelectionnees = new Set<number>();

  constructor(private annonceService: AnnonceService, private fb: FormBuilder) {
    this.annonceForm = this.fb.group({
      titre: ['', Validators.required],
      contenu: ['', Validators.required],
      epinglee: [false],
    });
  }

  ngOnInit(): void {
    this.chargerAnnonces();
    if (this.isFormateurRole) {
      this.annonceService.getMesCohortes().subscribe({
        next: (list) => (this.mesCohortes = list || []),
        error: () => (this.mesCohortes = []),
      });
    }
  }

  toggleCohorte(id: number, checked: boolean): void {
    if (checked) {
      this.cohorteIdsSelectionnees.add(id);
    } else {
      this.cohorteIdsSelectionnees.delete(id);
    }
  }

  private chargerAnnonces(): void {
    this.annonceService.getAll().subscribe({
      next: (data) => (this.annonces = data || []),
      error: () => (this.annonces = []),
    });
  }

  get isFormateurRole(): boolean {
    return this.auth.isFormateur();
  }

  ouvrirFormulaire(): void {
    this.errorMessage = '';
    this.cohorteIdsSelectionnees.clear();
    this.annonceForm.reset({ titre: '', contenu: '', epinglee: false });
    this.afficherFormulaire = true;
  }

  fermerFormulaire(): void {
    this.afficherFormulaire = false;
  }

  onSubmit(): void {
    if (this.annonceForm.invalid) {
      this.annonceForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    const payload = {
      ...this.annonceForm.getRawValue(),
      cohorteIds: Array.from(this.cohorteIdsSelectionnees),
    };
    this.annonceService.create(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.afficherFormulaire = false;
        this.chargerAnnonces();
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || "La publication de l'annonce a échoué.";
      },
    });
  }
}
