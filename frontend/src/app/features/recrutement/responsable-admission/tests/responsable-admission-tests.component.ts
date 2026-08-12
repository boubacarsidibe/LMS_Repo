import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

interface Epreuve {
  nom: string;
  date: string;
  heure: string;
  candidats: number;
  statut: string;
  coefficient: number;
  seuil: number;
}

interface NoteRow {
  id: number;
  nom: string;
  initials: string;
  noteObtenue: number;
  coefficient: number;
  decision: string;
  saved: boolean;
}

interface TestKpi {
  epreuvesActives: number;
  moyenneGenerale: number;
  notesSaisiesPct: number;
  prochainTest: string;
}

@Component({
  selector: 'app-responsable-admission-tests',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission-tests.component.html',
})
export class ResponsableAdmissionTestsComponent implements OnInit, OnDestroy {
  activeTab: 'epreuves' | 'notes' = 'epreuves';
  selectedProgram = 'all';
  selectedSession = '';
  selectedExam = '';
  searchNotesQuery = '';
  filterDecision = '';
  seuilAdmission = 60;

  kpis: TestKpi = { epreuvesActives: 0, moyenneGenerale: 0, notesSaisiesPct: 0, prochainTest: 'N/A' };
  chargementKpis = false;
  chargement = false;

  epreuves: Epreuve[] = [];
  noteRows: NoteRow[] = [];

  modalPlanExamOpen = false;
  newExam = { nomEpreuve: '', dateEpreuve: '', coefficient: 1, sessionId: null as number | null, seuil: 60 };

  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  private subs: Subscription[] = [];

  constructor(private service: RecrutementService) {}

  ngOnInit(): void {
    this.chargerKpis();
    this.chargerEpreuves();
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  chargerKpis(): void {
    this.chargementKpis = true;
    const sub = this.service.getAcademicTestKpis().subscribe({
      next: (d) => { this.kpis = d; this.chargementKpis = false; },
      error: () => { this.chargementKpis = false; }
    });
    this.subs.push(sub);
  }

  chargerEpreuves(): void {
    this.chargement = true;
    const sub = this.service.listAcademicExams().subscribe({
      next: (data: any[]) => {
        this.epreuves = data.map(e => ({
          nom: e.nom, date: e.date, heure: e.heure, candidats: e.candidats,
          statut: e.statut, coefficient: e.coefficient || 1, seuil: e.seuil || 60
        }));
        this.chargement = false;
      },
      error: () => { this.chargement = false; }
    });
    this.subs.push(sub);
  }

  chargerScores(nomEpreuve: string): void {
    this.chargement = true;
    const sub = this.service.getExamScores(nomEpreuve, this.seuilAdmission).subscribe({
      next: (data: any[]) => {
        this.noteRows = data.map(s => ({
          id: s.id, nom: `${s.candidatNom} ${s.candidatPrenom}`,
          initials: (s.candidatPrenom?.charAt(0)||'')+(s.candidatNom?.charAt(0)||''),
          noteObtenue: s.noteObtenue || 0, coefficient: s.coefficient || 1,
          decision: s.decision || 'AJOURNE', saved: true
        }));
        this.chargement = false;
      },
      error: () => { this.chargement = false; }
    });
    this.subs.push(sub);
  }

  get filteredNoteRows(): NoteRow[] {
    return this.noteRows.filter(r => {
      const q = this.searchNotesQuery.toLowerCase();
      return (!q || r.nom.toLowerCase().includes(q))
        && (!this.filterDecision || r.decision === this.filterDecision);
    });
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'TERMINE': return 'bg-success/10 text-success border-success/20';
      case 'EN_COURS': return 'bg-primary/10 text-primary border-primary/20';
      case 'PROGRAMME': return 'bg-surface-container-highest text-on-surface-variant border-surface-container-highest';
      default: return '';
    }
  }

  switchTab(tab: 'epreuves' | 'notes'): void { this.activeTab = tab; }

  onExamSelect(nomEpreuve: string): void {
    this.selectedExam = nomEpreuve;
    this.chargerScores(nomEpreuve);
    this.switchTab('notes');
  }

  onGradeChange(row: NoteRow): void {
    row.decision = row.noteObtenue >= this.seuilAdmission ? 'ADMIS' : 'AJOURNE';
    row.saved = false;
  }

  saveRow(row: NoteRow): void {
    this.service.saveExamScores(this.selectedExam, [{ id: row.id, noteObtenue: row.noteObtenue }]).subscribe({
      next: () => { row.saved = true; this.afficherToast('Note sauvegardée.', 'success'); },
      error: () => this.afficherToast('Erreur sauvegarde.', 'error')
    });
  }

  saveAllNotes(): void {
    const scores = this.noteRows.map(r => ({ id: r.id, noteObtenue: r.noteObtenue }));
    this.service.saveExamScores(this.selectedExam, scores).subscribe({
      next: (res: any) => { this.noteRows.forEach(r => r.saved = true); this.afficherToast(res.message, 'success'); this.chargerKpis(); },
      error: () => this.afficherToast('Erreur sauvegarde.', 'error')
    });
  }

  openPlanExamModal(): void { this.modalPlanExamOpen = true; this.newExam = { nomEpreuve: '', dateEpreuve: '', coefficient: 1, sessionId: null, seuil: 60 }; }
  createExam(): void {
    if (!this.newExam.nomEpreuve) { this.afficherToast('Nom requis.', 'error'); return; }
    const body: any = { nomEpreuve: this.newExam.nomEpreuve, coefficient: this.newExam.coefficient, dateEpreuve: this.newExam.dateEpreuve + 'T09:00:00', seuil: this.newExam.seuil };
    if (this.newExam.sessionId) body.sessionId = this.newExam.sessionId;
    this.service.createAcademicExam(body).subscribe({
      next: () => { this.modalPlanExamOpen = false; this.afficherToast('Épreuve planifiée.', 'success'); this.chargerEpreuves(); this.chargerKpis(); },
      error: () => this.afficherToast('Erreur création.', 'error')
    });
  }

  changerStatutEpreuve(nom: string, status: string): void {
    this.service.changeExamStatus(nom, status).subscribe({
      next: () => { this.afficherToast(`Statut : ${status}.`, 'success'); this.chargerEpreuves(); },
      error: () => this.afficherToast('Erreur statut.', 'error')
    });
  }

  afficherToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message; this.toastType = type; this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 4000);
  }
}