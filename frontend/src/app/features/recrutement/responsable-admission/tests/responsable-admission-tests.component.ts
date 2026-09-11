import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import {
  CandidatureResponse,
  ConvocationTestGroupeeRequest,
  FormatRapport,
  RecrutementService,
  SessionCandidatureResponse,
  TestAcademiqueResponse,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { ReportButtonComponent } from '../../../../shared/components/report-button/report-button.component';
import { UiNoteComponent } from '../../../../shared/components/ui/ui-note.component';

interface TestAffiche extends TestAcademiqueResponse {
  candidatNom: string;
  candidatPrenom: string;
  numeroDossier: string;
}

/** Seuil d'admissibilité par défaut si la session du candidat n'est pas identifiable. */
const SEUIL_ADMISSIBILITE_DEFAUT = 10;

@Component({
  selector: 'app-responsable-admission-tests',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule, ReportButtonComponent, UiNoteComponent],
  templateUrl: './responsable-admission-tests.component.html',
})
export class ResponsableAdmissionTestsComponent implements OnInit {
  tests: TestAcademiqueResponse[] = [];
  candidatures: CandidatureResponse[] = [];
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  errorMessage = '';
  generatingReport = false;
  private chargementsEnCours = 0;

  recherche = '';
  /** Id du test dont la note est en cours d'enregistrement (grille de notation). */
  noteEnCoursId: number | null = null;
  /**
   * Désactivée par défaut : lors d'une saisie en série (clic, écriture, tabulation,
   * clic suivant...), interrompre chaque note insuffisante par une boîte de
   * confirmation casse le rythme et force une décision avant même d'avoir vu
   * l'ensemble des résultats. Notes enregistrées silencieusement ; le badge
   * Résultat (déjà recalculé pour chaque ligne) reste cliquable pour évaluer une
   * fois toute la saisie terminée. Activable pour qui préfère traiter chaque rejet
   * au fil de l'eau.
   */
  evaluationDirecte = false;

