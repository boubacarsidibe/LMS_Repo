import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  CritereAdmissionRequest,
  CritereAdmissionResponse,
  EnumTypeCritere,
  RecrutementService,
  SessionCandidatureRequest,
  SessionCandidatureResponse,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { ColonneExport, ExportColumnsModalComponent } from '../../../../shared/components/export-columns-modal/export-columns-modal.component';

@Component({
  selector: 'app-responsable-admission-sessions',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule, ExportColumnsModalComponent],
  templateUrl: './responsable-admission-sessions.component.html',
})
export class ResponsableAdmissionSessionsComponent implements OnInit {
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  errorMessage = '';

  recherche = '';
  filtreStatut: 'TOUTES' | 'OUVERTES' | 'FERMEES' | 'ACTIVES' = 'TOUTES';

  drawerOpen = false;
  sessionEnEdition: SessionCandidatureResponse | null = null;
  sessionForm: FormGroup;
  isSubmitting = false;
  formError = '';

  exporting = false;
  exportModalOpen = false;

  readonly colonnesExport: ColonneExport[] = [
    { key: 'libelle', label: 'Libellé' },
    { key: 'dateOuverture', label: "Date d'ouverture" },
    { key: 'dateFermeture', label: 'Date de fermeture' },
    { key: 'estOuverte', label: 'Ouverte' },
    { key: 'estActive', label: 'Active' },
    { key: 'nbCandidatures', label: 'Nb candidatures' },
  ];
  readonly colonnesExportParDefaut = ['libelle', 'dateOuverture', 'dateFermeture', 'estOuverte', 'estActive', 'nbCandidatures'];

  readonly niveaux = ['Baccalauréat', 'Licence (L3)', 'Master 1 (M1)', 'Master 2 (M2)'];

  triColonne: 'libelle' | 'dateOuverture' | 'dateFermeture' | 'nbCandidatures' = 'dateOuverture';
  triDesc = true;

  // Critères personnalisés (n'existent que pour une session déjà créée)
  criteresSession: CritereAdmissionResponse[] = [];
  critereForm: FormGroup;
  critereEnEdition: CritereAdmissionResponse | null = null;
  critereFormOpen = false;
  critereError = '';
  critereSubmitting = false;
  filtreEnCours = false;
  filtreMessage = '';

  readonly typesCritere: { valeur: EnumTypeCritere; libelle: string }[] = [
    { valeur: 'TEXTE', libelle: 'Texte libre' },
    { valeur: 'NUMERIQUE', libelle: 'Nombre' },
    { valeur: 'BOOLEEN', libelle: 'Oui / Non' },
    { valeur: 'CHOIX', libelle: 'Choix parmi une liste' },
  ];

  constructor(private recrutementService: RecrutementService, private fb: FormBuilder) {
    this.sessionForm = this.fb.group({
      libelle: ['', Validators.required],
      dateOuverture: ['', Validators.required],
      dateFermeture: ['', Validators.required],
      estOuverte: [true],
      criteresActifs: [false],
      criteresNiveauEtudeMinimum: [null],
      criteresAgeMinimum: [null],
      criteresAgeMaximum: [null],
      criteresGenre: [null],
      criteresExperienceMinimale: [null],
      seuilReussiteTest: [10],
      infoFraisScolarite: [null],
      infoCarteEtudiante: [null],
      infoRentree: [null],
    });

    this.critereForm = this.fb.group({
      libelle: ['', Validators.required],
      type: ['TEXTE' as EnumTypeCritere, Validators.required],
      obligatoire: [true],
      options: [null],
      valeurMinimale: [null],
      valeurAttendue: [null],
      ordre: [0],
    });
  }

  ngOnInit(): void {
    this.chargerSessions();
  }

  private chargerSessions(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getAllSessions().subscribe({
      next: (data) => {
        this.sessions = data || [];
        this.loading = false;
      },
      error: () => {
        this.sessions = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger les sessions.';
      },
    });
  }

