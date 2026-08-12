import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  RecrutementService,
  DashboardOverview,
  SessionSummary,
  CandidatureResume,
  TestSummary,
  EntretienSummary,
  Program,
} from '../../../../core/recrutement/services/recrutement.service';

interface CandidatureAffichee {
  id: number;
  initials: string;
  nom: string;
  email: string;
  dossier: string;
  filiere: string;
  statut: string;
  statutCouleur: 'blue' | 'emerald' | 'warning' | 'error';
}

interface DocumentCandidat {
  nom: string;
  valide: boolean;
}

interface NewSession {
  nom: string;
  programme: string;
  programId: number | null;
  dateDebut: string;
  dateFin: string;
  quota: number;
  status: string;
  description: string;
}

@Component({
  selector: 'app-responsable-admission',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission.component.html',
})
export class ResponsableAdmissionComponent implements OnInit, OnDestroy {
  // ---- Données du dashboard ----
  dashboardData: DashboardOverview | null = null;
  chargement = false;
  erreurChargement = '';

  // Session
  sessionActive = true;
  sessionLibelle = 'Session Principale d\'Automne 2026';
  dateSession = '15 Mai - 30 Septembre 2026';
  quotaActuel = 186;
  quotaMax = 220;
  sessionId: number | null = null;
  vueGraphique = false;

  // Candidatures
  filterPanelOpen = false;
  activeActionMenu: number | null = null;
  candidateDrawerOpen = false;
  selectedCandidate: CandidatureAffichee | null = null;
  modalNewSessionOpen = false;
  modalCloseSessionOpen = false;
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  // Filters
  filterSearch = '';
  filterStatus = '';
  filterFormation = '';

  candidaturesRecentes: CandidatureAffichee[] = [];
  candidateDocuments: DocumentCandidat[] = [
    { nom: 'CV & Lettre de motivation', valide: true },
    { nom: 'Relevé de notes L3', valide: true },
    { nom: 'Pièce d\'Identité', valide: true },
    { nom: 'Diplôme du Baccalauréat', valide: false },
  ];

  // Tests & Entretiens
  testsAcademiques: TestSummary[] = [];
  entretiensJury: EntretienSummary[] = [];

  // ---- Nouvelle session ----
  programsForModal: Program[] = [];
  newSession: NewSession = {
    nom: '',
    programme: 'Master 1 Data Science & AI',
    programId: null,
    dateDebut: '',
    dateFin: '',
    quota: 50,
    status: 'BROUILLON',
    description: '',
  };

  private subs: Subscription[] = [];

  constructor(
    private router: Router,
    private recrutementService: RecrutementService
  ) {}

  ngOnInit(): void {
    this.chargerDashboard();
  }

  ngOnDestroy(): void {
    this.subs.forEach((s) => s.unsubscribe());
  }

  // ====================================
  // CHARGEMENT DES DONNÉES
  // ====================================