  convocationGroupeeOpen = false;
  selectionCandidatIds = new Set<number>();
  convocationForm: FormGroup;
  envoiGroupeEnCours = false;
  convocationFormError = '';

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder, private router: Router,
              private notifications: NotificationService) {
    this.convocationForm = this.fb.group({
      nomEpreuve: ['', Validators.required],
      coefficient: [1, [Validators.required, Validators.min(0.1)]],
      dateEpreuve: ['', Validators.required],
      lieuOuLien: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.chargementsEnCours = 3;

    this.recrutementService.getAllTests().subscribe({
      next: (data) => (this.tests = data || []),
      error: () => {
        this.tests = [];
        this.errorMessage = 'Impossible de charger les tests académiques.';
      },
      complete: () => this.finDeChargement(),
    });

    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => (this.candidatures = []),
      complete: () => this.finDeChargement(),
    });

    this.recrutementService.getAllSessions().subscribe({
      next: (data) => (this.sessions = data || []),
      error: () => (this.sessions = []),
      complete: () => this.finDeChargement(),
    });
  }

  /** Seuil de réussite (note/20) de la session à laquelle appartient le candidat. */
  private seuilPour(candidatId: number): number {
    const candidature = this.candidatureDe(candidatId);
    const session = candidature ? this.sessions.find((s) => s.id === candidature.sessionCandidatureId) : undefined;
    return session?.seuilReussiteTest ?? SEUIL_ADMISSIBILITE_DEFAUT;
  }

  private finDeChargement(): void {
    this.chargementsEnCours--;
    if (this.chargementsEnCours <= 0) {
      this.loading = false;
    }
  }

  private candidatureDe(candidatId: number): CandidatureResponse | undefined {
    return this.candidatures.find((c) => c.candidatId === candidatId);
  }

  get testsAffiches(): TestAffiche[] {
    return this.tests.map((t) => {
      const c = this.candidatureDe(t.candidatId);
      return {
        ...t,
        candidatNom: c?.candidatNom || '—',
        candidatPrenom: c?.candidatPrenom || '',
        numeroDossier: c?.numeroDossier || '',
      };
    });
  }

  get testsFiltres(): TestAffiche[] {
    const q = this.recherche.trim().toLowerCase();
    if (!q) return this.testsAffiches;
    return this.testsAffiches.filter((t) => {
      const texte = `${t.candidatPrenom} ${t.candidatNom} ${t.nomEpreuve} ${t.numeroDossier}`.toLowerCase();
      return texte.includes(q);
    });
  }

  /** Candidats admis au Test Académique (ADMIS1), éligibles pour une planification d'épreuve. */
  get candidatsEligibles(): CandidatureResponse[] {
    return this.candidatures.filter((c) => c.statut === 'ADMIS1');
  }

  get totalTests(): number {
    return this.tests.length;
  }

  get nbNotes(): number {
    return this.tests.filter((t) => t.noteObtenue !== null && t.noteObtenue !== undefined).length;
  }

  get nbAdmissibles(): number {
    return this.tests.filter((t) => t.noteObtenue !== null && t.noteObtenue !== undefined
      && t.noteObtenue >= this.seuilPour(t.candidatId)).length;
  }

  resultat(t: TestAcademiqueResponse): 'ADMISSIBLE' | 'INSUFFISANT' | 'EN_ATTENTE' {
    if (t.noteObtenue === null || t.noteObtenue === undefined) return 'EN_ATTENTE';
    return t.noteObtenue >= this.seuilPour(t.candidatId) ? 'ADMISSIBLE' : 'INSUFFISANT';
  }

  voirTest(test: TestAcademiqueResponse): void {
    this.router.navigate(['/responsable-admission/tests', test.id]);
  }

  /**
   * Saisie de note en ligne, directement dans le tableau — clic sur la cellule, on
   * tape la note, elle s'enregistre au blur. Volontairement en input non contrôlé
   * ([value] + (blur), pas [(ngModel)]) : {@link testsAffiches}/{@link testsFiltres}
   * renvoient des copies (spread) recalculées à chaque cycle de détection, un
   * ngModel s'y écrirait dans le vide et l'input se réinitialiserait sous la saisie.
   */
  /**
   * Enregistrement d'une note de la grille, confié à {@code app-ui-note} — qui gère
   * la saisie (virgule décimale, plage autorisée, restauration après refus) et
   * l'affichage de l'état.
   *
   * <p>Propriété fléchée et non méthode : le gabarit la passe en entrée du champ,
   * et une fonction recréée à chaque cycle de détection y relancerait un
   * enregistrement en boucle.
   */
  readonly enregistrerNote = (note: number | null, contexte: unknown): Observable<number | null> => {
    const test = contexte as TestAffiche;
    return this.recrutementService.modifierNoteTest(test.id, note).pipe(
      tap((maj) => {
        const original = this.tests.find((t) => t.id === test.id);
        if (original) {
          original.noteObtenue = maj.noteObtenue;
          // Recopié aussi : sans lui, le bouton « annuler » de la ligne gardait
          // l'état d'avant la saisie et ne proposait pas de revenir en arrière.
          original.noteObtenuePrecedente = maj.noteObtenuePrecedente;
        }

        // Note en dessous du seuil : par défaut on n'interrompt pas la saisie en
        // série (voir evaluationDirecte) — le badge Résultat, déjà recalculé,
        // reste cliquable pour évaluer une fois la série terminée.
        if (
          this.evaluationDirecte &&
          maj.noteObtenue !== null &&
          maj.noteObtenue < this.seuilPour(test.candidatId)
        ) {
          this.proposerRejetSousSeuil(test.candidatId, maj.noteObtenue);
        }
      }),
      map((maj) => maj.noteObtenue),
    );
  };

  /** Annule la dernière saisie de note (à tout moment, pas seulement juste après). */
  annulerNote(test: TestAffiche): void {
    this.noteEnCoursId = test.id;
    this.recrutementService.annulerNoteTest(test.id).subscribe({
      next: (maj) => {
        const original = this.tests.find((t) => t.id === test.id);
        if (original) {
          original.noteObtenue = maj.noteObtenue;
          original.noteObtenuePrecedente = maj.noteObtenuePrecedente;
        }
        this.noteEnCoursId = null;
      },
      error: () => {
        this.noteEnCoursId = null;
        this.notifications.erreur("Échec de l'annulation de la note.");
      },
    });
  }

  /** Évaluation à la demande — clic sur le badge « Insuffisant » d'une ligne déjà notée. */
  evaluerMaintenant(test: TestAffiche): void {
    if (test.noteObtenue == null) return;
    this.proposerRejetSousSeuil(test.candidatId, test.noteObtenue);
  }

  private proposerRejetSousSeuil(candidatId: number, note: number): void {
    const candidature = this.candidatureDe(candidatId);
    if (!candidature || candidature.statut === 'REJETE') return;
    const seuil = this.seuilPour(candidatId);
    const confirme = confirm(
      `${candidature.candidatPrenom} ${candidature.candidatNom} obtient ${note}/20, en dessous du seuil de réussite (${seuil}/20).\n\n` +
      `Rejeter automatiquement son dossier et l'en informer par email ?`
    );
    if (!confirme) return;
    this.recrutementService.rejeterDossier(candidature.id, `Note insuffisante au test académique (${note}/20, seuil requis : ${seuil}/20).`)
      .subscribe({
        next: () => this.charger(),
        error: () => this.notifications.erreur('Échec du rejet automatique du dossier.'),
      });
  }

  supprimer(test: TestAffiche): void {
    if (!confirm(`Supprimer le test "${test.nomEpreuve}" de ${test.candidatPrenom} ${test.candidatNom} ?`)) return;
    this.recrutementService.supprimerTest(test.id).subscribe({
      next: () => this.charger(),
      error: () => this.notifications.erreur('Échec de la suppression du test.'),
    });
  }

  // ===================== Convocation groupée =====================

  ouvrirConvocationGroupee(): void {
    this.convocationFormError = '';
    this.selectionCandidatIds.clear();
    this.convocationForm.reset({ nomEpreuve: '', coefficient: 1, dateEpreuve: '', lieuOuLien: '' });
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
    const request: ConvocationTestGroupeeRequest = {
      candidatIds: Array.from(this.selectionCandidatIds),
      nomEpreuve: brut.nomEpreuve,
      coefficient: Number(brut.coefficient),
      dateEpreuve: brut.dateEpreuve,
      lieuOuLien: brut.lieuOuLien,
    };
    this.recrutementService.convoquerTestsGroupe(request).subscribe({
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
    this.recrutementService.genererRapportTests(format).subscribe({
      next: (blob) => {
        downloadBlob(blob, `rapport_tests_${new Date().toISOString().slice(0, 10)}.${format}`);
        this.generatingReport = false;
      },
      error: () => {
        this.generatingReport = false;
        this.notifications.erreur('Échec de la génération du rapport.');
      },
    });
  }
}