  get sessionsFiltrees(): SessionCandidatureResponse[] {
    const q = this.recherche.trim().toLowerCase();
    const filtrees = this.sessions.filter((s) => {
      if (q && !s.libelle.toLowerCase().includes(q)) return false;
      switch (this.filtreStatut) {
        case 'OUVERTES':
          return s.estOuverte;
        case 'FERMEES':
          return !s.estOuverte;
        case 'ACTIVES':
          return s.estActive;
        default:
          return true;
      }
    });
    const sens = this.triDesc ? -1 : 1;
    return [...filtrees].sort((a, b) => {
      const colonne = this.triColonne;
      if (colonne === 'nbCandidatures') return (a.nbCandidatures - b.nbCandidatures) * sens;
      return a[colonne].localeCompare(b[colonne]) * sens;
    });
  }

  trierPar(colonne: 'libelle' | 'dateOuverture' | 'dateFermeture' | 'nbCandidatures'): void {
    if (this.triColonne === colonne) {
      this.triDesc = !this.triDesc;
    } else {
      this.triColonne = colonne;
      this.triDesc = false;
    }
  }

  get totalSessions(): number {
    return this.sessions.length;
  }

  get nbOuvertes(): number {
    return this.sessions.filter((s) => s.estOuverte).length;
  }

  get nbActives(): number {
    return this.sessions.filter((s) => s.estActive).length;
  }

  /** Session ouverte dont la date de fermeture est la plus proche dans le futur. */
  get prochaineFermeture(): SessionCandidatureResponse | null {
    const aujourdhui = new Date().toISOString().slice(0, 10);
    const candidates = this.sessions
      .filter((s) => s.estOuverte && s.dateFermeture >= aujourdhui)
      .sort((a, b) => a.dateFermeture.localeCompare(b.dateFermeture));
    return candidates.length > 0 ? candidates[0] : null;
  }

  openDrawer(session?: SessionCandidatureResponse): void {
    this.formError = '';
    this.criteresSession = [];
    this.filtreMessage = '';
    this.closeCritereForm();
    if (session) {
      this.sessionEnEdition = session;
      this.chargerCriteres(session.id);
      this.sessionForm.setValue({
        libelle: session.libelle,
        dateOuverture: session.dateOuverture,
        dateFermeture: session.dateFermeture,
        estOuverte: session.estOuverte,
        criteresActifs: session.criteresActifs,
        criteresNiveauEtudeMinimum: session.criteresNiveauEtudeMinimum,
        criteresAgeMinimum: session.criteresAgeMinimum,
        criteresAgeMaximum: session.criteresAgeMaximum,
        criteresGenre: session.criteresGenre,
        criteresExperienceMinimale: session.criteresExperienceMinimale,
        seuilReussiteTest: session.seuilReussiteTest ?? 10,
        infoFraisScolarite: session.infoFraisScolarite,
        infoCarteEtudiante: session.infoCarteEtudiante,
        infoRentree: session.infoRentree,
      });
    } else {
      this.sessionEnEdition = null;
      this.sessionForm.reset({
        libelle: '', dateOuverture: '', dateFermeture: '', estOuverte: true,
        criteresActifs: false, criteresNiveauEtudeMinimum: null, criteresAgeMinimum: null, criteresAgeMaximum: null,
        criteresGenre: null, criteresExperienceMinimale: null, seuilReussiteTest: 10,
        infoFraisScolarite: null, infoCarteEtudiante: null, infoRentree: null,
      });
    }
    this.drawerOpen = true;
  }

  closeDrawer(): void {
    this.drawerOpen = false;
  }