  chargerDashboard(): void {
    this.chargement = true;
    this.erreurChargement = '';
    const sub = this.recrutementService.getDashboardOverview().subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.chargement = false;

        if (data.sessionActive) {
          this.sessionId = data.sessionActive.id;
          this.sessionLibelle = data.sessionActive.libelle;
          this.sessionActive = data.sessionActive.estOuverte;
          this.quotaMax = data.sessionActive.quotaMax;
          this.quotaActuel = data.sessionActive.quotaActuel;
          this.dateSession =
            data.sessionActive.dateOuverture +
            ' - ' +
            data.sessionActive.dateFermeture;
        } else {
          this.sessionActive = false;
          this.sessionLibelle = 'Aucune session active';
        }

        this.candidaturesRecentes = (data.candidaturesRecentes || []).map(
          (c) => this.toCandidatureAffichee(c)
        );

        this.testsAcademiques = data.testsAcademiques || [];
        this.entretiensJury = data.entretiens || [];
      },
      error: (err) => {
        this.chargement = false;
        this.erreurChargement =
          'Impossible de charger les données du dashboard.';
        console.error('[Dashboard] Erreur chargement:', err);
      },
    });
    this.subs.push(sub);
  }

  // ====================================
  // SESSION
  // ====================================

  openNewSessionModal(): void {
    this.modalNewSessionOpen = true;
    this.newSession = { nom: '', programme: 'Master 1 Data Science & AI', programId: null, dateDebut: '', dateFin: '', quota: 50, status: 'BROUILLON', description: '' };
    // Charger la liste des programmes pour le select
    this.recrutementService.getPrograms().subscribe({
      next: (data) => (this.programsForModal = data),
      error: () => console.warn('[Overview] Impossible de charger les programmes'),
    });
  }

  createSession(): void {
    if (!this.newSession.nom || !this.newSession.dateDebut) {
      this.afficherToast('Veuillez remplir tous les champs obligatoires.', 'error');
      return;
    }
    const body: any = {
      libelle: this.newSession.nom,
      dateOuverture: this.newSession.dateDebut,
      dateFermeture: this.newSession.dateFin || this.newSession.dateDebut,
      quotaMax: this.newSession.quota,
      programId: this.newSession.programId,
      status: this.newSession.status,
    };
    const sub = this.recrutementService.createSession(body).subscribe({
      next: (session: any) => {
        this.modalNewSessionOpen = false;
        this.sessionActive = true;
        this.sessionId = session.id;
        this.sessionLibelle = session.libelle;
        this.quotaMax = session.quotaMax;
        this.quotaActuel = session.quotaActuel;
        this.afficherToast('Session créée avec succès !', 'success');
        this.chargerDashboard();
      },
      error: (err: any) => {
        console.error('[Dashboard] Erreur création session:', err);
        this.afficherToast('Erreur lors de la création de la session.', 'error');
      },
    });
    this.subs.push(sub);
  }

  closeSession(): void {
    this.modalCloseSessionOpen = true;
  }

  confirmCloseSession(): void {
    if (!this.sessionId) {
      this.modalCloseSessionOpen = false;
      this.sessionActive = false;
      this.afficherToast('Session clôturée.', 'success');
      return;
    }
    const sub = this.recrutementService.closeSession(this.sessionId).subscribe({
      next: () => {
        this.modalCloseSessionOpen = false;
        this.sessionActive = false;
        this.afficherToast('Session clôturée avec succès.', 'success');
        this.chargerDashboard();
      },
      error: (err) => {
        console.error('[Dashboard] Erreur fermeture session:', err);
        this.modalCloseSessionOpen = false;
        this.sessionActive = false;
        this.afficherToast('Session clôturée localement.', 'success');
      },
    });
    this.subs.push(sub);
  }

  toggleVueGraphique(): void {
    this.vueGraphique = !this.vueGraphique;
  }

  get pourcentageQuota(): number {
    if (this.quotaMax === 0) return 0;
    return Math.round((this.quotaActuel / this.quotaMax) * 100);
  }

  // ====================================
  // CANDIDATURES
  // ====================================

  get filteredCandidatures(): CandidatureAffichee[] {
    return this.candidaturesRecentes.filter((c) => {
      const s = this.filterSearch.toLowerCase();
      const matchSearch =
        !s ||
        c.nom.toLowerCase().includes(s) ||
        c.dossier.toLowerCase().includes(s) ||
        c.email.toLowerCase().includes(s);
      const matchStatus =
        !this.filterStatus || c.statut.includes(this.filterStatus);
      const matchFormation =
        !this.filterFormation || c.filiere.includes(this.filterFormation);
      return matchSearch && matchStatus && matchFormation;
    });
  }

  get activeFilterCount(): number {
    let count = 0;
    if (this.filterSearch) count++;
    if (this.filterStatus) count++;
    if (this.filterFormation) count++;
    return count;
  }

  toggleFilterPanel(event: MouseEvent): void {
    event.stopPropagation();
    this.filterPanelOpen = !this.filterPanelOpen;
  }

  resetFilters(): void {
    this.filterSearch = '';
    this.filterStatus = '';
    this.filterFormation = '';
    this.filterPanelOpen = false;
  }

  applyFilters(): void {
    this.filterPanelOpen = false;
  }

  toggleActionMenu(candidateId: number, event: MouseEvent): void {
    event.stopPropagation();
    this.activeActionMenu =
      this.activeActionMenu === candidateId ? null : candidateId;
  }

  openCandidateDrawer(cand: CandidatureAffichee): void {
    this.selectedCandidate = cand;
    this.candidateDrawerOpen = true;
    this.activeActionMenu = null;
  }

  closeCandidateDrawer(): void {
    this.candidateDrawerOpen = false;
    this.selectedCandidate = null;
  }

  voirDossier(cand: CandidatureAffichee, event: MouseEvent): void {
    event.stopPropagation();
    this.openCandidateDrawer(cand);
  }

  programmerTest(cand: CandidatureAffichee, event: MouseEvent): void {
    event.stopPropagation();
    this.activeActionMenu = null;
    this.router.navigate(['/responsable-admission/tests']);
  }

  rejeterCandidature(cand: CandidatureAffichee, event: MouseEvent): void {
    event.stopPropagation();
    this.activeActionMenu = null;
    this.recrutementService.rejeterDossier(cand.id, 'Rejeté depuis le dashboard').subscribe({
      next: () => {
        this.afficherToast(`Candidature ${cand.dossier} rejetée.`, 'success');
        this.chargerDashboard();
      },
      error: (err) => {
        console.error('[Dashboard] Erreur rejet:', err);
        this.afficherToast('Erreur lors du rejet.', 'error');
      },
    });
  }

  // ====================================
  // TESTS & ENTRETIENS
  // ====================================

  openTestDetails(): void {
    this.router.navigate(['/responsable-admission/tests']);
  }

  openEntretienDetails(): void {
    this.router.navigate(['/responsable-admission/entretiens']);
  }

  // ====================================
  // TOAST
  // ====================================

  afficherToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => {
      this.toastVisible = false;
    }, 4000);
  }

  // ====================================
  // HELPERS
  // ====================================

  private toCandidatureAffichee(c: CandidatureResume): CandidatureAffichee {
    const nomComplet = `${c.nomCandidat} ${c.prenomCandidat}`.trim() || 'Inconnu';
    const initiales =
      (c.prenomCandidat?.charAt(0) || '') + (c.nomCandidat?.charAt(0) || '');
    let couleur: CandidatureAffichee['statutCouleur'] = 'blue';
    const statutLower = c.statut.toLowerCase();
    if (
      statutLower.includes('admis') ||
      statutLower.includes('test réussi')
    ) {
      couleur = 'emerald';
    } else if (statutLower.includes('rejet')) {
      couleur = 'error';
    } else if (statutLower.includes('attente') || statutLower.includes('brouillon')) {
      couleur = 'warning';
    }

    return {
      id: c.id,
      initials: initiales || '??',
      nom: nomComplet,
      email: c.email || '',
      dossier: c.numeroDossier,
      filiere: c.formation || 'Non spécifiée',
      statut: c.statut,
      statutCouleur: couleur,
    };
  }

  // ====================================
  // GLOBAL CLICK HANDLER
  // ====================================

  // ====================================
  // QUICK PROGRAM CREATE
  // ====================================

  showQuickProgramForm = false;
  quickProgram = { title: '', level: '', department: '' };

  openQuickProgramCreate(event: MouseEvent): void {
    event.stopPropagation();
    this.showQuickProgramForm = !this.showQuickProgramForm;
  }

  createQuickProgram(): void {
    if (!this.quickProgram.title) {
      this.afficherToast('Le titre du programme est obligatoire.', 'error');
      return;
    }
    this.recrutementService.getPrograms().subscribe({
      next: () => {
        // Use the HTTP POST directly
        const http = (this.recrutementService as any).http;
        if (http) {
          http.post('/api/settings/programs', this.quickProgram).subscribe({
            next: (newProg: Program) => {
              this.programsForModal.push(newProg);
              this.newSession.programId = newProg.id;
              this.showQuickProgramForm = false;
              this.quickProgram = { title: '', level: '', department: '' };
              this.afficherToast('Programme créé et associé.', 'success');
            },
            error: () => this.afficherToast('Erreur lors de la création du programme.', 'error'),
          });
        }
      },
      error: () => this.afficherToast('Erreur API.', 'error'),
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('button')) {
      this.activeActionMenu = null;
    }
    if (!target.closest('.relative') || !target.closest('button')) {
      this.filterPanelOpen = false;
    }
  }
}