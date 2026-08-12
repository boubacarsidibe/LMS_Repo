import { Component, ViewChild, ElementRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Cours } from '../../models/cours';
import { CoursService } from '../../services/cours.service';
import { SupportService } from '../../services/support.service';

@Component({
  selector: 'app-mes-cours',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './mes-cours.html',
  styleUrl: './mes-cours.css',
})
export class MesCours implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  cours: Cours[] = [];

  depotForm: FormGroup;
  fichierSelectionne: File | null = null;
  isUploading = false;
  uploadMessage = '';
  uploadError = '';

  constructor(
    private fb: FormBuilder,
    private coursService: CoursService,
    private supportService: SupportService
  ) {
    this.depotForm = this.fb.group({
      typeRessource: ['pdf', Validators.required],
      coursCible: ['', Validators.required],
      datePublication: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadCours();
  }

  private loadCours(): void {
    this.coursService.getAll().subscribe({
      next: (data) => {
        this.cours = data || [];
      },
      error: () => {
        this.cours = [];
      }
    });
  }

  ouvrirExplorateur() {
    this.fileInput.nativeElement.click();
  }

  onFichierSelectionne(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.fichierSelectionne = input.files[0];
    }
  }

  onSubmit() {
    if (this.depotForm.invalid || !this.fichierSelectionne) {
      this.depotForm.markAllAsTouched();
      return;
    }

    this.isUploading = true;
    this.uploadError = '';
    this.uploadMessage = '';
    const { coursCible, typeRessource, datePublication } = this.depotForm.getRawValue();
    this.supportService.uploadCourseSupport(
      Number(coursCible),
      typeRessource,
      datePublication,
      this.fichierSelectionne
    ).subscribe({
      next: () => {
        this.isUploading = false;
        this.uploadMessage = 'La ressource a été publiée.';
        this.fichierSelectionne = null;
        this.depotForm.reset({ typeRessource: 'pdf', coursCible: '', datePublication: '' });
        this.fileInput.nativeElement.value = '';
        this.loadCours();
      },
      error: (error) => {
        this.isUploading = false;
        this.uploadError = error.error?.message || 'La publication de la ressource a échoué.';
      }
    });
  }

  recherche: string = '';
  semestreSelectionne: string = 'Semestre 1 - 2024';
  cohorteSelectionnee: string = 'Toutes les Cohortes';

  get coursFiltres(): Cours[] {
    return this.cours.filter(c => {
      const correspondRecherche = 
        c.titre.toLowerCase().includes(this.recherche.toLowerCase()) ||
        c.code.toLowerCase().includes(this.recherche.toLowerCase());
      return correspondRecherche;
    });
  }
}