  enregistrer(): void {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.formError = '';
    const request: SessionCandidatureRequest = this.sessionForm.getRawValue();

    const obs = this.sessionEnEdition
      ? this.recrutementService.modifierSession(this.sessionEnEdition.id, request)
      : this.recrutementService.creerSession(request);

    obs.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.closeDrawer();
        this.chargerSessions();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err.error?.message || "Échec de l'enregistrement de la session.";
      },
    });
  }

  supprimer(session: SessionCandidatureResponse): void {
    if (!confirm(`Supprimer définitivement la session "${session.libelle}" ?`)) return;
    this.recrutementService.supprimerSession(session.id).subscribe({
      next: () => this.chargerSessions(),
      error: () => alert('Échec de la suppression de la session.'),
    });
  }

  // ===================== CRITÈRES PERSONNALISÉS =====================

  private chargerCriteres(sessionId: number): void {
    this.recrutementService.getCriteresDeSession(sessionId).subscribe({
      next: (data) => (this.criteresSession = data || []),
      error: () => (this.criteresSession = []),
    });
  }

  openCritereForm(critere?: CritereAdmissionResponse): void {
    this.critereError = '';
    if (critere) {
      this.critereEnEdition = critere;
      this.critereForm.setValue({
        libelle: critere.libelle,
        type: critere.type,
        obligatoire: critere.obligatoire,
        options: critere.options,
        valeurMinimale: critere.valeurMinimale,
        valeurAttendue: critere.valeurAttendue,
        ordre: critere.ordre,
      });
    } else {
      this.critereEnEdition = null;
      this.critereForm.reset({
        libelle: '', type: 'TEXTE', obligatoire: true,
        options: null, valeurMinimale: null, valeurAttendue: null, ordre: this.criteresSession.length,
      });
    }
    this.critereFormOpen = true;
  }

  closeCritereForm(): void {
    this.critereFormOpen = false;
    this.critereEnEdition = null;
  }

  enregistrerCritere(): void {
    if (!this.sessionEnEdition || this.critereForm.invalid) {
      this.critereForm.markAllAsTouched();
      return;
    }
    this.critereSubmitting = true;
    this.critereError = '';
    const request: CritereAdmissionRequest = this.critereForm.getRawValue();

    const obs = this.critereEnEdition
      ? this.recrutementService.modifierCritere(this.critereEnEdition.id, request)
      : this.recrutementService.creerCritere(this.sessionEnEdition.id, request);

    obs.subscribe({
      next: () => {
        this.critereSubmitting = false;
        this.closeCritereForm();
        this.chargerCriteres(this.sessionEnEdition!.id);
      },
      error: (err) => {
        this.critereSubmitting = false;
        this.critereError = err.error?.message || "Échec de l'enregistrement du critère.";
      },
    });
  }

  retirerCritereListe(critere: CritereAdmissionResponse): void {
    if (!this.sessionEnEdition) return;
    if (!confirm(`Retirer le critère "${critere.libelle}" du formulaire de candidature ?`)) return;
    this.recrutementService.retirerCritere(critere.id).subscribe({
      next: () => this.chargerCriteres(this.sessionEnEdition!.id),
      error: () => alert('Échec du retrait du critère.'),
    });
  }

  reactiverCritereListe(critere: CritereAdmissionResponse): void {
    if (!this.sessionEnEdition) return;
    this.recrutementService.reactiverCritere(critere.id).subscribe({
      next: () => this.chargerCriteres(this.sessionEnEdition!.id),
      error: () => alert('Échec de la réactivation du critère.'),
    });
  }

  appliquerFiltre(): void {
    if (!this.sessionEnEdition) return;
    if (!confirm(
      `Appliquer le filtre automatique sur les dossiers "Soumis" de "${this.sessionEnEdition.libelle}" ? ` +
      'Chaque dossier qui ne remplit pas les critères sera automatiquement rejeté ; les autres passeront en présélection.'
    )) return;

    this.filtreEnCours = true;
    this.filtreMessage = '';
    this.recrutementService.appliquerFiltreCriteres(this.sessionEnEdition.id).subscribe({
      next: (resultat) => {
        this.filtreEnCours = false;
        this.filtreMessage = `${resultat.total} dossier(s) évalué(s) : ${resultat.preselectionnes} en présélection, ${resultat.rejetes} rejeté(s).`;
        this.chargerSessions();
      },
      error: (err) => {
        this.filtreEnCours = false;
        this.filtreMessage = err.error?.message || "Échec de l'application du filtre.";
      },
    });
  }

  exporter(colonnes: string[]): void {
    this.exporting = true;
    this.recrutementService.exporterSessions(colonnes).subscribe({
      next: (blob) => {
        downloadBlob(blob, `sessions_recrutement_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        alert("Échec de l'export Excel.");
      },
    });
  }
}
