import { Component, ViewChild, ElementRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Cours, CoursCreation } from '../../models/cours';
import { Support } from '../../models/support';
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
  gestionForm: FormGroup;
  coursSelectionne: Cours | null = null;
  supportsCours: Support[] = [];
  chargementSupports = false;
  isSavingCourse = false;
  gestionMessage = '';
  gestionError = '';
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

    this.gestionForm = this.fb.group({
      code: ['', Validators.required],
      titre: ['', Validators.required],
      semestre: [''],
      description: [''],
      volumeHoraire: [0, [Validators.required, Validators.min(0)]]
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

  gererCours(cours: Cours): void {
    this.coursSelectionne = cours;
    this.supportsCours = [];
    this.gestionMessage = '';
    this.gestionError = '';
    this.gestionForm.reset({
      code: cours.code,
      titre: cours.titre,
      semestre: cours.semestre || '',
      description: cours.description || '',
      volumeHoraire: cours.volumeHoraire ?? 0
    });
    this.chargementSupports = true;
    this.coursService.getSupports(cours.id).subscribe({
      next: supports => { this.supportsCours = supports || []; this.chargementSupports = false; },
      error: () => { this.gestionError = 'Impossible de charger les supports du cours.'; this.chargementSupports = false; }
    });
  }

  fermerGestion(): void {
    this.coursSelectionne = null;
    this.supportsCours = [];
  }

  enregistrerCours(): void {
    if (!this.coursSelectionne || this.gestionForm.invalid) {
      this.gestionForm.markAllAsTouched();
      return;
    }
    this.isSavingCourse = true;
    this.gestionMessage = '';
    this.gestionError = '';
    const data = this.gestionForm.getRawValue() as CoursCreation;
    this.coursService.update(this.coursSelectionne.id, data).subscribe({
      next: cours => {
        this.cours = this.cours.map(item => item.id === cours.id ? cours : item);
        this.coursSelectionne = cours;
        this.isSavingCourse = false;
        this.gestionMessage = 'Cours mis à jour.';
      },
      error: error => {
        this.isSavingCourse = false;
        this.gestionError = error.error?.message || 'La mise à jour du cours a échoué.';
      }
    });
  }

  supprimerCours(): void {
    if (!this.coursSelectionne || !confirm(`Supprimer le cours « ${this.coursSelectionne.titre} » et ses supports ?`)) {
      return;
    }
    this.isSavingCourse = true;
    this.coursService.delete(this.coursSelectionne.id).subscribe({
      next: () => { this.isSavingCourse = false; this.fermerGestion(); this.loadCours(); },
      error: error => {
        this.isSavingCourse = false;
        this.gestionError = error.error?.message || 'La suppression du cours a échoué.';
      }
    });
  }

  ajouterRessource(cours: Cours): void {
    this.depotForm.patchValue({ coursCible: String(cours.id) });
    document.getElementById('depot-rapide')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
