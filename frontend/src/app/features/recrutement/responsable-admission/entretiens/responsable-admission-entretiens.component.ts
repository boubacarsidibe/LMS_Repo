import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  EntretienRequest,
  EntretienResponse,
  RecrutementService,
} from '../../../../core/recrutement/services/recrutement.service';

interface EntretienAffiche extends EntretienResponse {
  candidatNom: string;
  candidatPrenom: string;
  numeroDossier: string;
}

@Component({
  selector: 'app-responsable-admission-entretiens',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './responsable-admission-entretiens.component.html',
})
export class ResponsableAdmissionEntretiensComponent implements OnInit {
  entretiens: EntretienResponse[] = [];
  candidatures: CandidatureResponse[] = [];
  loading = false;
  errorMessage = '';
  private chargementsEnCours = 0;

  recherche = '';
  filtre: 'TOUS' | 'A_VENIR' | 'PASSES' = 'TOUS';

  drawerOpen = false;
  entretienEnEdition: EntretienResponse | null = null;
  entretienForm: FormGroup;
  isSubmitting = false;
  formError = '';

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder) {
    this.entretienForm = this.fb.group({
      candidatId: ['', Validators.required],
      dateHeure: ['', Validators.required],
      lieuOuLien: ['', Validators.required],
      noteEntretien: [null],
      appreciationGlobale: [''],
    });
  }

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.chargementsEnCours = 2;

    this.recrutementService.getAllEntretiens().subscribe({
      next: (data) => (this.entretiens = data || []),
      error: () => {
        this.entretiens = [];
        this.errorMessage = 'Impossible de charger les entretiens.';
      },
      complete: () => this.finDeChargement(),
    });

    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => (this.candidatures = []),
      complete: () => this.finDeChargement(),
    });
  }

  private finDeChargement(): void {
    this.chargementsEnCours--;
    if (this.chargementsEnCours <= 0) {
      this.loading = false;
    }
  }

  /** Un candidat n'a pas de nom dans EntretienResponse : jointure côté client avec les candidatures. */
  private candidatureDe(candidatId: number): CandidatureResponse | undefined {
    return this.candidatures.find((c) => c.candidatId === candidatId);
  }

  get entretiensAffiches(): EntretienAffiche[] {
    return this.entretiens.map((e) => {
      const c = this.candidatureDe(e.candidatId);
      return {
        ...e,
        candidatNom: c?.candidatNom || '—',
        candidatPrenom: c?.candidatPrenom || '',
        numeroDossier: c?.numeroDossier || '',
      };
    });
  }

  get entretiensFiltres(): EntretienAffiche[] {
    const q = this.recherche.trim().toLowerCase();
    const maintenant = new Date();
    return this.entretiensAffiches.filter((e) => {
      if (q) {
        const texte = `${e.candidatPrenom} ${e.candidatNom} ${e.lieuOuLien} ${e.numeroDossier}`.toLowerCase();
        if (!texte.includes(q)) return false;
      }
      const estPasse = new Date(e.dateHeure) < maintenant;
      if (this.filtre === 'A_VENIR' && estPasse) return false;
      if (this.filtre === 'PASSES' && !estPasse) return false;
      return true;
    });
  }

  /** Candidats admis à l'entretien (ADMIS2), éligibles pour une planification. */
  get candidatsEligibles(): CandidatureResponse[] {
    return this.candidatures.filter((c) => c.statut === 'ADMIS2');
  }

  get totalEntretiens(): number {
    return this.entretiens.length;
  }

  get nbEffectues(): number {
    const maintenant = new Date();
    return this.entretiens.filter((e) => new Date(e.dateHeure) < maintenant).length;
  }

  get nbAVenir(): number {
    const maintenant = new Date();
    return this.entretiens.filter((e) => new Date(e.dateHeure) >= maintenant).length;
  }

  get nbAujourdhui(): number {
    const aujourdhui = new Date().toDateString();
    return this.entretiens.filter((e) => new Date(e.dateHeure).toDateString() === aujourdhui).length;
  }

  estPasse(dateHeure: string): boolean {
    return new Date(dateHeure) < new Date();
  }

  openDrawer(entretien?: EntretienResponse): void {
    this.formError = '';
    if (entretien) {
      this.entretienEnEdition = entretien;
      this.entretienForm.setValue({
        candidatId: entretien.candidatId,
        dateHeure: this.versInputDatetimeLocal(entretien.dateHeure),
        lieuOuLien: entretien.lieuOuLien,
        noteEntretien: entretien.noteEntretien ?? null,
        appreciationGlobale: entretien.appreciationGlobale || '',
      });
    } else {
      this.entretienEnEdition = null;
      this.entretienForm.reset({
        candidatId: '',
        dateHeure: '',
        lieuOuLien: '',
        noteEntretien: null,
        appreciationGlobale: '',
      });
    }
    this.drawerOpen = true;
  }

  closeDrawer(): void {
    this.drawerOpen = false;
  }

  private versInputDatetimeLocal(iso: string): string {
    // "2026-06-12T10:00:00" (backend) -> "2026-06-12T10:00" (attendu par <input type="datetime-local">)
    return iso ? iso.slice(0, 16) : '';
  }

  enregistrer(): void {
    if (this.entretienForm.invalid) {
      this.entretienForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.formError = '';
    const brut = this.entretienForm.getRawValue();
    const request: EntretienRequest = {
      ...brut,
      candidatId: Number(brut.candidatId),
      noteEntretien: brut.noteEntretien === null || brut.noteEntretien === '' ? null : Number(brut.noteEntretien),
    };

    const obs = this.entretienEnEdition
      ? this.recrutementService.modifierEntretien(this.entretienEnEdition.id, request)
      : this.recrutementService.creerEntretien(request);

    obs.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.closeDrawer();
        this.charger();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err.error?.message || "Échec de l'enregistrement de l'entretien.";
      },
    });
  }

  supprimer(entretien: EntretienAffiche): void {
    if (!confirm(`Annuler l'entretien de ${entretien.candidatPrenom} ${entretien.candidatNom} ?`)) return;
    this.recrutementService.supprimerEntretien(entretien.id).subscribe({
      next: () => this.charger(),
      error: () => alert("Échec de l'annulation de l'entretien."),
    });
  }
}
