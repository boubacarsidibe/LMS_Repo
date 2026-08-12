import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription, Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import {
  RecrutementService,
  SessionsKpi,
  SessionRow,
  Program,
} from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-responsable-admission-sessions',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission-sessions.component.html',
})
export class ResponsableAdmissionSessionsComponent implements OnInit, OnDestroy {
  // ---- KPIs ----
  kpis: SessionsKpi = { totalSessionsActives: 0, totalCandidatures: 0, tauxRemplissage: 0, prochaineCloture: 'N/A' };
  chargementKpis = false;

  // ---- Sessions ----
  sessions: SessionRow[] = [];
  chargement = false;
  erreurChargement = '';
  emptyMessage = '';

  // ---- Filtres ----
  searchQuery = '';
  filterStatus = '';
  filterLevel = '';
  private searchSubject = new Subject<string>();

  // ---- Modals / Drawer ----
  drawerOpen = false;
  editingSession: SessionRow | null = null;
  isCreating = false;
  modalDeleteOpen = false;
  deleteTarget: SessionRow | null = null;

  // ---- Nouvelle/édition session ----
  newSession = { libelle: '', dateOuverture: '', dateFermeture: '', quotaMax: 50, status: 'BROUILLON', programId: null as number | null, description: '' };
  programs: Program[] = [];
  showQuickProgramForm = false;
  quickProgram = { title: '', level: '', department: '' };

  // ---- Toast ----
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  // ---- Actions menu ----
  activeActionMenu: number | null = null;

  private subs: Subscription[] = [];

  constructor(private service: RecrutementService) {}

