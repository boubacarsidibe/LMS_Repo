import { Component, OnInit, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { estDansEspaceFormateur } from '../../utils/contexte-navigation';
import { Photo } from '../../models/photo';
import { PhotoService } from '../../services/photo.service';
import { Cohorte } from '../../models/cohorte';
import { CohorteService } from '../../services/cohorte.service';
import { Auth } from '../../../../core/auth/services/authService';
import { EspaceNavbarComponent } from '../../../../shared/components/espace-navbar/espace-navbar.component';

@Component({
  selector: 'app-phototheque',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, EspaceNavbarComponent],
  templateUrl: './phototheque.html',
  styleUrl: './phototheque.css',
})
export class Phototheque implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  private auth = inject(Auth);
  private router = inject(Router);

  // Voir Annonces pour le detail de ce mecanisme.
  readonly dansEspaceFormateur = estDansEspaceFormateur(this.router);

  photos: Photo[] = [];
  cohortes: Cohorte[] = [];
  afficherFormulaire = false;
  photoForm: FormGroup;
  fichierSelectionne: File | null = null;
  isSubmitting = false;
  errorMessage = '';

  constructor(
    private photoService: PhotoService,
    private cohorteService: CohorteService,
    private fb: FormBuilder
  ) {
    this.photoForm = this.fb.group({
      titre: ['', Validators.required],
      cohorteId: [null],
    });
  }

  ngOnInit(): void {
    this.chargerPhotos();
    this.cohorteService.getAll().subscribe({ next: list => this.cohortes = list, error: () => this.cohortes = [] });
  }

  private chargerPhotos(): void {
    this.photoService.getAll().subscribe({
      next: (data) => (this.photos = data || []),
      error: () => (this.photos = []),
    });
  }

  get isFormateurRole(): boolean {
    return this.auth.isFormateur();
  }

  ouvrirFormulaire(): void {
    this.errorMessage = '';
    this.fichierSelectionne = null;
    this.photoForm.reset({ titre: '', cohorteId: null });
    this.afficherFormulaire = true;
  }

  fermerFormulaire(): void {
    this.afficherFormulaire = false;
  }

  ouvrirExplorateur(): void {
    this.fileInput.nativeElement.click();
  }

  onFichierSelectionne(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.fichierSelectionne = input.files[0];
    }
  }

  onSubmit(): void {
    if (this.photoForm.invalid || !this.fichierSelectionne) {
      this.photoForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    const { titre, cohorteId } = this.photoForm.getRawValue();
    this.photoService.upload(titre, cohorteId, this.fichierSelectionne).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.afficherFormulaire = false;
        this.chargerPhotos();
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || "L'envoi de la photo a échoué.";
      },
    });
  }
}
