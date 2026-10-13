import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import {
  CandidatureResponse,
  ConvocationEntretienGroupeeRequest,
  EntretienRequest,
  EntretienResponse,
  FormatRapport,
  RecrutementService,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { ReportButtonComponent } from '../../../../shared/components/report-button/report-button.component';

interface EntretienAffiche extends EntretienResponse {
  candidatNom: string;
  candidatPrenom: string;
  numeroDossier: string;
}

@Component({
  selector: 'app-responsable-admission-entretiens',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule, ReportButtonComponent],
  templateUrl: './responsable-admission-entretiens.component.html',
})
export class ResponsableAdmissionEntretiensComponent implements OnInit {
  entretiens: EntretienResponse[] = [];
  candidatures: CandidatureResponse[] = [];
  loading = false;
  errorMessage = '';
  generatingReport = false;
  private chargementsEnCours = 0;

  recherche = '';
  filtre: 'TOUS' | 'A_VENIR' | 'PASSES' = 'TOUS';

  drawerOpen = false;
  entretienEnEdition: EntretienResponse | null = null;
  entretienForm: FormGroup;
  isSubmitting = false;
  formError = '';

  convocationGroupeeOpen = false;
  selectionCandidatIds = new Set<number>();
  convocationForm: FormGroup;
  envoiGroupeEnCours = false;
  convocationFormError = '';

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder, private router: Router,
              private notifications: NotificationService) {
    this.entretienForm = this.fb.group({
      candidatId: ['', Validators.required],
      dateHeure: ['', Validators.required],
      lieuOuLien: ['', Validators.required],
      noteEntretien: [null],
      appreciationGlobale: [''],
    });
    this.convocationForm = this.fb.group({
      dateHeure: ['', Validators.required],
      lieuOuLien: ['', Validators.required],
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

  /** Candidats convoqués à l'entretien (CONVOQUE_ENTRETIEN), éligibles pour une planification. */
  get candidatsEligibles(): CandidatureResponse[] {
    return this.candidatures.filter((c) => c.statut === 'CONVOQUE_ENTRETIEN');
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

  ouvrirEntretien(entretien: EntretienResponse): void {
    this.router.navigate(['/responsable-admission/entretiens', entretien.id]);
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

  supprimer(entretien: EntretienResponse): void {
    if (!confirm("Supprimer cet entretien ?")) return;
    this.recrutementService.supprimerEntretien(entretien.id).subscribe({
      next: () => this.charger(),
      error: () => this.notifications.erreur("Échec de l'annulation de l'entretien."),
    });
  }

  /**
   * Saisie de la note directement dans la liste, après un entretien groupé —
   * confiée à {@code app-ui-note}, comme la grille des tests académiques, pour que
   * les deux ne redivergent pas.
   *
   * <p>Propriété fléchée et non méthode : le gabarit la passe en entrée du champ,
   * et une fonction recréée à chaque cycle de détection y relancerait un
   * enregistrement en boucle.
   */
  readonly enregistrerNoteEntretien = (
    note: number | null,
    contexte: unknown,
  ): Observable<number | null> => {
    const entretien = contexte as EntretienAffiche;
    return this.recrutementService.modifierNoteEntretien(entretien.id, note).pipe(
      tap((maj) => {
        const original = this.entretiens.find((e) => e.id === entretien.id);
        if (original) original.noteEntretien = maj.noteEntretien;
      }),
      map((maj) => maj.noteEntretien),
    );
  };

  // ===================== Convocation groupée =====================

  ouvrirConvocationGroupee(): void {
    this.convocationFormError = '';
    this.selectionCandidatIds.clear();
    this.convocationForm.reset({ dateHeure: '', lieuOuLien: '' });
    this.convocationGroupeeOpen = true;
  }

  fermerConvocationGroupee(): void {
    this.convocationGroupeeOpen = false;
  }

  basculerSelection(candidatId: number): void {
    if (this.selectionCandidatIds.has(candidatId)) {
      this.selectionCandidatIds.delete(candidatId);
    } else {
      this.selectionCandidatIds.add(candidatId);
    }
  }

  toutSelectionner(): void {
    if (this.selectionCandidatIds.size === this.candidatsEligibles.length) {
      this.selectionCandidatIds.clear();
    } else {
      this.candidatsEligibles.forEach((c) => this.selectionCandidatIds.add(c.candidatId));
    }
  }

  envoyerConvocationGroupee(): void {
    if (this.convocationForm.invalid || this.selectionCandidatIds.size === 0) {
      this.convocationForm.markAllAsTouched();
      this.convocationFormError = this.selectionCandidatIds.size === 0 ? 'Sélectionnez au moins un candidat.' : '';
      return;
    }
    this.envoiGroupeEnCours = true;
    this.convocationFormError = '';
    const brut = this.convocationForm.getRawValue();
    const request: ConvocationEntretienGroupeeRequest = {
      candidatIds: Array.from(this.selectionCandidatIds),
      dateHeure: brut.dateHeure,
      lieuOuLien: brut.lieuOuLien,
    };
    this.recrutementService.convoquerEntretiensGroupe(request).subscribe({
      next: () => {
        this.envoiGroupeEnCours = false;
        this.fermerConvocationGroupee();
        this.charger();
      },
      error: (err) => {
        this.envoiGroupeEnCours = false;
        this.convocationFormError = err.error?.message || "Échec de l'envoi des convocations.";
      },
    });
  }

  genererRapport(format: FormatRapport): void {
    this.generatingReport = true;
    this.recrutementService.genererRapportEntretiens(format).subscribe({
      next: (blob) => {
        downloadBlob(blob, `rapport_entretiens_${new Date().toISOString().slice(0, 10)}.${format}`);
        this.generatingReport = false;
      },
      error: () => {
        this.generatingReport = false;
        this.notifications.erreur('Échec de la génération du rapport.');
      },
    });
  }
}