  ngOnInit(): void {
    this.chargerKpis();
    this.chargerSessions();
    this.chargerPrograms();
    const debSub = this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.chargerSessions());
    this.subs.push(debSub);
  }

  ngOnDestroy(): void { this.subs.forEach((s) => s.unsubscribe()); }

  chargerKpis(): void {
    this.chargementKpis = true;
    const sub = this.service.getSessionKpis().subscribe({
      next: (data) => { this.kpis = data; this.chargementKpis = false; },
      error: () => { this.chargementKpis = false; this.kpis = { totalSessionsActives: 4, totalCandidatures: 432, tauxRemplissage: 68, prochaineCloture: '2026-09-30' }; },
    });
    this.subs.push(sub);
  }

  chargerSessions(): void {
    this.chargement = true; this.erreurChargement = ''; this.emptyMessage = '';
    const sub = this.service.listSessions({ q: this.searchQuery || undefined, status: this.filterStatus || undefined, level: this.filterLevel || undefined }).subscribe({
      next: (data) => { this.sessions = data; this.chargement = false; if (data.length === 0) this.emptyMessage = 'Aucune session trouvée.'; },
      error: (err) => { console.error('[Sessions] Erreur:', err); this.chargement = false; this.erreurChargement = 'Erreur lors du chargement des sessions.'; },
    });
    this.subs.push(sub);
  }

  chargerPrograms(): void {
    const sub = this.service.getPrograms().subscribe({ next: (data) => (this.programs = data), error: () => console.warn('[Sessions] Impossible de charger les programmes'), });
    this.subs.push(sub);
  }

  onSearchChange(): void { this.searchSubject.next(this.searchQuery); }
  onFilterChange(): void { this.chargerSessions(); }

  openDrawer(): void {
    this.isCreating = true; this.editingSession = null;
    this.newSession = { libelle: '', dateOuverture: '', dateFermeture: '', quotaMax: 50, status: 'BROUILLON', programId: null, description: '' };
    this.drawerOpen = true;
  }

  openEditDrawer(session: SessionRow): void {
    this.isCreating = false; this.editingSession = session;
    this.newSession = { libelle: session.libelle, dateOuverture: session.dateOuverture, dateFermeture: session.dateFermeture, quotaMax: session.quotaMax, status: session.status, programId: session.program?.id ?? null, description: (session as any).description || '' };
    this.drawerOpen = true;
  }

  closeDrawer(): void { this.drawerOpen = false; this.editingSession = null; this.isCreating = false; }

  saveSession(): void {
    if (!this.newSession.libelle) { this.afficherToast('Le libellé est obligatoire.', 'error'); return; }
    if (this.isCreating) {
      const sub = this.service.createSessionApi(this.newSession).subscribe({
        next: () => { this.closeDrawer(); this.afficherToast('Session créée avec succès.', 'success'); this.chargerSessions(); this.chargerKpis(); },
        error: (err) => { console.error('[Sessions] Erreur création:', err); this.afficherToast('Erreur lors de la création.', 'error'); },
      });
      this.subs.push(sub);
    } else if (this.editingSession) {
      const sub = this.service.updateSession(this.editingSession.id, this.newSession).subscribe({
        next: () => { this.closeDrawer(); this.afficherToast('Session mise à jour.', 'success'); this.chargerSessions(); this.chargerKpis(); },
        error: (err) => { console.error('[Sessions] Erreur mise à jour:', err); this.afficherToast('Erreur lors de la mise à jour.', 'error'); },
      });
      this.subs.push(sub);
    }
  }

  toggleActionMenu(sessionId: number, event: MouseEvent): void { event.stopPropagation(); this.activeActionMenu = this.activeActionMenu === sessionId ? null : sessionId; }
  closeActionMenu(): void { this.activeActionMenu = null; }

  changeStatus(session: SessionRow): void {
    this.activeActionMenu = null; const newStatus = session.status === 'OUVERT' ? 'CLOTURE' : 'OUVERT';
    const sub = this.service.changeSessionStatus(session.id, newStatus).subscribe({
      next: () => { this.afficherToast(`Statut changé : ${newStatus === 'OUVERT' ? 'Ouverte' : 'Clôturée'}.`, 'success'); this.chargerSessions(); this.chargerKpis(); },
      error: (err) => { console.error('[Sessions] Erreur changement statut:', err); this.afficherToast('Erreur lors du changement de statut.', 'error'); },
    });
    this.subs.push(sub);
  }

  duplicateSession(session: SessionRow): void {
    this.activeActionMenu = null;
    const sub = this.service.duplicateSession(session.id).subscribe({
      next: () => { this.afficherToast('Session dupliquée en brouillon.', 'success'); this.chargerSessions(); this.chargerKpis(); },
      error: (err) => { console.error('[Sessions] Erreur duplication:', err); this.afficherToast('Erreur lors de la duplication.', 'error'); },
    });
    this.subs.push(sub);
  }

  confirmDeleteSession(session: SessionRow): void { this.activeActionMenu = null; this.deleteTarget = session; this.modalDeleteOpen = true; }

  deleteSession(): void {
    if (!this.deleteTarget) return;
    const sub = this.service.deleteSession(this.deleteTarget.id).subscribe({
      next: () => { this.modalDeleteOpen = false; this.deleteTarget = null; this.afficherToast('Session supprimée.', 'success'); this.chargerSessions(); this.chargerKpis(); },
      error: (err) => { console.error('[Sessions] Erreur suppression:', err); this.modalDeleteOpen = false; this.afficherToast('Erreur : impossible de supprimer cette session (candidats inscrits ?).', 'error'); },
    });
    this.subs.push(sub);
  }

  exportSessions(): void {
    const sub = this.service.exportSessions().subscribe({
      next: (blob) => { const url = window.URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = 'sessions_export.csv'; a.click(); window.URL.revokeObjectURL(url); this.afficherToast('Export CSV téléchargé.', 'success'); },
      error: (err) => { console.error('[Sessions] Erreur export:', err); this.afficherToast('Erreur lors de l\'export.', 'error'); },
    });
    this.subs.push(sub);
  }

  getQuotaColor(taux: number): string { if (taux >= 100) return 'bg-error'; if (taux > 80) return 'bg-orange-400'; return 'bg-[#30609a]'; }

  getStatusBadge(status: string): string {
    switch (status) { case 'OUVERT': return 'bg-emerald-50 text-emerald-700 border-emerald-200'; case 'CLOTURE': return 'bg-[#fef2f2] text-[#e52020] border-[#e52020]/20'; case 'BROUILLON': return 'bg-surface-container text-on-surface-variant border-surface-variant'; case 'SUSPENDU': return 'bg-orange-50 text-orange-600 border-orange-200'; default: return 'bg-surface-container text-on-surface-variant'; }
  }

  afficherToast(message: string, type: 'success' | 'error'): void { this.toastMessage = message; this.toastType = type; this.toastVisible = true; setTimeout(() => (this.toastVisible = false), 4000); }

  // Quick program create
  createQuickProgram(): void {
    if (!this.quickProgram.title) { this.afficherToast('Le titre du programme est obligatoire.', 'error'); return; }
    const http = (this.service as any).http;
    if (http) {
      http.post('/api/settings/programs', this.quickProgram).subscribe({
        next: (newProg: Program) => { this.programs.push(newProg); this.newSession.programId = newProg.id; this.showQuickProgramForm = false; this.quickProgram = { title: '', level: '', department: '' }; this.afficherToast('Programme créé et associé.', 'success'); },
        error: () => this.afficherToast('Erreur création programme.', 'error')
      });
    }
  }

  onDocumentClick(event: MouseEvent): void { const target = event.target as HTMLElement; if (!target.closest('button')) this.activeActionMenu = null; }
}