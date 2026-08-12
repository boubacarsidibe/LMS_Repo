import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription, Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

interface CandidatureRow {
  id: number;
  nom: string;
  initials: string;
  nationalite: string;
  idShort: string;
  formation: string;
  dateSoumission: string;
  pieces: string;
  paiement: string;
  selected: boolean;
}

interface DocumentItem {
  id: number;
  nom: string;
  valide: boolean;
}

interface ApplicationKpi {
  totalCandidatures: number;
  aVerifier: number;
  conformes: number;
  incomplets: number;
  relancesPotentielles: number;
}

@Component({
  selector: 'app-responsable-admission-candidatures',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission-candidatures.component.html',
})
export class ResponsableAdmissionCandidaturesComponent implements OnInit, OnDestroy {
  // ---- KPIs ----
  kpis: ApplicationKpi = { totalCandidatures: 0, aVerifier: 0, conformes: 0, incomplets: 0, relancesPotentielles: 0 };
  chargementKpis = false;

  drawerOpen = false;
  selectedCandidate: CandidatureRow | null = null;
  searchQuery = '';
  filterStatus = '';
  filterProgram = '';
  filterSession = '';
  private searchSubject = new Subject<string>();

  modalReminderOpen = false;
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  candidatures: CandidatureRow[] = [];
  chargement = false;

  documentsList: DocumentItem[] = [
    { id: 1, nom: 'CV & Lettre de motivation', valide: true },
    { id: 2, nom: 'Relevé de notes L3', valide: true },
    { id: 3, nom: 'Pièce d\'Identité', valide: true },
    { id: 4, nom: 'Diplôme du Baccalauréat', valide: false },
  ];

  private subs: Subscription[] = [];

  constructor(private service: RecrutementService) {}

  ngOnInit(): void {
    this.chargerKpis();
    this.chargerCandidatures();
    const debSub = this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.chargerCandidatures());
    this.subs.push(debSub);
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  chargerKpis(): void {
    this.chargementKpis = true;
    const sub = this.service.getApplicationKpis().subscribe({ next: d => { this.kpis = d; this.chargementKpis = false; }, error: () => { this.chargementKpis = false; } });
    this.subs.push(sub);
  }

  chargerCandidatures(): void {
    this.chargement = true;
    const sub = this.service.listApplications({ search: this.searchQuery || undefined, status: this.filterStatus || undefined, sessionId: undefined }).subscribe({
      next: (data: any[]) => {
        this.candidatures = data.map((c: any) => ({
          id: c.id, nom: `${c.candidatNom} ${c.candidatPrenom}`, initials: (c.candidatPrenom?.charAt(0)||'')+(c.candidatNom?.charAt(0)||''),
          nationalite: c.candidatNationalite || '', idShort: c.numeroDossier?.split('-').pop() || '',
          formation: c.program?.title || '', dateSoumission: c.dateSoumission ? new Date(c.dateSoumission).toLocaleDateString('fr') : '',
          pieces: c.documentStatus, paiement: c.paymentStatus, selected: false
        }));
        this.chargement = false;
      },
      error: () => { this.chargement = false; }
    });
    this.subs.push(sub);
  }

  get filteredCandidatures(): CandidatureRow[] {
    return this.candidatures.filter(c => {
      const s = this.searchQuery.toLowerCase();
      return (!s || c.nom.toLowerCase().includes(s) || c.idShort.includes(s))
        && (!this.filterProgram || c.formation.toLowerCase().includes(this.filterProgram.toLowerCase()))
        && (!this.filterStatus || c.pieces === this.filterStatus);
    });
  }

  get selectedCount(): number { return this.candidatures.filter(c => c.selected).length; }

  toggleSelectAll(event: Event): void { const checked = (event.target as HTMLInputElement).checked; this.candidatures.forEach(c => c.selected = checked); }
  updateSelection(): void {}

  onSearchChange(): void { this.searchSubject.next(this.searchQuery); }
  onFilterChange(): void { this.chargerCandidatures(); this.chargerKpis(); }
  resetFilters(): void { this.searchQuery = ''; this.filterStatus = ''; this.filterProgram = ''; this.filterSession = ''; this.chargerCandidatures(); this.chargerKpis(); }

  openCandidateDrawer(cand: CandidatureRow): void { this.selectedCandidate = cand; this.drawerOpen = true; }
  closeDrawer(): void { this.drawerOpen = false; this.selectedCandidate = null; }

  openReminderModal(): void { this.modalReminderOpen = true; }
  sendReminder(): void {
    const sub = this.service.sendReminder().subscribe({ next: (res: any) => { this.modalReminderOpen = false; this.afficherToast(res.message, 'success'); }, error: () => { this.modalReminderOpen = false; this.afficherToast('Erreur envoi relances.', 'error'); } });
    this.subs.push(sub);
  }

  exportApplications(): void {
    const sub = this.service.exportApplications().subscribe({ next: blob => { const url = window.URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = 'candidatures_export.csv'; a.click(); window.URL.revokeObjectURL(url); this.afficherToast('Export téléchargé.', 'success'); }, error: () => this.afficherToast('Erreur export.', 'error') });
    this.subs.push(sub);
  }

  validerDocument(docId: number, valide: boolean): void {
    if (!this.selectedCandidate) return;
    const sub = this.service.updateDocumentStatus(this.selectedCandidate.id, docId, valide).subscribe({ next: (d: any) => { this.afficherToast(valide ? 'Document validé.' : 'Document rejeté.', 'success'); this.chargerCandidatures(); this.chargerKpis(); }, error: () => this.afficherToast('Erreur validation.', 'error') });
    this.subs.push(sub);
  }

  afficherToast(message: string, type: 'success' | 'error'): void { this.toastMessage = message; this.toastType = type; this.toastVisible = true; setTimeout(() => this.toastVisible = false, 4000); }
}