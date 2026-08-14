import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { estDansEspaceFormateur } from '../../utils/contexte-navigation';
import { Certificat } from '../../models/certificat';
import { CertificatService } from '../../services/certificat.service';
import { Etudiant } from '../../models/etudiant';
import { EtudiantService } from '../../services/etudiant.service';
import { Cohorte } from '../../models/cohorte';
import { CohorteService } from '../../services/cohorte.service';
import { Auth } from '../../../../core/auth/services/authService';
import { EspaceNavbarComponent } from '../../../../shared/components/espace-navbar/espace-navbar.component';

@Component({
  selector: 'app-certificats',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EspaceNavbarComponent],
  templateUrl: './certificats.html',
  styleUrl: './certificats.css',
})
export class Certificats implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);

  // Voir Annonces pour le detail de ce mecanisme.
  readonly dansEspaceFormateur = estDansEspaceFormateur(this.router);

  certificats: Certificat[] = [];
  etudiants: Etudiant[] = [];
  cohortes: Cohorte[] = [];
  afficherFormulaire = false;
  certificatForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  telechargementEnCoursId: number | null = null;

  constructor(
    private certificatService: CertificatService,
    private etudiantService: EtudiantService,
    private cohorteService: CohorteService,
    private fb: FormBuilder
  ) {
    this.certificatForm = this.fb.group({
      etudiantId: [null, Validators.required],
      intituleFormation: ['', Validators.required],
      cohorteId: [null],
    });
  }

  ngOnInit(): void {
    this.chargerCertificats();
    if (this.isFormateurRole) {
      this.etudiantService.getAll().subscribe({ next: list => this.etudiants = list, error: () => this.etudiants = [] });
      this.cohorteService.getAll().subscribe({ next: list => this.cohortes = list, error: () => this.cohortes = [] });
    }
  }

  private chargerCertificats(): void {
    this.certificatService.getAll().subscribe({
      next: (data) => (this.certificats = data || []),
      error: () => (this.certificats = []),
    });
  }

  get isFormateurRole(): boolean {
    return this.auth.isFormateur();
  }

  ouvrirFormulaire(): void {
    this.errorMessage = '';
    this.certificatForm.reset({ etudiantId: null, intituleFormation: '', cohorteId: null });
    this.afficherFormulaire = true;
  }

  fermerFormulaire(): void {
    this.afficherFormulaire = false;
  }

  onSubmit(): void {
    if (this.certificatForm.invalid) {
      this.certificatForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.certificatService.create(this.certificatForm.getRawValue()).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.afficherFormulaire = false;
        this.chargerCertificats();
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || "L'émission du certificat a échoué.";
      },
    });
  }

  telecharger(certificat: Certificat): void {
    this.telechargementEnCoursId = certificat.id;
    this.certificatService.telechargerPdf(certificat.id).subscribe({
      next: (blob) => {
        this.telechargementEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `certificat-${certificat.reference}.pdf`;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementEnCoursId = null;
      },
    });
  }
}
