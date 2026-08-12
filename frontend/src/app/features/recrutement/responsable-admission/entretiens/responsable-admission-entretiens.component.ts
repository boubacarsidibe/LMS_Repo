import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription, Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

interface InterviewRow {
  id: number;
  candidatNom: string;
  candidatPrenom: string;
  date: string;
  heure: string;
  lieuOuLien: string;
  statut: string;
  noteEntretien: number | null;
  appreciationGlobale: string;
}

interface InterviewKpi {
  totalEntretiens: number;
  effectues: number;
  aujourdhui: number;
  enAttente: number;
}

@Component({
  selector: 'app-responsable-admission-entretiens',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission-entretiens.component.html',
})
export class ResponsableAdmissionEntretiensComponent implements OnInit, OnDestroy {
  // KPIs
  kpis: InterviewKpi = { totalEntretiens: 0, effectues: 0, aujourdhui: 0, enAttente: 0 };
  chargementKpis = false;

  // Tabs & Filters
  activeTab: 'ALL' | 'PLANNED' | 'COMPLETED' = 'ALL';
  searchQuery = '';
  page = 0;
  limit = 10;
  totalCount = 0;
  private searchSubject = new Subject<string>();

  // Modals / Drawer
  modalPlanOpen = false;
  modalEvalOpen = false;
  selectedInterview: InterviewRow | null = null;
  newInterview = { dateHeure: '', lieuOuLien: 'Salle A', candidatId: null as number | null };
  evalForm = { noteEntretien: 0, appreciationGlobale: '', decision: 'ADMIS' };

  // Data
  entretiens: InterviewRow[] = [];
  chargement = false;
  emptyMessage = '';

  // Toast
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  private subs: Subscription[] = [];

  constructor(private service: RecrutementService) {}

  ngOnInit(): void {
    this.chargerKpis();
    this.chargerEntretiens();
    const debSub = this.searchSubject.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => { this.page = 0; this.chargerEntretiens(); });
    this.subs.push(debSub);
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  chargerKpis(): void {
    this.chargementKpis = true;
    const sub = this.service.getInterviewKpis().subscribe({
      next: d => { this.kpis = d; this.chargementKpis = false; },
      error: () => { this.chargementKpis = false; }
    });
    this.subs.push(sub);
  }

  chargerEntretiens(): void {
    this.chargement = true;
    this.emptyMessage = '';
    const sub = this.service.listInterviews(this.activeTab, this.searchQuery || undefined, this.page, this.limit).subscribe({
      next: (data: any[]) => {
        this.entretiens = data.map(e => ({
          id: e.id,
          candidatNom: e.candidatNom || '', candidatPrenom: e.candidatPrenom || '',
          date: e.date || '', heure: e.heure || '', lieuOuLien: e.lieuOuLien || '',
          statut: e.statut || 'PLANIFIE', noteEntretien: e.noteEntretien, appreciationGlobale: e.appreciationGlobale || ''
        }));
        this.chargement = false;
        if (this.entretiens.length === 0) this.emptyMessage = 'Aucun entretien trouvé.';
      },
      error: () => { this.chargement = false; }
    });
    this.subs.push(sub);
  }

  switchTab(tab: 'ALL' | 'PLANNED' | 'COMPLETED'): void {
    this.activeTab = tab;
    this.page = 0;
    this.chargerEntretiens();
  }

  onSearchChange(): void { this.searchSubject.next(this.searchQuery); }

  prevPage(): void { if (this.page > 0) { this.page--; this.chargerEntretiens(); } }
  nextPage(): void { this.page++; this.chargerEntretiens(); }

  // ---- Plan interview ----
  openPlanModal(): void {
    this.modalPlanOpen = true;
    this.newInterview = { dateHeure: '', lieuOuLien: 'Salle A', candidatId: null };
  }

  createInterview(): void {
    const body: any = { dateHeure: this.newInterview.dateHeure + ':00', lieuOuLien: this.newInterview.lieuOuLien };
    if (this.newInterview.candidatId) body.candidatId = this.newInterview.candidatId;
    this.service.createInterview(body).subscribe({
      next: () => {
        this.modalPlanOpen = false;
        this.afficherToast('Entretien planifié.', 'success');
        this.chargerEntretiens(); this.chargerKpis();
      },
      error: () => this.afficherToast('Erreur création.', 'error')
    });
  }

  // ---- Evaluate ----
  openEvalModal(interview: InterviewRow): void {
    this.selectedInterview = interview;
    this.evalForm = { noteEntretien: interview.noteEntretien || 0, appreciationGlobale: interview.appreciationGlobale || '', decision: 'ADMIS' };
    this.modalEvalOpen = true;
  }

  saveEvaluation(): void {
    if (!this.selectedInterview) return;
    this.service.saveEvaluation(this.selectedInterview.id, this.evalForm).subscribe({
      next: () => {
        this.modalEvalOpen = false;
        this.afficherToast('Évaluation enregistrée.', 'success');
        this.chargerEntretiens(); this.chargerKpis();
      },
      error: () => this.afficherToast('Erreur évaluation.', 'error')
    });
  }

  // ---- Delete ----
  deleteInterview(id: number): void {
    if (!confirm('Annuler cet entretien ?')) return;
    this.service.deleteInterview(id).subscribe({
      next: () => { this.afficherToast('Entretien annulé.', 'success'); this.chargerEntretiens(); this.chargerKpis(); },
      error: () => this.afficherToast('Erreur suppression.', 'error')
    });
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'TERMINE': return 'bg-success/10 text-success border-success/20';
      case 'PLANIFIE': return 'bg-primary/10 text-primary border-primary/20';
      default: return 'bg-surface-container-highest text-on-surface-variant';
    }
  }

  afficherToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message; this.toastType = type; this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 4000);
  }
}